package com.hellow.decor;

import com.hellow.element.RenderContext;
import com.hellow.style.HellowColor;

/**
 * Fills the element rectangle — a flat panel when both colors match, a vertical
 * gradient otherwise. Drawn under the content.
 */
public record Backdrop(HellowColor top, HellowColor bottom) implements Decoration {

    public Backdrop(HellowColor color) {
        this(color, color);
    }

    @Override
    public void before(RenderContext ctx, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        ctx.g().fillGradient(x, y, x + width, y + height, this.top.argb(), this.bottom.argb());
    }
}
