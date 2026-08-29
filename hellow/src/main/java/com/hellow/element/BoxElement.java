package com.hellow.element;

import com.hellow.layout.Spacing;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public final class BoxElement implements Element {
    private final Element content;
    private final Spacing margin;
    private final Spacing padding;
    private final int borderWidth;
    private final int borderColor;

    public BoxElement(Element content, Spacing margin, Spacing padding, int borderWidth, int borderColor) {
        this.content = content;
        this.margin = margin == null ? Spacing.NONE : margin;
        this.padding = padding == null ? Spacing.NONE : padding;
        this.borderWidth = Math.max(0, borderWidth);
        this.borderColor = borderColor;
    }

    public int width(Font font) {
        return this.margin.horizontal() + this.borderWidth * 2 + this.padding.horizontal() + this.content.width(font);
    }

    public int height(Font font, int width) {
        int inner = width - this.margin.horizontal() - this.borderWidth * 2 - this.padding.horizontal();
        return this.margin.vertical() + this.borderWidth * 2 + this.padding.vertical() + this.content.height(font, Math.max(0, inner));
    }

    public void render(RenderContext ctx) {
        Font font = ctx.font();
        int contentW = Math.max(0, ctx.width() - this.margin.horizontal() - this.borderWidth * 2 - this.padding.horizontal());
        int contentH = this.content.height(font, contentW);
        int bx = ctx.x() + this.margin.left();
        int by = ctx.y() + this.margin.top();
        int bw = ctx.width() - this.margin.horizontal();
        // Include the vertical margin so the drawn box exactly fills the height the
        // layout allocated (height() counts margin.vertical() too) — without it the
        // bottom margin silently collapses.
        int bh = this.margin.vertical() + this.borderWidth * 2 + this.padding.vertical() + contentH;
        if (this.borderWidth > 0 && this.borderColor != 0) {
            ctx.g().fill(bx, by, bx + bw, by + bh, this.borderColor);
        }
        int cx = bx + this.borderWidth + this.padding.left();
        int cy = by + this.borderWidth + this.padding.top();
        this.content.render(new RenderContext(ctx.g(), font, cx, cy, contentW, ctx.mouseX(), ctx.mouseY()));
    }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        int contentW = Math.max(0, ctx.width() - this.margin.horizontal() - this.borderWidth * 2 - this.padding.horizontal());
        int cx = ctx.x() + this.margin.left() + this.borderWidth + this.padding.left();
        int cy = ctx.y() + this.margin.top() + this.borderWidth + this.padding.top();
        return this.content.mouseClicked(new RenderContext(ctx.g(), ctx.font(), cx, cy, contentW, ctx.mouseX(), ctx.mouseY()), button);
    }

    @Override
    public boolean mouseScrolled(RenderContext ctx, double dX, double dY) {
        int contentW = Math.max(0, ctx.width() - this.margin.horizontal() - this.borderWidth * 2 - this.padding.horizontal());
        int cx = ctx.x() + this.margin.left() + this.borderWidth + this.padding.left();
        int cy = ctx.y() + this.margin.top() + this.borderWidth + this.padding.top();
        return this.content.mouseScrolled(new RenderContext(ctx.g(), ctx.font(), cx, cy, contentW, ctx.mouseX(), ctx.mouseY()), dX, dY);
    }

    @Override
    public Component tooltipAt(RenderContext ctx) {
        int contentW = Math.max(0, ctx.width() - this.margin.horizontal() - this.borderWidth * 2 - this.padding.horizontal());
        int cx = ctx.x() + this.margin.left() + this.borderWidth + this.padding.left();
        int cy = ctx.y() + this.margin.top() + this.borderWidth + this.padding.top();
        return this.content.tooltipAt(new RenderContext(ctx.g(), ctx.font(), cx, cy, contentW, ctx.mouseX(), ctx.mouseY()));
    }

    @Override
    public boolean mouseDragged(RenderContext ctx, int button, double dragX, double dragY) {
        int contentW = Math.max(0, ctx.width() - this.margin.horizontal() - this.borderWidth * 2 - this.padding.horizontal());
        int cx = ctx.x() + this.margin.left() + this.borderWidth + this.padding.left();
        int cy = ctx.y() + this.margin.top() + this.borderWidth + this.padding.top();
        return this.content.mouseDragged(new RenderContext(ctx.g(), ctx.font(), cx, cy, contentW, ctx.mouseX(), ctx.mouseY()), button, dragX, dragY);
    }

    // Single-child keyboard fan-out (see ColumnElement).
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.content.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.content.charTyped(codePoint, modifiers);
    }
}