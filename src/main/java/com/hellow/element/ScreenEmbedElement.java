package com.hellow.element;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ScreenEmbedElement implements Element, Lifecycle {

    private static final int DESIGN_W = 320;
    private static final int DESIGN_H = 240;

    private final Screen embedded;
    private final int hintHeight;
    private final boolean forwardEsc;

    private boolean initialized = false;
    private boolean hovering = false;
    private boolean dragActive = false;

    public ScreenEmbedElement(Screen embedded) {
        this(embedded, 120);
    }

    public ScreenEmbedElement(Screen embedded, int height) {
        this(embedded, height, false);
    }

    public ScreenEmbedElement(Screen embedded, int height, boolean forwardEsc) {
        this.embedded = embedded;
        this.hintHeight = Math.max(1, height);
        this.forwardEsc = forwardEsc;
    }

    public Screen embedded() {
        return this.embedded;
    }

    public int width(Font font) {
        return 200;
    }

    public int height(Font font, int width) {
        return this.hintHeight;
    }

    private static int localX(RenderContext ctx) {
        return ctx.mouseX() - ctx.x();
    }

    private static int localY(RenderContext ctx) {
        return ctx.mouseY() - ctx.y();
    }

    private float scale(RenderContext ctx) {
        int rectW = Math.max(1, ctx.width());
        int rectH = Math.max(1, height(ctx.font(), rectW));
        return Math.min((float) rectW / DESIGN_W, (float) rectH / DESIGN_H);
    }

    private void ensureInit(RenderContext ctx) {
        if (this.initialized) return;
        this.initialized = true;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            this.embedded.init(mc, DESIGN_W, DESIGN_H);
        }
    }

    private int designX(RenderContext ctx) {
        return (int) (localX(ctx) / scale(ctx));
    }

    private int designY(RenderContext ctx) {
        return (int) (localY(ctx) / scale(ctx));
    }

    private boolean inside(RenderContext ctx) {
        return ctx.mouseX() >= ctx.x() && ctx.mouseX() < ctx.x() + ctx.width()
                && ctx.mouseY() >= ctx.y() && ctx.mouseY() < ctx.y() + height(ctx.font(), ctx.width());
    }

    @Override
    public void render(RenderContext ctx) {
        this.hovering = inside(ctx);
        if (ctx.g() == null) return;
        ensureInit(ctx);
        float s = scale(ctx);
        GuiGraphics g = ctx.g();
        g.pose().pushPose();
        g.pose().translate(ctx.x(), ctx.y(), 0.0F);
        g.pose().scale(s, s, 1.0F);
        g.enableScissor(ctx.x(), ctx.y(), ctx.x() + ctx.width(), ctx.y() + height(ctx.font(), ctx.width()));
        try {
            this.embedded.render(g, designX(ctx), designY(ctx), 0.0F);
        } finally {
            g.disableScissor();
            g.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        if (!inside(ctx)) return false;
        ensureInit(ctx);
        this.dragActive = true;
        return this.embedded.mouseClicked(designX(ctx), designY(ctx), button);
    }

    @Override
    public boolean mouseScrolled(RenderContext ctx, double dX, double dY) {
        if (!inside(ctx)) return false;
        ensureInit(ctx);
        return this.embedded.mouseScrolled(designX(ctx), designY(ctx), dX, dY);
    }

    @Override
    public boolean mouseDragged(RenderContext ctx, int button, double dragX, double dragY) {
        if (!this.dragActive && !inside(ctx)) return false;
        ensureInit(ctx);
        float s = scale(ctx);
        return this.embedded.mouseDragged(designX(ctx), designY(ctx), button, dragX / s, dragY / s);
    }

    @Override
    public boolean mouseReleased(RenderContext ctx, int button) {
        if (!this.dragActive && !inside(ctx)) return false;
        this.dragActive = false;
        ensureInit(ctx);
        return this.embedded.mouseReleased(designX(ctx), designY(ctx), button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.hovering) return false;
        if (keyCode == 256 && !this.forwardEsc) return false;
        return this.embedded.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (!this.hovering) return false;
        return this.embedded.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!this.hovering) return false;
        return this.embedded.charTyped(codePoint, modifiers);
    }

    @Override
    public Component tooltipAt(RenderContext ctx) {
        return null;
    }

    @Override
    public void tick() {
        this.embedded.tick();
    }

    @Override
    public void onUnmount() {
        this.dragActive = false;
        this.hovering = false;
        this.embedded.removed();
    }
}
