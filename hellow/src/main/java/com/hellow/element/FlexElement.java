package com.hellow.element;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * A weighted-flex wrapper around a child element.
 *
 * <p>Inside a flex-capable container (currently {@link RowElement}), a {@code FlexElement}
 * with {@code weight > 0} absorbs a proportional share of the space the container's
 * natural content leaves over. The child is rendered with that larger width, so a
 * {@link ButtonElement} inside it draws full-width while a {@link SpacerElement} inside
 * it becomes a pure flexible gap.
 *
 * <p>Outside a flex-capable container, or when there is no spare space, the wrapper
 * behaves exactly like its child.
 *
 * @param child  the element to flex
 * @param weight flex weight; must be &gt; 0 to participate in space distribution
 */
public record FlexElement(Element child, int weight) implements Element {

    public FlexElement {
        if (weight < 0) throw new IllegalArgumentException("flex weight must be >= 0, got " + weight);
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
