package com.hellow.element;

import com.hellow.layout.ElementLayout;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Wraps a child with an {@link ElementLayout}: weight, fill, width bounds and
 * alignment as data instead of a wrapper class per capability.
 *
 * <p>The legacy {@link FlexElement} / {@link FillElement} wrappers are still
 * supported by {@link ElementLayouts}; this type is simply the one that can
 * express every combination. The width bounds are enforced even when the parent
 * is not layout-aware (a {@code BoxElement}, say), because the wrapper clamps the
 * width it measures and the width it hands to the child at draw time.
 */
public final class StyledElement implements Element, Lifecycle {
    private final Element child;
    private final ElementLayout layout;

    public StyledElement(Element child, ElementLayout layout) {
        if (child == null) throw new IllegalArgumentException("styled child required");
        this.child = child;
        this.layout = layout == null ? ElementLayout.NATURAL : layout;
    }

    public ElementLayout layout() {
        return this.layout;
    }

    public Element child() {
        return this.child;
    }

    public int width(Font font) {
        return this.layout.clampWidth(this.child.width(font));
    }

    public int height(Font font, int width) {
        return this.child.height(font, this.layout.clampWidth(width));
    }

    public void render(RenderContext ctx) {
        this.child.render(inner(ctx, this.layout.clampWidth(ctx.width())));
    }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        return this.child.mouseClicked(inner(ctx, this.layout.clampWidth(ctx.width())), button);
    }

    @Override
    public boolean mouseScrolled(RenderContext ctx, double deltaX, double deltaY) {
        return this.child.mouseScrolled(inner(ctx, this.layout.clampWidth(ctx.width())), deltaX, deltaY);
    }

    @Override
    public boolean mouseDragged(RenderContext ctx, int button, double dragX, double dragY) {
        return this.child.mouseDragged(inner(ctx, this.layout.clampWidth(ctx.width())), button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(RenderContext ctx, int button) {
        return this.child.mouseReleased(inner(ctx, this.layout.clampWidth(ctx.width())), button);
    }

    @Override
    public Component tooltipAt(RenderContext ctx) {
        return this.child.tooltipAt(inner(ctx, this.layout.clampWidth(ctx.width())));
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

    private RenderContext inner(RenderContext ctx, int width) {
        return new RenderContext(ctx.g(), ctx.font(), ctx.x(), ctx.y(), width, ctx.mouseX(), ctx.mouseY());
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
