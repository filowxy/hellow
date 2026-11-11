package com.hellow.decor;

import com.hellow.element.Element;
import com.hellow.element.Lifecycle;
import com.hellow.element.RenderContext;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Wraps any element with an ordered list of {@link Decoration}s.
 *
 * <p>Layout is untouched: the wrapper reports and draws exactly the child's
 * rectangle, decorations may paint outside it (shadows, glows) when the parent
 * does not clip. Every input event is forwarded to the child, so decoration and
 * interaction stay independent.
 */
public final class DecoratedElement implements Element, Lifecycle {
    private final Element child;
    private final List<Decoration> decorations;

    public DecoratedElement(Element child, List<Decoration> decorations) {
        if (child == null) throw new IllegalArgumentException("decorated child required");
        this.child = child;
        this.decorations = decorations == null ? List.of() : List.copyOf(decorations);
    }

    public Element child() {
        return this.child;
    }

    public List<Decoration> decorations() {
        return this.decorations;
    }

    public int width(Font font) {
        return this.child.width(font);
    }

    public int height(Font font, int width) {
        return this.child.height(font, width);
    }

    public void render(RenderContext ctx) {
        for (Decoration decoration : this.decorations) {
            decoration.before(ctx, ctx.x(), ctx.y(), ctx.width(), heightAt(ctx));
        }
        this.child.render(ctx);
        for (Decoration decoration : this.decorations) {
            decoration.after(ctx, ctx.x(), ctx.y(), ctx.width(), heightAt(ctx));
        }
    }

    /**
     * The rectangle decorations outline. {@code RenderContext} carries no height,
     * so it is measured from the child at the width it is about to be drawn at.
     */
    private int heightAt(RenderContext ctx) {
        return this.child.height(ctx.font(), ctx.width());
    }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        return this.child.mouseClicked(ctx, button);
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
    public Component tooltipAt(RenderContext ctx) {
        return this.child.tooltipAt(ctx);
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
