package com.hellow.debug;

import com.hellow.HellowConstants;
import com.hellow.decor.Decoration;
import com.hellow.decor.DecorationPresets;
import com.hellow.decor.DecoratedElement;
import com.hellow.element.ButtonElement;
import com.hellow.element.Element;
import com.hellow.element.Elements;
import com.hellow.element.RenderContext;
import com.hellow.element.RowElement;
import com.hellow.element.TextElement;
import com.hellow.layout.ContentAlign;
import com.hellow.layout.Justify;
import com.hellow.modal.HellowModal;
import com.hellow.state.State;
import com.hellow.style.HellowTheme;
import com.hellow.style.HellowThemes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * In-game tuner for the layout and decoration primitives: sliders and pickers
 * change a live sample, and the resulting JSON snippet is written to the log so
 * the same configuration can be pasted into a page file.
 *
 * <p>Deliberately reachable from {@link DebugScreen} instead of a command —
 * everyconfig is command-free and Hellow should not grow a command surface for
 * a development tool.
 */
public final class LayoutPlaygroundScreen extends HellowModal {
    private static final List<String> JUSTIFY = List.of(
            "START", "CENTER", "END", "SPACE_BETWEEN", "SPACE_AROUND", "SPACE_EVENLY", "EQUAL");
    private static final List<String> ALIGN = List.of(
            "LEFT_TOP", "CENTER_TOP", "RIGHT_TOP", "LEFT_MIDDLE", "CENTER_MIDDLE", "RIGHT_MIDDLE");
    private static final List<String> PRESETS = List.of(
            "none", "outline", "panel", "card", "glow", "chip", "focus", "shadow", "framed");
    private static final int PREVIEW_HEIGHT = 64;

    private final HellowTheme theme;
    private final State<Integer> gap = State.of(6);
    private final State<String> justify = State.of("SPACE_BETWEEN");
    private final State<String> align = State.of("CENTER_MIDDLE");
    private final State<String> preset = State.of("card");
    private final State<Integer> previewWidth = State.of(280);
    private Element body;

    public LayoutPlaygroundScreen(Screen parent) {
        this(parent, HellowThemes.defaultTheme());
    }

    public LayoutPlaygroundScreen(Screen parent, HellowTheme theme) {
        super(Component.literal("Hellow Layout Playground"), parent, theme);
        this.theme = theme == null ? HellowThemes.defaultTheme() : theme;
        this.dialogWidth = 560;
        this.dialogHeight = 330;
    }

    @Override
    protected void init() {
        resetBottomButtons();
        addButton(Elements.button("Log JSON", this::logJson));
        addButton(Elements.button("Close", () -> {
            if (this.minecraft != null) this.minecraft.setScreen(this.parent);
        }));
        super.init();
        this.body = buildBody();
    }

    private Element buildBody() {
        return Elements.column(6,
                Elements.heading("Layout playground"),
                Elements.wrappedText("Tune the sample; the JSON goes to the log.", 0xFFAAAAAA),
                Elements.slider("Gap", 0, 24, this.gap),
                Elements.enumSelect("Justify", JUSTIFY, this.justify),
                Elements.enumSelect("Align", ALIGN, this.align),
                Elements.enumSelect("Decoration preset", PRESETS, this.preset),
                Elements.slider("Sample width", 120, 480, this.previewWidth),
                previewElement());
    }

    private Element previewElement() {
        return new PreviewElement();
    }

    /**
     * Reads the live state on every draw, so the sample updates as the widgets
     * change without rebuilding the tree (which would reset slider drags).
     */
    private final class PreviewElement implements Element {
        @Override
        public int width(Font font) {
            return LayoutPlaygroundScreen.this.previewWidth.get();
        }

        @Override
        public int height(Font font, int width) {
            return PREVIEW_HEIGHT;
        }

        @Override
        public void render(RenderContext ctx) {
            int width = Math.min(ctx.width(), LayoutPlaygroundScreen.this.previewWidth.get());
            int x = ctx.x() + Math.max(0, (ctx.width() - width) / 2);
            List<Element> chips = List.of(
                    new ButtonElement("one", () -> {}),
                    new ButtonElement("two", () -> {}),
                    new ButtonElement("three", () -> {}));
            Element row = new RowElement(chips, LayoutPlaygroundScreen.this.gap.get(),
                    ContentAlign.valueOf(LayoutPlaygroundScreen.this.align.get()),
                    Justify.valueOf(LayoutPlaygroundScreen.this.justify.get()));
            List<Decoration> decorations = preset(
                    LayoutPlaygroundScreen.this.preset.get(), LayoutPlaygroundScreen.this.theme);
            Element decorated = decorations.isEmpty() ? row : new DecoratedElement(row, decorations);
            decorated.render(new RenderContext(ctx.g(), ctx.font(), x,
                    ctx.y() + (PREVIEW_HEIGHT - row.height(ctx.font(), width)) / 2,
                    width, ctx.mouseX(), ctx.mouseY()));
        }
    }

    private static List<Decoration> preset(String name, HellowTheme theme) {
        return switch (name) {
            case "outline" -> DecorationPresets.outline(theme);
            case "panel" -> DecorationPresets.panel(theme);
            case "card" -> DecorationPresets.card(theme);
            case "glow" -> DecorationPresets.glow(theme);
            case "chip" -> DecorationPresets.chip(theme);
            case "focus" -> DecorationPresets.focus(theme);
            case "shadow" -> DecorationPresets.shadow(theme);
            case "framed" -> DecorationPresets.framed(theme);
            default -> List.of();
        };
    }

    private void logJson() {
        String json = "{\"type\":\"row\",\"gap\":" + this.gap.get()
                + ",\"justify\":\"" + this.justify.get().toLowerCase(Locale.ROOT) + "\""
                + ",\"align\":\"" + this.align.get().toLowerCase(Locale.ROOT) + "\""
                + ",\"decorations\":[\"" + this.preset.get() + "\"]"
                + ",\"children\":[ ... ]}";
        HellowConstants.log().info("[hellow] layout playground: {}", json);
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                    Component.literal("Layout JSON written to the log"), false);
        }
    }

    @Override
    protected void renderBody(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (this.body == null) return;
        this.body.render(new RenderContext(g, this.font, bodyLeft(), bodyTop(), bodyWidth(), mouseX, mouseY));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.body != null && this.body.mouseClicked(bodyCtx(mouseX, mouseY), button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (this.body != null && this.body.mouseScrolled(bodyCtx(mouseX, mouseY), deltaX, deltaY)) return true;
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.body != null && this.body.mouseDragged(bodyCtx(mouseX, mouseY), button, dragX, dragY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.body != null && this.body.mouseReleased(bodyCtx(mouseX, mouseY), button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private RenderContext bodyCtx(double mouseX, double mouseY) {
        return new RenderContext(null, this.font, bodyLeft(), bodyTop(), bodyWidth(), (int) mouseX, (int) mouseY);
    }
}
