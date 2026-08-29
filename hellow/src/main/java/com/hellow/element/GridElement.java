package com.hellow.element;

import com.hellow.layout.FlexMath;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class GridElement implements Element {
    private final List<Element> children;
    private final int columns;
    private final int gap;
    private final int[] columnWeights;

    public GridElement(List<Element> children, int columns, int gap) {
        this(children, columns, gap, null);
    }

    /**
     * @param columnWeights per-column flex weight; length must equal {@code columns}.
     *                      A non-zero weight makes that column absorb spare container
     *                      width proportionally (percentage-column effect). All-zero or
     *                      {@code null} keeps the legacy natural-width behavior.
     */
    public GridElement(List<Element> children, int columns, int gap, int[] columnWeights) {
        this.children = children;
        this.columns = Math.max(1, columns);
        this.gap = gap;
        if (columnWeights != null && columnWeights.length != this.columns) {
            throw new IllegalArgumentException(
                    "columnWeights length must equal columns (" + this.columns + "), got " + columnWeights.length);
        }
        this.columnWeights = columnWeights;
    }

    public int width(Font font) {
        int maxW = 0;
        for (int i = 0; i < this.children.size(); i += this.columns) {
            int w = 0;
            for (int j = i; j < Math.min(i + this.columns, this.children.size()); j++) {
                w += this.children.get(j).width(font);
                if (j < Math.min(i + this.columns, this.children.size()) - 1) w += this.gap;
            }
            maxW = Math.max(maxW, w);
        }
        return maxW;
    }

    public int height(Font font, int width) {
        int rows = rows();
        int h = 0;
        int[] colW = columnWidths(font, width);
        for (int r = 0; r < rows; r++) {
            h += rowHeight(font, r, colW, width);
            if (r < rows - 1) h += this.gap;
        }
        return h;
    }

    /**
     * Height of one grid row, measured at each cell's render width — the same width the
     * cell is drawn at (column width when weighted, natural width otherwise) — so
     * width-dependent cells (e.g. wrapped text) always fit the drawn row.
     */
    private int rowHeight(Font font, int row, int[] colW, int containerWidth) {
        int rowH = 0;
        int rowStart = row * this.columns;
        int rowEnd = Math.min(rowStart + this.columns, this.children.size());
        int[] widths = rowRenderWidths(font, containerWidth, row, colW);
        for (int j = rowStart; j < rowEnd; j++) {
            Element cell = this.children.get(j);
            rowH = Math.max(rowH, cell.height(font, widths[j - rowStart]));
        }
        return rowH;
    }

    public void render(RenderContext ctx) {
        int[] colW = columnWidths(ctx.font(), ctx.width());
        int y = ctx.y();
        int rows = rows();
        for (int r = 0; r < rows; r++) {
            int rowH = rowHeight(ctx.font(), r, colW, ctx.width());
            int x = ctx.x();
            int rowStart = r * this.columns;
            int rowEnd = Math.min(rowStart + this.columns, this.children.size());
            for (int j = rowStart; j < rowEnd; j++) {
                int w = cellWidthIn(ctx.font(), ctx.width(), r, j, colW);
                this.children.get(j).render(new RenderContext(ctx.g(), ctx.font(), x, y, w, ctx.mouseX(), ctx.mouseY()));
                x += w + this.gap;
            }
            y += rowH + this.gap;
        }
    }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        int[] colW = columnWidths(ctx.font(), ctx.width());
        int y = ctx.y();
        int rows = rows();
        for (int r = 0; r < rows; r++) {
            int rowH = rowHeight(ctx.font(), r, colW, ctx.width());
            int x = ctx.x();
            int rowStart = r * this.columns;
            int rowEnd = Math.min(rowStart + this.columns, this.children.size());
            for (int j = rowStart; j < rowEnd; j++) {
                int w = cellWidthIn(ctx.font(), ctx.width(), r, j, colW);
                RenderContext c = new RenderContext(ctx.g(), ctx.font(), x, y, w, ctx.mouseX(), ctx.mouseY());
                if (this.children.get(j).mouseClicked(c, button)) return true;
                x += w + this.gap;
            }
            y += rowH + this.gap;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(RenderContext ctx, double dX, double dY) {
        int[] colW = columnWidths(ctx.font(), ctx.width());
        int y = ctx.y();
        int rows = rows();
        for (int r = 0; r < rows; r++) {
            int rowH = rowHeight(ctx.font(), r, colW, ctx.width());
            int x = ctx.x();
            int rowStart = r * this.columns;
            int rowEnd = Math.min(rowStart + this.columns, this.children.size());
            for (int j = rowStart; j < rowEnd; j++) {
                int w = cellWidthIn(ctx.font(), ctx.width(), r, j, colW);
                RenderContext c = new RenderContext(ctx.g(), ctx.font(), x, y, w, ctx.mouseX(), ctx.mouseY());
                if (this.children.get(j).mouseScrolled(c, dX, dY)) return true;
                x += w + this.gap;
            }
            y += rowH + this.gap;
        }
        return false;
    }

    @Override
    public Component tooltipAt(RenderContext ctx) {
        int[] colW = columnWidths(ctx.font(), ctx.width());
        int y = ctx.y();
        int rows = rows();
        for (int r = 0; r < rows; r++) {
            int rowH = rowHeight(ctx.font(), r, colW, ctx.width());
            int x = ctx.x();
            int rowStart = r * this.columns;
            int rowEnd = Math.min(rowStart + this.columns, this.children.size());
            for (int j = rowStart; j < rowEnd; j++) {
                int w = cellWidthIn(ctx.font(), ctx.width(), r, j, colW);
                Component tooltip = this.children.get(j).tooltipAt(
                        new RenderContext(ctx.g(), ctx.font(), x, y, w, ctx.mouseX(), ctx.mouseY()));
                if (tooltip != null) return tooltip;
                x += w + this.gap;
            }
            y += rowH + this.gap;
        }
        return null;
    }

    @Override
    public boolean mouseDragged(RenderContext ctx, int button, double dragX, double dragY) {
        int[] colW = columnWidths(ctx.font(), ctx.width());
        int y = ctx.y();
        int rows = rows();
        for (int r = 0; r < rows; r++) {
            int rowH = rowHeight(ctx.font(), r, colW, ctx.width());
            int x = ctx.x();
            int rowStart = r * this.columns;
            int rowEnd = Math.min(rowStart + this.columns, this.children.size());
            for (int j = rowStart; j < rowEnd; j++) {
                int w = cellWidthIn(ctx.font(), ctx.width(), r, j, colW);
                RenderContext c = new RenderContext(ctx.g(), ctx.font(), x, y, w, ctx.mouseX(), ctx.mouseY());
                if (this.children.get(j).mouseDragged(c, button, dragX, dragY)) return true;
                x += w + this.gap;
            }
            y += rowH + this.gap;
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

    // -------- Column width helpers --------

    private int rows() {
        return (this.children.size() + this.columns - 1) / this.columns;
    }

    /** True when at least one column carries a positive flex weight. */
    private boolean weighted() {
        if (this.columnWeights == null) return false;
        for (int w : this.columnWeights) if (w > 0) return true;
        return false;
    }

    /** Per-column natural width (max of that column's cells); empty columns measure 0. */
    private int[] naturalColumnWidths(Font font) {
        int[] w = new int[this.columns];
        for (int c = 0; c < this.columns; c++) {
            for (int j = c; j < this.children.size(); j += this.columns) {
                w[c] = Math.max(w[c], this.children.get(j).width(font));
            }
        }
        return w;
    }

    /**
     * Per-column render width when weighted, {@code null} to keep the legacy
     * per-cell natural width behavior. Container width is only spread to columns when
     * there is spare room; otherwise columns keep their natural width.
     */
    private int[] columnWidths(Font font, int containerWidth) {
        if (!weighted()) return null;
        return FlexMath.gridColumnWidths(naturalColumnWidths(font), containerWidth, this.gap, this.columnWeights);
    }

    private int cellWidth(Element cell, Font font, int[] colW, int col) {
        return colW == null ? cell.width(font) : colW[col];
    }

    /**
     * Per-present-cell render width in one row: column width when weighted, natural
     * width otherwise, and with any spare container width given to {@link FillElement}
     * cells (shared equally). {@code colW == null} encodes "unweighted".
     */
    private int[] rowRenderWidths(Font font, int containerWidth, int row, int[] colW) {
        int rowStart = row * this.columns;
        int rowEnd = Math.min(rowStart + this.columns, this.children.size());
        int n = rowEnd - rowStart;
        int[] widths = new int[n];
        int total = 0;
        int fillCount = 0;
        for (int j = rowStart; j < rowEnd; j++) {
            int w = cellWidth(this.children.get(j), font, colW, j % this.columns);
            widths[j - rowStart] = w;
            total += w;
            if (this.children.get(j) instanceof FillElement) fillCount++;
        }
        int spare = containerWidth - (total + this.gap * Math.max(0, n - 1));
        if (fillCount > 0 && spare > 0) {
            // Equal shares, rounding remainder to the last fill (same convention as
            // FlexMath) so multiple fills cover the row edge-to-edge.
            int share = spare / fillCount;
            int remainder = spare % fillCount;
            int seen = 0;
            for (int j = rowStart; j < rowEnd; j++) {
                if (this.children.get(j) instanceof FillElement) {
                    seen++;
                    widths[j - rowStart] += share + (seen == fillCount ? remainder : 0);
                }
            }
        }
        return widths;
    }

    private int cellWidthIn(Font font, int containerWidth, int row, int j, int[] colW) {
        return rowRenderWidths(font, containerWidth, row, colW)[j - row * this.columns];
    }
}