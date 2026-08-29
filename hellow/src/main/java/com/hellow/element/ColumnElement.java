package com.hellow.element;

import com.hellow.layout.ContentAlign;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class ColumnElement implements Element {
    private final List<Element> children;
    private final int gap;
    private final ContentAlign align;
    private int cachedWidth = -1;
    private int[] cachedHeights;
    private int[] cachedWidths;

    public ColumnElement(List<Element> children, int gap) {
        this(children, gap, ContentAlign.LEFT_TOP);
    }

    /**
     * @param align controls the horizontal placement of each child within the column
     *              width (its horizontal component). Children keep their natural
     *              width; only the position inside the column shifts.
     */
    public ColumnElement(List<Element> children, int gap, ContentAlign align) {
        this.children = children;
        this.gap = gap;
        this.align = align == null ? ContentAlign.LEFT_TOP : align;
    }

    public int width(Font font) {
        int w = 0;
        for (Element child : this.children) w = Math.max(w, child.width(font));
        return w;
    }

    public int height(Font font, int width) {
        ensureMeasures(font, width);
        int h = 0;
        for (int ch : this.cachedHeights) h += ch;
        if (this.cachedHeights.length > 0) h += this.gap * (this.cachedHeights.length - 1);
        return h;
    }

    public void render(RenderContext ctx) {
        ensureMeasures(ctx.font(), ctx.width());
        int y = ctx.y();
        for (int i = 0; i < this.children.size(); i++) {
            Element child = this.children.get(i);
            int cw = this.cachedWidths[i];
            int cx = this.align.x(ctx.x(), ctx.width(), cw);
            child.render(new RenderContext(ctx.g(), ctx.font(), cx, y, cw, ctx.mouseX(), ctx.mouseY()));
            y += this.cachedHeights[i] + this.gap;
        }
    }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        ensureMeasures(ctx.font(), ctx.width());
        int y = ctx.y();
        for (int i = 0; i < this.children.size(); i++) {
            Element child = this.children.get(i);
            int cw = this.cachedWidths[i];
            int cx = this.align.x(ctx.x(), ctx.width(), cw);
            RenderContext c = new RenderContext(ctx.g(), ctx.font(), cx, y, cw, ctx.mouseX(), ctx.mouseY());
            if (child.mouseClicked(c, button)) return true;
            y += this.cachedHeights[i] + this.gap;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(RenderContext ctx, double dX, double dY) {
        ensureMeasures(ctx.font(), ctx.width());
        int y = ctx.y();
        for (int i = 0; i < this.children.size(); i++) {
            Element child = this.children.get(i);
            int cw = this.cachedWidths[i];
            int cx = this.align.x(ctx.x(), ctx.width(), cw);
            RenderContext c = new RenderContext(ctx.g(), ctx.font(), cx, y, cw, ctx.mouseX(), ctx.mouseY());
            if (child.mouseScrolled(c, dX, dY)) return true;
            y += this.cachedHeights[i] + this.gap;
        }
        return false;
    }

    @Override
    public Component tooltipAt(RenderContext ctx) {
        ensureMeasures(ctx.font(), ctx.width());
        int y = ctx.y();
        for (int i = 0; i < this.children.size(); i++) {
            Element child = this.children.get(i);
            int cw = this.cachedWidths[i];
            int cx = this.align.x(ctx.x(), ctx.width(), cw);
            Component tooltip = child.tooltipAt(new RenderContext(ctx.g(), ctx.font(), cx, y, cw, ctx.mouseX(), ctx.mouseY()));
            if (tooltip != null) return tooltip;
            y += this.cachedHeights[i] + this.gap;
        }
        return null;
    }

    @Override
    public boolean mouseDragged(RenderContext ctx, int button, double dragX, double dragY) {
        ensureMeasures(ctx.font(), ctx.width());
        int y = ctx.y();
        for (int i = 0; i < this.children.size(); i++) {
            Element child = this.children.get(i);
            int cw = this.cachedWidths[i];
            int cx = this.align.x(ctx.x(), ctx.width(), cw);
            RenderContext c = new RenderContext(ctx.g(), ctx.font(), cx, y, cw, ctx.mouseX(), ctx.mouseY());
            if (child.mouseDragged(c, button, dragX, dragY)) return true;
            y += this.cachedHeights[i] + this.gap;
        }
        return false;
    }

    // Keyboard events carry no geometry, so they just fan out to every child — a
    // focused control (e.g. a TextInputElement, which guards on its own focus) is
    // the only one that reacts. Without this fan-out, TextInputElement inside any
    // column could never receive typing.
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

    /**
     * Computes each child's render width and height at that width. Kept in one place so
     * {@code render()} is the truth source: {@code height()} measures children at the
     * exact width render() draws them at. {@link FillElement} children stretch over the
     * column's spare width (shared equally between multiple fills), everything else keeps
     * its natural width.
     */
    private void ensureMeasures(Font font, int width) {
        if (this.cachedWidth == width) return;
        this.cachedWidth = width;
        int n = this.children.size();
        this.cachedHeights = new int[n];
        this.cachedWidths = new int[n];
        int totalNat = 0;
        java.util.List<Integer> fills = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            Element child = this.children.get(i);
            totalNat += child.width(font);
            if (child instanceof FillElement) fills.add(i);
        }
        int spare = width - (totalNat + this.gap * Math.max(0, n - 1));
        if (fills.isEmpty() || spare <= 0) {
            for (int i = 0; i < n; i++) {
                this.cachedWidths[i] = this.children.get(i).width(font);
                this.cachedHeights[i] = this.children.get(i).height(font, this.cachedWidths[i]);
            }
            return;
        }
        // Fill = the shared spare share (never natural width PLUS spare, which would
        // push the child wider than the column); the rounding remainder goes to the
        // last fill so multiple fills truly fill the column edge-to-edge.
        int share = spare / fills.size();
        int remainder = spare % fills.size();
        for (int k = 0; k < fills.size(); k++) {
            int i = fills.get(k);
            this.cachedWidths[i] = this.children.get(i).width(font)
                    + share + (k == fills.size() - 1 ? remainder : 0);
            this.cachedHeights[i] = this.children.get(i).height(font, this.cachedWidths[i]);
        }
        for (int i = 0; i < n; i++) {
            if (this.cachedWidths[i] == 0) {
                this.cachedWidths[i] = this.children.get(i).width(font);
                this.cachedHeights[i] = this.children.get(i).height(font, this.cachedWidths[i]);
            }
        }
    }
}