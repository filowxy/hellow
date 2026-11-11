package com.hellow.page;

import com.hellow.element.Element;
import com.hellow.element.Lifecycle;
import com.hellow.element.RenderContext;
import com.hellow.style.HellowTheme;
import com.hellow.style.HellowThemes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PageScreen extends Screen {
    private static final int PANEL_PADDING = 2;
    private static final long FOCUS_MS = 400;

    private final Page page;
    private Screen parent;
    private final String titleText;
    private final HellowTheme theme;
    private boolean mounted = false;
    private boolean cleanedUp = false;
    private boolean backgroundDrawn = false;
    private final long openedAtMillis = System.currentTimeMillis();

    public PageScreen(Page page, Screen parent) {
        this(page, parent, HellowTheme.DEFAULT);
    }

    public PageScreen(Page page, Screen parent, HellowTheme theme) {
        super(Component.literal(titleOf(page)));
        this.page = page;
        this.parent = parent;
        this.theme = theme == null ? HellowThemes.defaultTheme() : theme;
        this.titleText = titleOf(page);
    }

    public Page page() { return this.page; }

    public Screen parentScreen() { return this.parent; }

    public void reparent(Screen parent) {
        this.parent = parent;
    }

    private static String titleOf(Page page) {
        if (page instanceof PageSpec spec) return spec.titleKey();
        return page.id().value();
    }

    private PageSpec asSpec() {
        return this.page instanceof PageSpec spec ? spec : null;
    }

    private int contentLeft() { return windowLeft() + (windowWidth() - contentWidth()) / 2; }
    private int contentTop() { return windowTop() + window().padding() + window().titleBarHeight(); }
    private int contentWidth() {
        WindowSpec w = window();
        int available = Math.max(0, windowWidth() - w.padding() * 2);
        return w.contentMaxWidth() == WindowSpec.AUTO ? available : Math.min(available, w.contentMaxWidth());
    }
    private int contentHeight() {
        int chrome = window().padding() * 2 + window().titleBarHeight();
        return Math.max(0, windowHeight() - chrome);
    }

    private WindowSpec window() {
        WindowSpec spec = asSpec() == null ? null : asSpec().window();
        return spec == null ? WindowSpec.DEFAULT : spec;
    }

    private int windowWidth() {
        int requested = window().width();
        return requested == WindowSpec.AUTO ? this.width : Math.min(requested, this.width);
    }

    private int windowHeight() {
        int requested = window().height();
        return requested == WindowSpec.AUTO ? this.height : Math.min(requested, this.height);
    }

    private int windowLeft() { return window().anchor().x(0, this.width, windowWidth()); }
    private int windowTop() { return window().anchor().y(0, this.height, windowHeight()); }

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
        this.backgroundDrawn = false;
        this.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, this.width, this.height, this.theme.dimOverlay().argb());
        PageSpec spec = asSpec();
        if (spec != null && spec.body() != null) {
            int left = contentLeft();
            int top = contentTop();
            int w = contentWidth();
            int h = contentHeight();
            g.fill(left - PANEL_PADDING, top - PANEL_PADDING,
                    left + w + PANEL_PADDING, top + h + PANEL_PADDING, this.theme.panelBackground().argb());
            g.renderOutline(left - PANEL_PADDING, top - PANEL_PADDING,
                    w + PANEL_PADDING * 2, h + PANEL_PADDING * 2, this.theme.panelBorder().argb());
            g.drawString(this.font, this.titleText, contentLeft(), windowTop() + window().padding(),
                    this.theme.titleColor().argb(), true);
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
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        PageSpec spec = asSpec();
        if (spec != null && spec.body() != null) {
            if (spec.body().mouseReleased(bodyContext((int) mouseX, (int) mouseY), button)) return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
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
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        PageSpec spec = asSpec();
        if (spec != null && spec.body() != null
                && spec.body().keyReleased(keyCode, scanCode, modifiers)) return true;
        return super.keyReleased(keyCode, scanCode, modifiers);
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
        closeInternal(true);
    }

    @Override
    public void removed() {
        closeInternal(false);
        super.removed();
    }

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
