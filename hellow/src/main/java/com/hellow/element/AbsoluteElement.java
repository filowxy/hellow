package com.hellow.element;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class AbsoluteElement implements Element {
    public record Entry(Element element, int x, int y) {}

    private final List<Entry> entries;

    public AbsoluteElement(List<Entry> entries) { this.entries = entries; }

    public int width(Font font) {
        int w = 0;
        for (Entry e : this.entries) w = Math.max(w, e.x() + e.element().width(font));
        return w;
    }

    public int height(Font font, int width) {
        int h = 0;
        for (Entry e : this.entries) {
            // Measure each entry at the natural width it is rendered at.
            h = Math.max(h, e.y() + e.element().height(font, e.element().width(font)));
        }
        return h;
    }

    public void render(RenderContext ctx) {
        for (Entry e : this.entries) {
            e.element().render(new RenderContext(ctx.g(), ctx.font(), ctx.x() + e.x(), ctx.y() + e.y(), e.element().width(ctx.font()), ctx.mouseX(), ctx.mouseY()));
        }
    }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        for (Entry e : this.entries) {
            RenderContext c = new RenderContext(ctx.g(), ctx.font(), ctx.x() + e.x(), ctx.y() + e.y(), e.element().width(ctx.font()), ctx.mouseX(), ctx.mouseY());
            if (e.element().mouseClicked(c, button)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(RenderContext ctx, double dX, double dY) {
        for (Entry e : this.entries) {
            RenderContext c = new RenderContext(ctx.g(), ctx.font(), ctx.x() + e.x(), ctx.y() + e.y(), e.element().width(ctx.font()), ctx.mouseX(), ctx.mouseY());
            if (e.element().mouseScrolled(c, dX, dY)) return true;
        }
        return false;
    }

    @Override
    public Component tooltipAt(RenderContext ctx) {
        for (Entry e : this.entries) {
            RenderContext c = new RenderContext(ctx.g(), ctx.font(), ctx.x() + e.x(), ctx.y() + e.y(), e.element().width(ctx.font()), ctx.mouseX(), ctx.mouseY());
            Component tooltip = e.element().tooltipAt(c);
            if (tooltip != null) return tooltip;
        }
        return null;
    }

    @Override
    public boolean mouseDragged(RenderContext ctx, int button, double dragX, double dragY) {
        for (Entry e : this.entries) {
            RenderContext c = new RenderContext(ctx.g(), ctx.font(), ctx.x() + e.x(), ctx.y() + e.y(), e.element().width(ctx.font()), ctx.mouseX(), ctx.mouseY());
            if (e.element().mouseDragged(c, button, dragX, dragY)) return true;
        }
        return false;
    }

    // Keyboard fan-out (see ColumnElement) — no geometry needed; focused controls
    // guard on their own focus state.
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (Entry e : this.entries) {
            if (e.element().keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (Entry e : this.entries) {
            if (e.element().charTyped(codePoint, modifiers)) return true;
        }
        return false;
    }
}