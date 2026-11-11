package com.hellow.modal;

import com.hellow.HellowConstants;
import com.hellow.debug.HellowDebug;
import com.hellow.element.Element;
import com.hellow.element.RenderContext;
import com.hellow.style.HellowTheme;
import com.hellow.style.HellowThemes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class HellowModal extends Screen {
    protected Screen parent;

    protected int dialogWidth = 320;
    protected int dialogHeight = 180;

    protected int titleArea = 28;

    protected int buttonArea = 36;

    private static final int PADDING = 8;
    private static final int BOTTOM_BUTTON_WIDTH = 100;
    private static final int BOTTOM_BUTTON_HEIGHT = 20;
    private static final int BOTTOM_BUTTON_GAP = 8;
    private static final int WINDOW_MARGIN_X = 20;
    private static final int WINDOW_MARGIN_Y = 44;
    private static final double MAX_UPSCALE = 1.6;

    private static final long FOCUS_MS = 400;

    private final List<Button> bottomButtons = new ArrayList<>();
    private final List<Element> bottomElements = new ArrayList<>();
    private final HellowTheme theme;
    private boolean backgroundDrawn;
    private final long openedAtMillis = System.currentTimeMillis();

    private int designDialogWidth = -1;
    private int designDialogHeight = -1;

    public HellowModal(Component title, Screen parent) {
        this(title, parent, HellowTheme.DEFAULT);
    }

    public HellowModal(Component title, Screen parent, HellowTheme theme) {
        super(title);
        this.parent = parent;
        this.theme = theme == null ? HellowThemes.defaultTheme() : theme;
    }

    protected void addButton(Element button) {
        if (!this.bottomButtons.isEmpty()) {
            HellowConstants.log().warn("[hellow] {} ignored an element button: vanilla bottom button already added",
                    getClass().getSimpleName());
            return;
        }
        this.bottomElements.add(Objects.requireNonNull(button, "button"));
    }

    protected void addBottomButton(Button button) {
        if (!this.bottomElements.isEmpty()) {
            HellowConstants.log().warn("[hellow] {} ignored a vanilla button: element bottom button already added",
                    getClass().getSimpleName());
            return;
        }
        this.bottomButtons.add(Objects.requireNonNull(button, "button"));
    }

    protected final void resetBottomButtons() {
        this.bottomButtons.clear();
        this.bottomElements.clear();
    }

    protected final int dialogLeft() {
        return Math.max(0, (this.width - this.dialogWidth) / 2);
    }

    protected final int dialogTop() {
        return Math.max(0, (this.height - this.dialogHeight) / 2);
    }

    protected final int bodyLeft() {
        return dialogLeft() + PADDING;
    }

    protected final int bodyTop() {
        return dialogTop() + this.titleArea;
    }

    protected final int bodyWidth() {
        return this.dialogWidth - PADDING * 2;
    }

    protected final int bodyHeight() {
        return this.dialogHeight - this.titleArea - this.buttonArea;
    }

    @Override
    protected void init() {
        fitToScreen();
        this.clearWidgets();
        super.init();
        int count = this.bottomButtons.size();
        if (count == 0) return;
        int avail = this.dialogWidth - PADDING * 2;
        int buttonW = Math.min(BOTTOM_BUTTON_WIDTH,
                Math.max(1, (avail - (count - 1) * BOTTOM_BUTTON_GAP) / count));
        int total = count * buttonW + (count - 1) * BOTTOM_BUTTON_GAP;
        int x = dialogLeft() + (this.dialogWidth - total) / 2;
        int y = dialogTop() + this.dialogHeight - this.buttonArea
                + (this.buttonArea - BOTTOM_BUTTON_HEIGHT) / 2;
        for (Button button : this.bottomButtons) {
            button.setX(x);
            button.setY(y);
            button.setWidth(buttonW);
            button.setHeight(BOTTOM_BUTTON_HEIGHT);
            this.addRenderableWidget(button);
            x += buttonW + BOTTOM_BUTTON_GAP;
        }
    }

    private void fitToScreen() {
        if (this.designDialogWidth < 0 || this.designDialogHeight < 0) {
            this.designDialogWidth = this.dialogWidth;
            this.designDialogHeight = this.dialogHeight;
        }
        int availW = Math.max(1, this.width - WINDOW_MARGIN_X);
        int availH = Math.max(1, this.height - WINDOW_MARGIN_Y);
        double scale = Math.min(availW / (double) this.designDialogWidth,
                availH / (double) this.designDialogHeight);
        scale = Math.max(0.0, Math.min(scale, MAX_UPSCALE));
        this.dialogWidth = Math.max(1, (int) Math.floor(this.designDialogWidth * scale));
        this.dialogHeight = Math.max(1, (int) Math.floor(this.designDialogHeight * scale));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.backgroundDrawn = false;
        this.renderBackground(g, mouseX, mouseY, partialTick);
        try {
            g.fill(0, 0, this.width, this.height, this.theme.dimOverlay().argb());

            int left = dialogLeft();
            int top = dialogTop();
            g.fill(left, top, left + this.dialogWidth, top + this.dialogHeight, this.theme.dialogBackground().argb());
            g.renderOutline(left, top, this.dialogWidth, this.dialogHeight, this.theme.dialogBorder().argb());

            int titleY = top + (this.titleArea - this.font.lineHeight) / 2;
            g.drawString(this.font, this.title, left + PADDING, titleY, this.theme.titleColor().argb(), true);

            if (bodyHeight() > 0) {
                g.enableScissor(bodyLeft(), bodyTop(), bodyLeft() + bodyWidth(), bodyTop() + bodyHeight());
                this.renderBody(g, mouseX, mouseY, partialTick);
                g.disableScissor();
            }

            renderElementButtons(g, mouseX, mouseY);

            super.render(g, mouseX, mouseY, partialTick);
        } catch (Throwable t) {
            HellowDebug.capture("render", t, this);
            g.fill(0, 0, this.width, this.height, 0x66000000);
            int left = dialogLeft();
            int top = dialogTop();
            g.fill(left, top, left + this.dialogWidth, top + this.dialogHeight, this.theme.dialogBackground().argb());
            g.drawString(this.font, "Hellow UI error — open " + HellowDebug.class.getSimpleName() + ".openDebug()",
                    left + 8, top + 8, 0xFFFF5555, true);
        }
    }

    private void renderElementButtons(GuiGraphics g, int mouseX, int mouseY) {
        if (this.bottomElements.isEmpty()) return;
        Font font = Minecraft.getInstance().font;
        int[] widths = elementButtonWidths(font);
        int total = 0;
        for (int w : widths) total += w;
        total += BOTTOM_BUTTON_GAP * (this.bottomElements.size() - 1);
        int x = dialogLeft() + (this.dialogWidth - total) / 2;
        int y = dialogTop() + this.dialogHeight - this.buttonArea
                + (this.buttonArea - BOTTOM_BUTTON_HEIGHT) / 2;
        Component tooltip = null;
        for (int i = 0; i < this.bottomElements.size(); i++) {
            RenderContext ctx = new RenderContext(g, font, x, y, widths[i], mouseX, mouseY);
            this.bottomElements.get(i).render(ctx);
            Component t = this.bottomElements.get(i).tooltipAt(ctx);
            if (t != null) tooltip = t;
            x += widths[i] + BOTTOM_BUTTON_GAP;
        }
        if (tooltip != null) {
            g.renderTooltip(font, List.of(tooltip),
                    Optional.<net.minecraft.world.inventory.tooltip.TooltipComponent>empty(), mouseX, mouseY);
        }
    }

    private int[] elementButtonWidths(Font font) {
        int n = this.bottomElements.size();
        int[] widths = new int[n];
        int totalNat = 0;
        for (int i = 0; i < n; i++) {
            widths[i] = Math.max(BOTTOM_BUTTON_WIDTH, this.bottomElements.get(i).width(font));
            totalNat += widths[i];
        }
        totalNat += BOTTOM_BUTTON_GAP * (n - 1);
        int avail = this.dialogWidth - PADDING * 2;
        if (totalNat > avail) {
            int shrink = ((totalNat - avail) + n - 1) / n;
            for (int i = 0; i < n; i++) {
                widths[i] = Math.max(8, widths[i] - shrink);
            }
        }
        return widths;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        try {
            if (dispatchElementButtons((int) mouseX, (int) mouseY, button)) return true;
            return super.mouseClicked(mouseX, mouseY, button);
        } catch (Throwable t) {
            HellowDebug.capture("mouseClicked", t, this);
            return false;
        }
    }

    private boolean dispatchElementButtons(int mouseX, int mouseY, int button) {
        if (this.bottomElements.isEmpty()) return false;
        Font font = Minecraft.getInstance().font;
        int[] widths = elementButtonWidths(font);
        int total = 0;
        for (int w : widths) total += w;
        total += BOTTOM_BUTTON_GAP * (this.bottomElements.size() - 1);
        int x = dialogLeft() + (this.dialogWidth - total) / 2;
        int y = dialogTop() + this.dialogHeight - this.buttonArea
                + (this.buttonArea - BOTTOM_BUTTON_HEIGHT) / 2;
        for (int i = 0; i < this.bottomElements.size(); i++) {
            RenderContext c = new RenderContext(null, font, x, y, widths[i], mouseX, mouseY);
            if (this.bottomElements.get(i).mouseClicked(c, button)) return true;
            x += widths[i] + BOTTOM_BUTTON_GAP;
        }
        return false;
    }

    protected void renderBody(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (this.backgroundDrawn) return;
        this.backgroundDrawn = true;
        Minecraft mc = this.minecraft;
        if (mc == null) return;
        if (mc.level != null) {
            super.renderBackground(g, mouseX, mouseY, partialTick);
            return;
        }
        this.renderPanorama(g, partialTick);
        this.renderBlurredBackground(partialTick);
        float clarity = Math.max(0.0F, 1.0F - (System.currentTimeMillis() - this.openedAtMillis) / (float) FOCUS_MS);
        if (clarity > 0.01F) {
            PANORAMA.render(g, this.width, this.height, clarity, partialTick);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        try {
            if (keyCode == 256) {
                this.onClose();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        } catch (Throwable t) {
            HellowDebug.capture("keyPressed", t, this);
            return false;
        }
    }

    @Override
    public void onClose() {
        if (this.parent != null && this.minecraft != null && this.minecraft.screen == this) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
