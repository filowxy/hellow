package com.hellow.decor;

import com.hellow.element.RenderContext;
import com.hellow.style.HellowColor;

/**
 * Four L-shaped accents, one per corner — a HUD-style frame that costs eight
 * rectangles and needs no knowledge of what is behind the element.
 */
public record CornerAccents(int length, int thickness, HellowColor color) implements Decoration {

    @Override
    public void before(RenderContext ctx, int x, int y, int width, int height) {
        int len = Math.max(1, this.length);
        int t = Math.max(1, this.thickness);
        if (width <= 0 || height <= 0) return;
        len = Math.min(len, Math.min(width, height));
        int argb = this.color.argb();
        // top-left
        ctx.g().fill(x, y, x + len, y + t, argb);
        ctx.g().fill(x, y, x + t, y + len, argb);
        // top-right
        ctx.g().fill(x + width - len, y, x + width, y + t, argb);
        ctx.g().fill(x + width - t, y, x + width, y + len, argb);
        // bottom-left
        ctx.g().fill(x, y + height - t, x + len, y + height, argb);
        ctx.g().fill(x, y + height - len, x + t, y + height, argb);
        // bottom-right
        ctx.g().fill(x + width - len, y + height - t, x + width, y + height, argb);
        ctx.g().fill(x + width - t, y + height - len, x + width, y + height, argb);
    }
}
