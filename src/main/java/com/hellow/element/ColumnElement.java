package com.hellow.element;

import com.hellow.layout.ContentAlign;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class ColumnElement implements Element, Lifecycle {
    private final List<Element> children;
    private final int gap;
    private final ContentAlign align;
    private int cachedWidth = -1;
    private int[] cachedHeights;
    private int[] cachedWidths;

    public ColumnElement(List<Element> children, int gap) {
        this(children, gap, ContentAlign.LEFT_TOP);
    }

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
            int cx = ElementLayouts.of(child).alignOr(this.align).x(ctx.x(), ctx.width(), cw);
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
            int cx = ElementLayouts.of(child).alignOr(this.align).x(ctx.x(), ctx.width(), cw);
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
            int cx = ElementLayouts.of(child).alignOr(this.align).x(ctx.x(), ctx.width(), cw);
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
            int cx = ElementLayouts.of(child).alignOr(this.align).x(ctx.x(), ctx.width(), cw);
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
            int cx = ElementLayouts.of(child).alignOr(this.align).x(ctx.x(), ctx.width(), cw);
            RenderContext c = new RenderContext(ctx.g(), ctx.font(), cx, y, cw, ctx.mouseX(), ctx.mouseY());
            if (child.mouseDragged(c, button, dragX, dragY)) return true;
            y += this.cachedHeights[i] + this.gap;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(RenderContext ctx, int button) {
        ensureMeasures(ctx.font(), ctx.width());
        int y = ctx.y();
        for (int i = 0; i < this.children.size(); i++) {
            Element child = this.children.get(i);
            int cw = this.cachedWidths[i];
            int cx = ElementLayouts.of(child).alignOr(this.align).x(ctx.x(), ctx.width(), cw);
            RenderContext c = new RenderContext(ctx.g(), ctx.font(), cx, y, cw, ctx.mouseX(), ctx.mouseY());
            if (child.mouseReleased(c, button)) return true;
            y += this.cachedHeights[i] + this.gap;
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

    private void ensureMeasures(Font font, int width) {
        if (this.cachedWidth == width) return;
        this.cachedWidth = width;
        int n = this.children.size();
        this.cachedHeights = new int[n];
        this.cachedWidths = new int[n];
        int[] natural = new int[n];
        int totalNat = 0;
        int fillCount = 0;
        for (int i = 0; i < n; i++) {
            Element child = this.children.get(i);
            natural[i] = ElementLayouts.naturalWidth(child, font);
            totalNat += natural[i];
            if (ElementLayouts.of(child).fill()) fillCount++;
        }
        int spare = width - (totalNat + this.gap * Math.max(0, n - 1));
        if (fillCount > 0 && spare > 0) {
            int share = spare / fillCount;
            int remainder = spare % fillCount;
            int seen = 0;
            for (int i = 0; i < n; i++) {
                Element child = this.children.get(i);
                if (ElementLayouts.of(child).fill()) {
                    seen++;
                    // Fill = the shared spare share (never natural width PLUS spare,
                    // which would push the child wider than the column); the rounding
                    // remainder goes to the last fill so the column fills exactly.
                    this.cachedWidths[i] = natural[i] + share + (seen == fillCount ? remainder : 0);
                } else {
                    this.cachedWidths[i] = natural[i];
                }
                this.cachedWidths[i] = ElementLayouts.of(child).clampWidth(this.cachedWidths[i]);
            }
        } else {
            for (int i = 0; i < n; i++) {
                this.cachedWidths[i] = ElementLayouts.of(this.children.get(i)).clampWidth(natural[i]);
            }
        }
        for (int i = 0; i < n; i++) {
            this.cachedHeights[i] = ElementLayouts.naturalHeight(this.children.get(i), font, this.cachedWidths[i]);
        }
    }
}
