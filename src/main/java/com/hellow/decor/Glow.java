package com.hellow.decor;

import com.hellow.element.RenderContext;
import com.hellow.style.HellowColor;

/**
 * Soft outer halo: {@code radius} rectangles growing outward, each at a fraction
 * of the color's alpha.
 *
 * <p>(ponytail: layered rectangles, no blur pass. At GUI scale the stepping is
 * invisible; a real gaussian glow would need a shader/post-process and a render
 * target per element — not worth it for a frame highlight.)
 */
public record Glow(int radius, HellowColor color) implements Decoration {

    @Override
    public void before(RenderContext ctx, int x, int y, int width, int height) {
        int r = Math.max(1, this.radius);
        if (width <= 0 || height <= 0) return;
        int argb = this.color.argb();
        int alpha = ((argb >>> 24) & 0xFF) / (r + 1);
        if (alpha <= 0) return;
        int rgb = argb & 0xFFFFFF;
        for (int i = r; i >= 1; i--) {
            ctx.g().fill(x - i, y - i, x + width + i, y + height + i, alpha << 24 | rgb);
        }
    }
}
