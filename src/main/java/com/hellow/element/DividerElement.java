package com.hellow.element;

import com.hellow.style.HellowTheme;
import net.minecraft.client.gui.Font;

public final class DividerElement implements Element {
    private final int thickness;
    private final int color;

    public DividerElement() {
        this(1, HellowTheme.DEFAULT.panelBorder().argb());
    }

    public DividerElement(int thickness, int color) {
        this.thickness = Math.max(1, thickness);
        this.color = color;
    }

    public int width(Font font) {
        return 0;
    }

    public int height(Font font, int width) {
        return this.thickness;
    }

    public void render(RenderContext ctx) {
        ctx.g().fill(ctx.x(), ctx.y(), ctx.x() + ctx.width(), ctx.y() + this.thickness, this.color);
    }
}
