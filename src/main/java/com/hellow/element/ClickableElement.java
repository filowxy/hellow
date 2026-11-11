package com.hellow.element;

import com.hellow.action.Action;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.List;

public record ClickableElement(Element child, Action action) implements Element, Lifecycle {

    public ClickableElement {
        if (child == null) throw new IllegalArgumentException("child required");
        if (action == null) throw new IllegalArgumentException("action required");
    }

    public static ClickableElement of(Element child, Runnable runnable) {
        return new ClickableElement(child, runnable::run);
    }

    public int width(Font font) { return this.child.width(font); }

    public int height(Font font, int width) { return this.child.height(font, width); }

    public void render(RenderContext ctx) { this.child.render(ctx); }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        if (this.child.mouseClicked(ctx, button)) return true;
        if (button == 0 && overChild(ctx)) {
            this.action.run();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(RenderContext ctx, double deltaX, double deltaY) {
        return this.child.mouseScrolled(ctx, deltaX, deltaY);
    }

    @Override
    public boolean mouseDragged(RenderContext ctx, int button, double dragX, double dragY) {
        return this.child.mouseDragged(ctx, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(RenderContext ctx, int button) {
        return this.child.mouseReleased(ctx, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.child.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return this.child.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.child.charTyped(codePoint, modifiers);
    }

    @Override
    public Component tooltipAt(RenderContext ctx) {
        return this.child.tooltipAt(ctx);
    }

    private boolean overChild(RenderContext ctx) {
        return ctx.mouseX() >= ctx.x() && ctx.mouseX() <= ctx.x() + ctx.width()
                && ctx.mouseY() >= ctx.y() && ctx.mouseY() <= ctx.y() + this.child.height(ctx.font(), ctx.width());
    }

    @Override
    public List<Element> children() {
        return List.of(this.child);
    }

    @Override
    public void onMount() {
        Lifecycle.mountOne(this.child);
    }

    @Override
    public void tick() {
        Lifecycle.tickOne(this.child);
    }

    @Override
    public void onUnmount() {
        Lifecycle.unmountOne(this.child);
    }
}
