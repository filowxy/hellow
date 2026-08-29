package com.hellow.element;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * A fill-width wrapper around a child element.
 *
 * <p>Inside a fill-capable container (currently {@link ColumnElement} and
 * {@link GridElement}), a {@code FillElement} is rendered at the container's remaining
 * width instead of the child's natural width, so width-dependent children (e.g. a
 * {@link TextElement} in WRAP mode) get a bounded width and actually wrap. Outside such
 * a container, or when there is no spare width, the wrapper behaves exactly like its
 * child.
 *
 * <p>(ponytail: the fill behavior is opt-in per container via {@code instanceof
 * FillElement}; multiple fills in one container share the remaining width equally. If a
 * general "fill child spec" is ever needed, move this into a container-child record.)
 *
 * @param child the element to stretch to the container's remaining width
 */
public record FillElement(Element child) implements Element {

    public FillElement {
        if (child == null) throw new IllegalArgumentException("child required");
    }

    public int width(Font font) { return this.child.width(font); }

    public int height(Font font, int width) { return this.child.height(font, width); }

    public void render(RenderContext ctx) { this.child.render(ctx); }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        return this.child.mouseClicked(ctx, button);
    }

    @Override
    public boolean mouseScrolled(RenderContext ctx, double dX, double dY) {
        return this.child.mouseScrolled(ctx, dX, dY);
    }

    @Override
    public boolean mouseDragged(RenderContext ctx, int button, double dragX, double dragY) {
        return this.child.mouseDragged(ctx, button, dragX, dragY);
    }

    @Override
    public Component tooltipAt(RenderContext ctx) {
        return this.child.tooltipAt(ctx);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.child.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.child.charTyped(codePoint, modifiers);
    }
}