package com.hellow.decor;

import com.hellow.element.RenderContext;
import com.hellow.style.HellowColor;

/** A rectangular outline of {@code thickness} pixels drawn under the content. */
public record SolidBorder(int thickness, HellowColor color) implements Decoration {

    @Override
    public void before(RenderContext ctx, int x, int y, int width, int height) {
        int t = Math.max(1, this.thickness);
        if (width <= 0 || height <= 0) return;
        int argb = this.color.argb();
        ctx.g().fill(x, y, x + width, y + t, argb);
        ctx.g().fill(x, y + height - t, x + width, y + height, argb);
        ctx.g().fill(x, y + t, x + t, y + height - t, argb);
        ctx.g().fill(x + width - t, y + t, x + width, y + height - t, argb);
    }
}
