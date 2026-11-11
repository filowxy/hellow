package com.hellow.element;

import com.hellow.layout.ContentAlign;
import com.hellow.layout.FlexMath;
import com.hellow.layout.Justify;
import com.hellow.layout.JustifyMath;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class RowElement implements Element, Lifecycle {
    private final List<Element> children;
    private final int gap;
    private final ContentAlign align;
    private final Justify justify;
    private Font cachedFont;
    private int[] cachedWidths;

    public RowElement(List<Element> children, int gap) {
        this(children, gap, ContentAlign.LEFT_TOP, Justify.START);
    }

    public RowElement(List<Element> children, int gap, ContentAlign align) {
        this(children, gap, align, Justify.START);
    }

    /**
     * @param justify how spare width is handed out; {@link Justify#START} keeps
     *                the historical behavior (natural widths, left-packed, only
     *                weighted children grow)
     */
    public RowElement(List<Element> children, int gap, ContentAlign align, Justify justify) {
        this.children = children;
        this.gap = gap;
        this.align = align == null ? ContentAlign.LEFT_TOP : align;
        this.justify = justify == null ? Justify.START : justify;
    }

    public int width(Font font) {
        int w = 0;
        for (int i = 0; i < this.children.size(); i++) {
            w += ElementLayouts.naturalWidth(this.children.get(i), font);
            if (i < this.children.size() - 1) w += this.gap;
        }
        return w;
    }

    public int height(Font font, int width) {
        int[] widths = layoutWidths(font, width);
        int h = 0;
        for (int i = 0; i < this.children.size(); i++) {
            h = Math.max(h, ElementLayouts.naturalHeight(this.children.get(i), font, widths[i]));
        }
        return h;
    }

    public void render(RenderContext ctx) {
        Font font = ctx.font();
        int[] widths = layoutWidths(font, ctx.width());
        int[] xs = JustifyMath.offsets(this.justify, ctx.width(), widths, this.gap);
        int rowH = rowHeight(font, widths);
        for (int i = 0; i < this.children.size(); i++) {
            Element child = this.children.get(i);
            int ch = ElementLayouts.naturalHeight(child, font, widths[i]);
            int cy = ElementLayouts.of(child).alignOr(this.align).y(ctx.y(), rowH, ch);
            child.render(new RenderContext(ctx.g(), font, ctx.x() + xs[i], cy, widths[i], ctx.mouseX(), ctx.mouseY()));
        }
    }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        Font font = ctx.font();
        int[] widths = layoutWidths(font, ctx.width());
        int[] xs = JustifyMath.offsets(this.justify, ctx.width(), widths, this.gap);
        int rowH = rowHeight(font, widths);
        for (int i = 0; i < this.children.size(); i++) {
            Element child = this.children.get(i);
            int ch = ElementLayouts.naturalHeight(child, font, widths[i]);
            int cy = ElementLayouts.of(child).alignOr(this.align).y(ctx.y(), rowH, ch);
            RenderContext c = new RenderContext(ctx.g(), font, ctx.x() + xs[i], cy, widths[i], ctx.mouseX(), ctx.mouseY());
            if (child.mouseClicked(c, button)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(RenderContext ctx, double dX, double dY) {
        Font font = ctx.font();
        int[] widths = layoutWidths(font, ctx.width());
        int[] xs = JustifyMath.offsets(this.justify, ctx.width(), widths, this.gap);
        int rowH = rowHeight(font, widths);
        for (int i = 0; i < this.children.size(); i++) {
            Element child = this.children.get(i);
            int ch = ElementLayouts.naturalHeight(child, font, widths[i]);
            int cy = ElementLayouts.of(child).alignOr(this.align).y(ctx.y(), rowH, ch);
            RenderContext c = new RenderContext(ctx.g(), font, ctx.x() + xs[i], cy, widths[i], ctx.mouseX(), ctx.mouseY());
            if (child.mouseScrolled(c, dX, dY)) return true;
        }
        return false;
    }

    @Override
    public Component tooltipAt(RenderContext ctx) {
        Font font = ctx.font();
        int[] widths = layoutWidths(font, ctx.width());
        int[] xs = JustifyMath.offsets(this.justify, ctx.width(), widths, this.gap);
        int rowH = rowHeight(font, widths);
        for (int i = 0; i < this.children.size(); i++) {
            Element child = this.children.get(i);
            int ch = ElementLayouts.naturalHeight(child, font, widths[i]);
            int cy = ElementLayouts.of(child).alignOr(this.align).y(ctx.y(), rowH, ch);
            Component tooltip = child.tooltipAt(
                    new RenderContext(ctx.g(), font, ctx.x() + xs[i], cy, widths[i], ctx.mouseX(), ctx.mouseY()));
            if (tooltip != null) return tooltip;
        }
        return null;
    }

    @Override
    public boolean mouseDragged(RenderContext ctx, int button, double dragX, double dragY) {
        Font font = ctx.font();
        int[] widths = layoutWidths(font, ctx.width());
        int[] xs = JustifyMath.offsets(this.justify, ctx.width(), widths, this.gap);
        int rowH = rowHeight(font, widths);
        for (int i = 0; i < this.children.size(); i++) {
            Element child = this.children.get(i);
            int ch = ElementLayouts.naturalHeight(child, font, widths[i]);
            int cy = ElementLayouts.of(child).alignOr(this.align).y(ctx.y(), rowH, ch);
            RenderContext c = new RenderContext(ctx.g(), font, ctx.x() + xs[i], cy, widths[i], ctx.mouseX(), ctx.mouseY());
            if (child.mouseDragged(c, button, dragX, dragY)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(RenderContext ctx, int button) {
        Font font = ctx.font();
        int[] widths = layoutWidths(font, ctx.width());
        int[] xs = JustifyMath.offsets(this.justify, ctx.width(), widths, this.gap);
        int rowH = rowHeight(font, widths);
        for (int i = 0; i < this.children.size(); i++) {
            Element child = this.children.get(i);
            int ch = ElementLayouts.naturalHeight(child, font, widths[i]);
            int cy = ElementLayouts.of(child).alignOr(this.align).y(ctx.y(), rowH, ch);
            RenderContext c = new RenderContext(ctx.g(), font, ctx.x() + xs[i], cy, widths[i], ctx.mouseX(), ctx.mouseY());
            if (child.mouseReleased(c, button)) return true;
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

    private int[] naturalWidths(Font font) {
        if (this.cachedFont != font) {
            this.cachedFont = font;
            this.cachedWidths = new int[this.children.size()];
            for (int j = 0; j < this.children.size(); j++) {
                this.cachedWidths[j] = ElementLayouts.naturalWidth(this.children.get(j), font);
            }
        }
        return this.cachedWidths;
    }

    private int[] layoutWidths(Font font, int available) {
        int[] nat = naturalWidths(font);
        if (this.justify.overridesWidths()) {
            // Equal slots ignore natural width on purpose: that is what makes a
            // tab row fill exactly the container instead of overflowing it.
            return JustifyMath.equalWidths(available, nat.length, this.gap);
        }
        int[] weights = new int[nat.length];
        for (int i = 0; i < nat.length; i++) {
            weights[i] = ElementLayouts.of(this.children.get(i)).weight();
        }
        int[] widths = FlexMath.rowWidths(nat, available, this.gap, weights);
        // Bounds stay hard even after flex handed the child extra space.
        for (int i = 0; i < widths.length; i++) {
            widths[i] = ElementLayouts.of(this.children.get(i)).clampWidth(widths[i]);
        }
        return widths;
    }

    private int rowHeight(Font font, int[] widths) {
        int h = 0;
        for (int i = 0; i < this.children.size(); i++) {
            h = Math.max(h, ElementLayouts.naturalHeight(this.children.get(i), font, widths[i]));
        }
        return h;
    }
}
