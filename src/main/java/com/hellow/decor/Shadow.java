package com.hellow.decor;

import com.hellow.element.RenderContext;
import com.hellow.style.HellowColor;

/**
 * Drop shadow under the element: {@code spread} ever-smaller offset rectangles,
 * each at a fraction of the color's alpha, so the falloff accumulates into a
 * soft edge.
 *
 * <p>It paints outside the element bounds; it only shows when the parent does not
 * clip to the same rectangle.
 */
public record Shadow(int offsetX, int offsetY, int spread, HellowColor color) implements Decoration {

    @Override
    public void before(RenderContext ctx, int x, int y, int width, int height) {
        int s = Math.max(0, this.spread);
        if (width <= 0 || height <= 0) return;
        int argb = this.color.argb();
        int alpha = ((argb >>> 24) & 0xFF) / (s + 1);
        if (alpha <= 0) return;
        int rgb = argb & 0xFFFFFF;
        for (int i = s; i >= 0; i--) {
            ctx.g().fill(x + this.offsetX - i, y + this.offsetY - i,
                    x + width + this.offsetX + i, y + height + this.offsetY + i,
                    alpha << 24 | rgb);
        }
    }
}
