package com.hellow.element;

import com.hellow.layout.ContentAlign;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import java.util.List;

/**
 * Left-to-right flow that breaks onto a new line when the next child would
 * overflow.
 *
 * <p>The line budget is the width the parent actually hands down, capped by
 * {@code maxWidth}: before this, {@code maxWidth} was used verbatim in both
 * measurement and rendering, so a wrap row inside a narrower container silently
 * overflowed it. {@code maxWidth <= 0} now means "no cap".
 */
public final class WrapRowElement implements Element, Lifecycle {
    private final List<Element> children;
    private final int gap;
    private final int maxWidth;
    private final ContentAlign align;

    public WrapRowElement(List<Element> children, int gap, int maxWidth) {
        this(children, gap, maxWidth, ContentAlign.LEFT_TOP);
    }

    /** @param align horizontal placement of each line inside the available width */
    public WrapRowElement(List<Element> children, int gap, int maxWidth, ContentAlign align) {
        this.children = children;
        this.gap = gap;
        this.maxWidth = Math.max(0, maxWidth);
        this.align = align == null ? ContentAlign.LEFT_TOP : align;
    }

    public int width(Font font) {
        if (this.maxWidth > 0) return this.maxWidth;
        int w = 0;
        for (Element child : this.children) w += ElementLayouts.naturalWidth(child, font) + this.gap;
        return Math.max(0, w - this.gap);
    }

    public int height(Font font, int width) {
        int[] ys = placement(font, width)[1];
        int lowest = 0;
        for (int i = 0; i < this.children.size(); i++) {
            lowest = Math.max(lowest, ys[i] + ElementLayouts.naturalHeight(this.children.get(i), font, widths(font, width)[i]));
        }
        return lowest;
    }

    public void render(RenderContext ctx) {
        int[][] p = placement(ctx.font(), ctx.width());
        int[] widths = widths(ctx.font(), ctx.width());
        for (int i = 0; i < this.children.size(); i++) {
            this.children.get(i).render(new RenderContext(ctx.g(), ctx.font(),
                    ctx.x() + p[0][i], ctx.y() + p[1][i], widths[i], ctx.mouseX(), ctx.mouseY()));
        }
    }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        int[][] p = placement(ctx.font(), ctx.width());
        int[] widths = widths(ctx.font(), ctx.width());
        for (int i = 0; i < this.children.size(); i++) {
            RenderContext c = childContext(ctx, p, widths, i);
            if (this.children.get(i).mouseClicked(c, button)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(RenderContext ctx, double dX, double dY) {
        int[][] p = placement(ctx.font(), ctx.width());
        int[] widths = widths(ctx.font(), ctx.width());
        for (int i = 0; i < this.children.size(); i++) {
            RenderContext c = childContext(ctx, p, widths, i);
            if (this.children.get(i).mouseScrolled(c, dX, dY)) return true;
        }
        return false;
    }

    @Override
    public Component tooltipAt(RenderContext ctx) {
        int[][] p = placement(ctx.font(), ctx.width());
        int[] widths = widths(ctx.font(), ctx.width());
        for (int i = 0; i < this.children.size(); i++) {
            Component tooltip = this.children.get(i).tooltipAt(childContext(ctx, p, widths, i));
            if (tooltip != null) return tooltip;
        }
        return null;
    }

    @Override
    public boolean mouseDragged(RenderContext ctx, int button, double dragX, double dragY) {
        int[][] p = placement(ctx.font(), ctx.width());
        int[] widths = widths(ctx.font(), ctx.width());
        for (int i = 0; i < this.children.size(); i++) {
            RenderContext c = childContext(ctx, p, widths, i);
            if (this.children.get(i).mouseDragged(c, button, dragX, dragY)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(RenderContext ctx, int button) {
        int[][] p = placement(ctx.font(), ctx.width());
        int[] widths = widths(ctx.font(), ctx.width());
        for (int i = 0; i < this.children.size(); i++) {
            RenderContext c = childContext(ctx, p, widths, i);
            if (this.children.get(i).mouseReleased(c, button)) return true;
        }
        return false;
    }

    private RenderContext childContext(RenderContext ctx, int[][] p, int[] widths, int i) {
        return new RenderContext(ctx.g(), ctx.font(),
                ctx.x() + p[0][i], ctx.y() + p[1][i], widths[i], ctx.mouseX(), ctx.mouseY());
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
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        for (Element child : this.children) {
            if (child.keyReleased(keyCode, scanCode, modifiers)) return true;
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

    @Override
    public List<Element> children() {
        return this.children;
    }

    @Override
    public void onMount() {
        Lifecycle.mount(this.children);
    }

    @Override
    public void tick() {
        Lifecycle.tickAll(this.children);
    }

    @Override
    public void onUnmount() {
        Lifecycle.unmount(this.children);
    }

    /** The line budget: the parent's width, capped by {@code maxWidth}. */
    private int limit(int available) {
        int w = Math.max(0, available);
        return this.maxWidth > 0 ? Math.min(w, this.maxWidth) : w;
    }

    private int[] widths(Font font, int available) {
        int[] widths = new int[this.children.size()];
        for (int i = 0; i < widths.length; i++) {
            widths[i] = ElementLayouts.naturalWidth(this.children.get(i), font);
        }
        return widths;
    }

    /** @return {@code {xs, ys}}, both relative to the element's own origin */
    private int[][] placement(Font font, int available) {
        int limit = limit(available);
        int n = this.children.size();
        int[] widths = widths(font, available);
        int[] xs = new int[n];
        int[] ys = new int[n];
        if (n == 0) return new int[][]{xs, ys};

        int[] rowOf = new int[n];
        int row = 0;
        int cursor = 0;
        boolean rowHasChild = false;
        for (int i = 0; i < n; i++) {
            if (rowHasChild && cursor + widths[i] > limit) {
                row++;
                cursor = 0;
                rowHasChild = false;
            }
            rowOf[i] = row;
            cursor += widths[i] + this.gap;
            rowHasChild = true;
        }

        int rows = row + 1;
        int[] rowWidth = new int[rows];
        int[] rowHeight = new int[rows];
        for (int i = 0; i < n; i++) {
            rowWidth[rowOf[i]] += widths[i];
            rowHeight[rowOf[i]] = Math.max(rowHeight[rowOf[i]],
                    ElementLayouts.naturalHeight(this.children.get(i), font, widths[i]));
        }
        for (int r = 0; r < rows; r++) {
            if (rowWidth[r] > 0) rowWidth[r] += this.gap * Math.max(0, rowOfInRow(rowOf, r) - 1);
        }

        int[] rowY = new int[rows];
        int y = 0;
        for (int r = 0; r < rows; r++) {
            rowY[r] = y;
            y += rowHeight[r] + this.gap;
        }

        int[] rowCursor = new int[rows];
        for (int r = 0; r < rows; r++) {
            rowCursor[r] = this.align.x(0, limit, rowWidth[r]);
        }
        for (int i = 0; i < n; i++) {
            xs[i] = rowCursor[rowOf[i]];
            ys[i] = rowY[rowOf[i]];
            rowCursor[rowOf[i]] += widths[i] + this.gap;
        }
        return new int[][]{xs, ys};
    }

    private static int rowOfInRow(int[] rowOf, int row) {
        int count = 0;
        for (int r : rowOf) if (r == row) count++;
        return count;
    }
}
