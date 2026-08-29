package com.hellow.element;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class WrapRowElement implements Element {
    private final List<Element> children;
    private final int gap;
    private final int maxWidth;

    public WrapRowElement(List<Element> children, int gap, int maxWidth) {
        this.children = children;
        this.gap = gap;
        this.maxWidth = Math.max(0, maxWidth);
    }

    public int width(Font font) { return this.maxWidth; }

    public int height(Font font, int width) {
        int x = 0, h = 0, rowH = 0;
        boolean any = false;
        for (Element child : this.children) {
            int cw = child.width(font);
            if (any && x + cw > this.maxWidth) {
                h += rowH + this.gap;
                x = 0; rowH = 0; any = false;
            }
            x += cw + this.gap;
            rowH = Math.max(rowH, child.height(font, cw));
            any = true;
        }
        return any ? h + rowH : h;
    }

    public void render(RenderContext ctx) {
        int x = ctx.x(), y = ctx.y(), rowH = 0;
        boolean any = false;
        for (Element child : this.children) {
            int cw = child.width(ctx.font());
            if (any && x + cw > ctx.x() + this.maxWidth) {
                y += rowH + this.gap; x = ctx.x(); rowH = 0; any = false;
            }
            child.render(new RenderContext(ctx.g(), ctx.font(), x, y, cw, ctx.mouseX(), ctx.mouseY()));
            x += cw + this.gap;
            rowH = Math.max(rowH, child.height(ctx.font(), cw));
            any = true;
        }
    }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        int x = ctx.x(), y = ctx.y(), rowH = 0;
        boolean any = false;
        for (Element child : this.children) {
            int cw = child.width(ctx.font());
            if (any && x + cw > ctx.x() + this.maxWidth) {
                y += rowH + this.gap; x = ctx.x(); rowH = 0; any = false;
            }
            RenderContext c = new RenderContext(ctx.g(), ctx.font(), x, y, cw, ctx.mouseX(), ctx.mouseY());
            if (child.mouseClicked(c, button)) return true;
            x += cw + this.gap;
            rowH = Math.max(rowH, child.height(ctx.font(), cw));
            any = true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(RenderContext ctx, double dX, double dY) {
        int x = ctx.x(), y = ctx.y(), rowH = 0;
        boolean any = false;
        for (Element child : this.children) {
            int cw = child.width(ctx.font());
            if (any && x + cw > ctx.x() + this.maxWidth) {
                y += rowH + this.gap; x = ctx.x(); rowH = 0; any = false;
            }
            RenderContext c = new RenderContext(ctx.g(), ctx.font(), x, y, cw, ctx.mouseX(), ctx.mouseY());
            if (child.mouseScrolled(c, dX, dY)) return true;
            x += cw + this.gap;
            rowH = Math.max(rowH, child.height(ctx.font(), cw));
            any = true;
        }
        return false;
    }

    @Override
    public Component tooltipAt(RenderContext ctx) {
        int x = ctx.x(), y = ctx.y(), rowH = 0;
        boolean any = false;
        for (Element child : this.children) {
            int cw = child.width(ctx.font());
            if (any && x + cw > ctx.x() + this.maxWidth) {
                y += rowH + this.gap; x = ctx.x(); rowH = 0; any = false;
            }
            Component tooltip = child.tooltipAt(new RenderContext(ctx.g(), ctx.font(), x, y, cw, ctx.mouseX(), ctx.mouseY()));
            if (tooltip != null) return tooltip;
            x += cw + this.gap;
            rowH = Math.max(rowH, child.height(ctx.font(), cw));
            any = true;
        }
        return null;
    }

    @Override
    public boolean mouseDragged(RenderContext ctx, int button, double dragX, double dragY) {
        int x = ctx.x(), y = ctx.y(), rowH = 0;
        boolean any = false;
        for (Element child : this.children) {
            int cw = child.width(ctx.font());
            if (any && x + cw > ctx.x() + this.maxWidth) {
                y += rowH + this.gap; x = ctx.x(); rowH = 0; any = false;
            }
            RenderContext c = new RenderContext(ctx.g(), ctx.font(), x, y, cw, ctx.mouseX(), ctx.mouseY());
            if (child.mouseDragged(c, button, dragX, dragY)) return true;
            x += cw + this.gap;
            rowH = Math.max(rowH, child.height(ctx.font(), cw));
            any = true;
        }
        return false;
    }

    // Keyboard fan-out (see ColumnElement) — no geometry needed; focused controls
    // guard on their own focus state.
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (Element child : this.children) {
            if (child.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (Element child : this.children) {
            if (child.charTyped(codePoint, modifiers)) return true;
        }
        return false;
    }
}
