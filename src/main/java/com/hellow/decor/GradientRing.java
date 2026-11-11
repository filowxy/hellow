package com.hellow.decor;

import com.hellow.element.RenderContext;
import com.hellow.style.HellowColor;

/**
 * A 1px-ish gradient frame: the whole rectangle is painted with a start-to-end
 * gradient, then the interior is covered with {@code inner}, leaving a ring.
 *
 * <p>This is the generalized form of the card border TISAM hand-drew inside its
 * card element — animated start color, fixed end color, content on top.
 */
public record GradientRing(int thickness, HellowColor start, HellowColor end, HellowColor inner)
        implements Decoration {

    @Override
    public void before(RenderContext ctx, int x, int y, int width, int height) {
        int t = Math.max(1, this.thickness);
        if (width <= t * 2 || height <= t * 2) return;
        ctx.g().fillGradient(x, y, x + width, y + height, this.start.argb(), this.end.argb());
        ctx.g().fill(x + t, y + t, x + width - t, y + height - t, this.inner.argb());
    }
}
