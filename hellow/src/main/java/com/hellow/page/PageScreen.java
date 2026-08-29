package com.hellow.page;

import com.hellow.element.Element;
import com.hellow.element.Lifecycle;
import com.hellow.element.RenderContext;
import com.hellow.style.HellowTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The screen that renders a {@link Page} as a window.
 *
 * <p>Owns the page-window chrome (dim background, title bar, content panel), clips the
 * page's element tree to the content panel with a scissor, and forwards every input
 * event (mouse, key, char) plus {@code tick()} to the body element so interactive
 * widgets keep working inside a page.
 *
 * <p>Closing (ESC) unwinds the window stack via {@link PageScheduler#onPageClosed(Page)}
 * and returns to the {@code parent} screen the window was opened from.
 */
public final class PageScreen extends Screen {
    private static final int PADDING = 10;
    private static final int TITLE_BAR_HEIGHT = 20;
    private static final int PANEL_PADDING = 2;

    private final Page page;
    private final Screen parent;
    private final String titleText;
    private final HellowTheme theme;
    private boolean mounted = false;
    private boolean cleanedUp = false;

    public PageScreen(Page page, Screen parent) {
        this(page, parent, HellowTheme.DEFAULT);
    }

    /** @param theme chrome colors; {@code null} falls back to {@link HellowTheme#DEFAULT} */
    public PageScreen(Page page, Screen parent, HellowTheme theme) {
        super(Component.literal(titleOf(page)));
        this.page = page;
        this.parent = parent;
        this.theme = theme == null ? HellowTheme.DEFAULT : theme;
        this.titleText = titleOf(page);
    }

    public Page page() { return this.page; }

    private static String titleOf(Page page) {
        if (page instanceof PageSpec spec) return spec.titleKey();
        return page.id().value();
    }

    private PageSpec asSpec() {
        return this.page instanceof PageSpec spec ? spec : null;
    }

    private int contentLeft() { return PADDING; }
    private int contentTop() { return PADDING + TITLE_BAR_HEIGHT; }
    private int contentWidth() { return this.width - PADDING * 2; }
    private int contentHeight() { return this.height - contentTop() - PADDING; }

    private RenderContext bodyContext(int mouseX, int mouseY) {
        return new RenderContext(null, Minecraft.getInstance().font,
                contentLeft(), contentTop(), contentWidth(), mouseX, mouseY);
    }

    @Override
    protected void init() {
        super.init();
        if (this.mounted) return;
        this.mounted = true;
        if (asSpec() != null && asSpec().body() instanceof Lifecycle lc) lc.onMount();
    }

    @Override
    public void tick() {
        super.tick();
        if (asSpec() != null && asSpec().body() instanceof Lifecycle lc) lc.tick();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Page window chrome first, then widgets on top.
        g.fill(0, 0, this.width, this.height, this.theme.dimOverlay());
        PageSpec spec = asSpec();
        if (spec != null && spec.body() != null) {
            int left = contentLeft();
            int top = contentTop();
            int w = contentWidth();
            int h = contentHeight();
            g.fill(left - PANEL_PADDING, top - PANEL_PADDING,
                    left + w + PANEL_PADDING, top + h + PANEL_PADDING, this.theme.panelBackground());
            g.renderOutline(left - PANEL_PADDING, top - PANEL_PADDING,
                    w + PANEL_PADDING * 2, h + PANEL_PADDING * 2, this.theme.panelBorder());
            g.drawString(this.font, this.titleText, left, PADDING, this.theme.titleColor(), true);
            g.enableScissor(left, top, left + w, top + h);
            RenderContext ctx = new RenderContext(g, this.font, left, top, w, mouseX, mouseY);
            spec.body().render(ctx);
            g.disableScissor();
            Component tooltip = spec.body().tooltipAt(ctx);
            if (tooltip != null) {
                g.renderTooltip(this.font, java.util.List.of(tooltip),
                        java.util.Optional.<net.minecraft.world.inventory.tooltip.TooltipComponent>empty(), mouseX, mouseY);
            }
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // No-op: the dim layer is drawn in render() so pages don't flash the dirt texture.
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        PageSpec spec = asSpec();
        if (spec != null && spec.body() != null) {
            if (spec.body().mouseClicked(bodyContext((int) mouseX, (int) mouseY), button)) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        PageSpec spec = asSpec();
        if (spec != null && spec.body() != null) {
            if (spec.body().mouseScrolled(bodyContext((int) mouseX, (int) mouseY), deltaX, deltaY)) return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        PageSpec spec = asSpec();
        if (spec != null && spec.body() != null) {
            if (spec.body().mouseDragged(bodyContext((int) mouseX, (int) mouseY), button, dragX, dragY)) return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        PageSpec spec = asSpec();
        if (spec != null && spec.body() != null
                && spec.body().keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        PageSpec spec = asSpec();
        if (spec != null && spec.body() != null
                && spec.body().charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        // ESC / explicit close: unwind the window stack, unmount, and return to the
        // parent. Only when we are actually the active screen being closed.
        closeInternal(true);
    }

    @Override
    public void removed() {
        // Programmatic replacement calls removed(), not onClose() (Minecraft never
        // calls onClose for a setScreen-replaced screen). Unwind the window stack and
        // the lifecycle so the scheduler doesn't leak the page — but never try to
        // restore the parent, the caller already chose the next screen.
        closeInternal(false);
        super.removed();
    }

    /** Idempotent cleanup shared by ESC and programmatic replacement. */
    private void closeInternal(boolean restoreParent) {
        if (this.cleanedUp) return;
        this.cleanedUp = true;
        PageScheduler.onPageClosed(this.page);
        if (asSpec() != null && asSpec().body() instanceof Lifecycle lc) lc.onUnmount();
        if (restoreParent && this.minecraft != null && this.minecraft.screen == this
                && this.parent != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
