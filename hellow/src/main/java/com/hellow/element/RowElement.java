package com.hellow.element;

import com.hellow.layout.ContentAlign;
import com.hellow.layout.FlexMath;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class RowElement implements Element {
    private final List<Element> children;
    private final int gap;
    private final ContentAlign align;
    private Font cachedFont;
    private int[] cachedWidths;

    public RowElement(List<Element> children, int gap) {
        this(children, gap, ContentAlign.LEFT_TOP);
    }

    /**
     * @param align controls where the row sits inside the available width (its
     *              horizontal component). When the row contains flex children the
     *              row fills the width instead, so alignment is ignored.
     */
    public RowElement(List<Element> children, int gap, ContentAlign align) {
        this.children = children;
        this.gap = gap;
        this.align = align == null ? ContentAlign.LEFT_TOP : align;
    }

    public int width(Font font) {
        int w = 0;
        for (int i = 0; i < this.children.size(); i++) {
            w += this.children.get(i).width(font);
            if (i < this.children.size() - 1) w += this.gap;
        }
        return w;
    }

    public int height(Font font, int width) {
        // Measure each child at the same per-child width render() draws it at, so a
        // width-dependent child (e.g. wrapped text in a flex slot) does not get its
        // height under-estimated and overflow the row.
        int[] widths = layoutWidths(font, width);
        int h = 0;
        for (int i = 0; i < this.children.size(); i++) {
            h = Math.max(h, this.children.get(i).height(font, widths[i]));
        }
        return h;
    }

    public void render(RenderContext ctx) {
        Font font = ctx.font();
        int[] widths = layoutWidths(font, ctx.width());
        int n = this.children.size();
        if (n == 0) return;
        int x = startX(ctx.x(), ctx.width(), widths);
        int rowH = 0;
        for (int i = 0; i < n; i++) rowH = Math.max(rowH, this.children.get(i).height(font, widths[i]));
        for (int i = 0; i < n; i++) {
            this.children.get(i).render(new RenderContext(ctx.g(), font, x, ctx.y(), widths[i], ctx.mouseX(), ctx.mouseY()));
            x += widths[i] + this.gap;
        }
    }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        Font font = ctx.font();
        int[] widths = layoutWidths(font, ctx.width());
        int n = this.children.size();
        if (n == 0) return false;
        int x = startX(ctx.x(), ctx.width(), widths);
        for (int i = 0; i < n; i++) {
            RenderContext c = new RenderContext(ctx.g(), font, x, ctx.y(), widths[i], ctx.mouseX(), ctx.mouseY());
            if (this.children.get(i).mouseClicked(c, button)) return true;
            x += widths[i] + this.gap;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(RenderContext ctx, double dX, double dY) {
        Font font = ctx.font();
        int[] widths = layoutWidths(font, ctx.width());
        int n = this.children.size();
        if (n == 0) return false;
        int x = startX(ctx.x(), ctx.width(), widths);
        for (int i = 0; i < n; i++) {
            RenderContext c = new RenderContext(ctx.g(), font, x, ctx.y(), widths[i], ctx.mouseX(), ctx.mouseY());
            if (this.children.get(i).mouseScrolled(c, dX, dY)) return true;
            x += widths[i] + this.gap;
        }
        return false;
    }

    @Override
    public Component tooltipAt(RenderContext ctx) {
        Font font = ctx.font();
        int[] widths = layoutWidths(font, ctx.width());
        int n = this.children.size();
        if (n == 0) return null;
        int x = startX(ctx.x(), ctx.width(), widths);
        for (int i = 0; i < n; i++) {
            Component tooltip = this.children.get(i).tooltipAt(
                    new RenderContext(ctx.g(), font, x, ctx.y(), widths[i], ctx.mouseX(), ctx.mouseY()));
            if (tooltip != null) return tooltip;
            x += widths[i] + this.gap;
        }
        return null;
    }

    @Override
    public boolean mouseDragged(RenderContext ctx, int button, double dragX, double dragY) {
        Font font = ctx.font();
        int[] widths = layoutWidths(font, ctx.width());
        int n = this.children.size();
        if (n == 0) return false;
        int x = startX(ctx.x(), ctx.width(), widths);
        for (int i = 0; i < n; i++) {
            RenderContext c = new RenderContext(ctx.g(), font, x, ctx.y(), widths[i], ctx.mouseX(), ctx.mouseY());
            if (this.children.get(i).mouseDragged(c, button, dragX, dragY)) return true;
            x += widths[i] + this.gap;
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

    // -------- Layout math --------

    private int[] naturalWidths(Font font) {
        if (this.cachedFont != font) {
            this.cachedFont = font;
            this.cachedWidths = new int[this.children.size()];
            for (int j = 0; j < this.children.size(); j++) this.cachedWidths[j] = this.children.get(j).width(font);
        }
        return this.cachedWidths;
    }

    private int totalWeight() {
        int w = 0;
        for (Element child : this.children) {
            if (child instanceof FlexElement f) w += f.weight();
        }
        return w;
    }

    /**
     * Per-child render widths for an available width: natural widths, plus any spare
     * space distributed to flex children proportionally to their weight.
     */
    private int[] layoutWidths(Font font, int available) {
        int[] nat = naturalWidths(font);
        int[] weights = new int[nat.length];
        for (int i = 0; i < nat.length; i++) {
            if (this.children.get(i) instanceof FlexElement f) weights[i] = f.weight();
        }
        return FlexMath.rowWidths(nat, available, this.gap, weights);
    }

    private int startX(int boxX, int boxWidth, int[] widths) {
        int total = 0;
        for (int w : widths) total += w;
        if (this.children.size() > 1) total += this.gap * (this.children.size() - 1);
        // A row that fills the available width starts at the left edge.
        if (totalWeight() > 0) return boxX;
        return this.align.x(boxX, boxWidth, total);
    }
}
