package com.hellow.element;

import com.hellow.layout.FlexMath;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class GridElement implements Element, Lifecycle {
    private final List<Element> children;
    private final int columns;
    private final int gap;
    private final int[] columnWeights;
    private int cachedLayoutWidth = -1;
    private int[][] rowWidthsCache;
    private int[] rowHeightsCache;

    public GridElement(List<Element> children, int columns, int gap) {
        this(children, columns, gap, null);
    }

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
        ensureLayout(font, width);
        int h = 0;
        for (int r = 0; r < this.rowHeightsCache.length; r++) {
            h += this.rowHeightsCache[r];
            if (r < this.rowHeightsCache.length - 1) h += this.gap;
        }
        return h;
    }

    public void render(RenderContext ctx) {
        ensureLayout(ctx.font(), ctx.width());
        int y = ctx.y();
        for (int r = 0; r < this.rowWidthsCache.length; r++) {
            int[] widths = this.rowWidthsCache[r];
            int x = ctx.x();
            int rowStart = r * this.columns;
            for (int j = 0; j < widths.length; j++) {
                this.children.get(rowStart + j).render(
                        new RenderContext(ctx.g(), ctx.font(), x, y, widths[j], ctx.mouseX(), ctx.mouseY()));
                x += widths[j] + this.gap;
            }
            y += this.rowHeightsCache[r] + this.gap;
        }
    }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        ensureLayout(ctx.font(), ctx.width());
        int y = ctx.y();
        for (int r = 0; r < this.rowWidthsCache.length; r++) {
            int[] widths = this.rowWidthsCache[r];
            int x = ctx.x();
            int rowStart = r * this.columns;
            for (int j = 0; j < widths.length; j++) {
                RenderContext c = new RenderContext(ctx.g(), ctx.font(), x, y, widths[j], ctx.mouseX(), ctx.mouseY());
                if (this.children.get(rowStart + j).mouseClicked(c, button)) return true;
                x += widths[j] + this.gap;
            }
            y += this.rowHeightsCache[r] + this.gap;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(RenderContext ctx, double dX, double dY) {
        ensureLayout(ctx.font(), ctx.width());
        int y = ctx.y();
        for (int r = 0; r < this.rowWidthsCache.length; r++) {
            int[] widths = this.rowWidthsCache[r];
            int x = ctx.x();
            int rowStart = r * this.columns;
            for (int j = 0; j < widths.length; j++) {
                RenderContext c = new RenderContext(ctx.g(), ctx.font(), x, y, widths[j], ctx.mouseX(), ctx.mouseY());
                if (this.children.get(rowStart + j).mouseScrolled(c, dX, dY)) return true;
                x += widths[j] + this.gap;
            }
            y += this.rowHeightsCache[r] + this.gap;
        }
        return false;
    }

    @Override
    public Component tooltipAt(RenderContext ctx) {
        ensureLayout(ctx.font(), ctx.width());
        int y = ctx.y();
        for (int r = 0; r < this.rowWidthsCache.length; r++) {
            int[] widths = this.rowWidthsCache[r];
            int x = ctx.x();
            int rowStart = r * this.columns;
            for (int j = 0; j < widths.length; j++) {
                Component tooltip = this.children.get(rowStart + j).tooltipAt(
                        new RenderContext(ctx.g(), ctx.font(), x, y, widths[j], ctx.mouseX(), ctx.mouseY()));
                if (tooltip != null) return tooltip;
                x += widths[j] + this.gap;
            }
            y += this.rowHeightsCache[r] + this.gap;
        }
        return null;
    }

    @Override
    public boolean mouseDragged(RenderContext ctx, int button, double dragX, double dragY) {
        ensureLayout(ctx.font(), ctx.width());
        int y = ctx.y();
        for (int r = 0; r < this.rowWidthsCache.length; r++) {
            int[] widths = this.rowWidthsCache[r];
            int x = ctx.x();
            int rowStart = r * this.columns;
            for (int j = 0; j < widths.length; j++) {
                RenderContext c = new RenderContext(ctx.g(), ctx.font(), x, y, widths[j], ctx.mouseX(), ctx.mouseY());
                if (this.children.get(rowStart + j).mouseDragged(c, button, dragX, dragY)) return true;
                x += widths[j] + this.gap;
            }
            y += this.rowHeightsCache[r] + this.gap;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(RenderContext ctx, int button) {
        ensureLayout(ctx.font(), ctx.width());
        int y = ctx.y();
        for (int r = 0; r < this.rowWidthsCache.length; r++) {
            int[] widths = this.rowWidthsCache[r];
            int x = ctx.x();
            int rowStart = r * this.columns;
            for (int j = 0; j < widths.length; j++) {
                RenderContext c = new RenderContext(ctx.g(), ctx.font(), x, y, widths[j], ctx.mouseX(), ctx.mouseY());
                if (this.children.get(rowStart + j).mouseReleased(c, button)) return true;
                x += widths[j] + this.gap;
            }
            y += this.rowHeightsCache[r] + this.gap;
        }
        return false;
    }

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

    private int rows() {
        return (this.children.size() + this.columns - 1) / this.columns;
    }

    private boolean weighted() {
        if (this.columnWeights == null) return false;
        for (int w : this.columnWeights) if (w > 0) return true;
        return false;
    }

    private int[] naturalColumnWidths(Font font) {
        int[] w = new int[this.columns];
        for (int c = 0; c < this.columns; c++) {
            for (int j = c; j < this.children.size(); j += this.columns) {
                w[c] = Math.max(w[c], ElementLayouts.naturalWidth(this.children.get(j), font));
            }
        }
        return w;
    }

    private int[] columnWidths(Font font, int containerWidth) {
        if (!weighted()) return null;
        return FlexMath.gridColumnWidths(naturalColumnWidths(font), containerWidth, this.gap, this.columnWeights);
    }

    private int cellWidth(Element cell, Font font, int[] colW, int col) {
        return colW == null ? ElementLayouts.naturalWidth(cell, font) : colW[col];
    }

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
            if (ElementLayouts.of(this.children.get(j)).fill()) fillCount++;
        }
        int spare = containerWidth - (total + this.gap * Math.max(0, n - 1));
        if (fillCount > 0 && spare > 0) {
            int share = spare / fillCount;
            int remainder = spare % fillCount;
            int seen = 0;
            for (int j = rowStart; j < rowEnd; j++) {
                if (ElementLayouts.of(this.children.get(j)).fill()) {
                    seen++;
                    widths[j - rowStart] += share + (seen == fillCount ? remainder : 0);
                }
            }
        }
        for (int j = rowStart; j < rowEnd; j++) {
            widths[j - rowStart] = ElementLayouts.of(this.children.get(j)).clampWidth(widths[j - rowStart]);
        }
        return widths;
    }

    private void ensureLayout(Font font, int containerWidth) {
        if (this.cachedLayoutWidth == containerWidth && this.rowWidthsCache != null) return;
        this.cachedLayoutWidth = containerWidth;
        int rows = rows();
        int[] colW = columnWidths(font, containerWidth);
        this.rowWidthsCache = new int[rows][];
        this.rowHeightsCache = new int[rows];
        for (int r = 0; r < rows; r++) {
            int[] widths = rowRenderWidths(font, containerWidth, r, colW);
            this.rowWidthsCache[r] = widths;
            int rowStart = r * this.columns;
            int rowH = 0;
            for (int j = 0; j < widths.length; j++) {
                rowH = Math.max(rowH, ElementLayouts.naturalHeight(this.children.get(rowStart + j), font, widths[j]));
            }
            this.rowHeightsCache[r] = rowH;
        }
    }
}
