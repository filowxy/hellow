package com.hellow.debug;

import com.hellow.element.Element;
import com.hellow.element.Elements;
import com.hellow.element.RenderContext;
import com.hellow.element.ScrollElement;
import com.hellow.element.TextElement;
import com.hellow.modal.HellowModal;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * In-game viewer for the captured UI errors. Opens via {@link HellowDebug#openDebug()}.
 * The body is a scrollable list of one-line summaries; Export writes the full report
 * (including stack traces) to {@code config/hellow/debug/}.
 */
public final class DebugScreen extends HellowModal {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private Element body;
    private String exportNotice;

    public DebugScreen(Screen parent) {
        super(Component.literal("Hellow Debug Report"), parent);
        this.dialogWidth = 520;
        this.dialogHeight = 320;
    }

    @Override
    protected void init() {
        resetBottomButtons();
        addButton(Elements.button("Refresh", this::reload));
        addButton(Elements.button("Export", this::exportReport));
        addButton(Elements.button("Clear", () -> {
            HellowDebug.clear();
            reload();
        }));
        addButton(Elements.button("Close", () -> {
            if (this.minecraft != null) this.minecraft.setScreen(this.parent);
        }));
        super.init();
        reload();
    }

    /** Rebuild the body list from the current entries. */
    private void reload() {
        List<Element> rows = new ArrayList<>();
        if (this.exportNotice != null) {
            rows.add(new TextElement(Component.literal(this.exportNotice), 0xFF55FF55));
        }
        if (HellowDebug.count() == 0) {
            rows.add(new TextElement(Component.translatable("hellow.debug.empty")
                    .withStyle(ChatFormatting.GRAY), 0xFFAAAAAA));
        } else {
            for (DebugEntry e : HellowDebug.entries()) {
                String stamp = TIME.format(Instant.ofEpochMilli(e.timeMillis()).atZone(ZoneId.systemDefault()).toLocalDateTime());
                rows.add(new TextElement(Component.literal(stamp + "  " + e.headLine()), 0xFFEEEEEE));
            }
        }
        this.body = new ScrollElement(rows, 4, Math.max(1, bodyHeight()));
    }

    private void exportReport() {
        Path path = HellowDebug.exportFile();
        this.exportNotice = path == null
                ? "Export failed — see logs"
                : "Exported to " + path;
        reload();
    }

    @Override
    protected void renderBody(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (this.body == null) return;
        this.body.render(new RenderContext(g, this.font, bodyLeft(), bodyTop(), bodyWidth(), mouseX, mouseY));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.body != null && this.body.mouseClicked(bodyCtx(mouseX, mouseY), button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (this.body != null && this.body.mouseScrolled(bodyCtx(mouseX, mouseY), deltaX, deltaY)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.body != null && this.body.mouseDragged(bodyCtx(mouseX, mouseY), button, dragX, dragY)) {
            return true;
        }
        return false;
    }

    private RenderContext bodyCtx(double mouseX, double mouseY) {
        return new RenderContext(null, this.font, bodyLeft(), bodyTop(), bodyWidth(), (int) mouseX, (int) mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (this.minecraft != null && this.parent != null) {
                this.minecraft.setScreen(this.parent);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}