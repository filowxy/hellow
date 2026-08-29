package com.hellow.element;

import com.hellow.style.HellowTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.function.Function;

public final class ExpandableListElement implements Element {
    private static final int ITEM_INDENT = 12;
    private static final String TOGGLE_COLLAPSED = "[+] ";
    private static final String TOGGLE_EXPANDED = "[-] ";

    private final String header;
    private final List<String> items;
    private final Function<String, Element> itemRenderer;
    private final int gap;
    private final HellowTheme theme;
    private boolean expanded = false;

    public ExpandableListElement(List<String> items, Function<String, Element> itemRenderer, int gap) {
        this("Expandable list", items, itemRenderer, gap, HellowTheme.DEFAULT);
    }

    public ExpandableListElement(String header, List<String> items,
                                 Function<String, Element> itemRenderer, int gap) {
        this(header, items, itemRenderer, gap, HellowTheme.DEFAULT);
    }

    public ExpandableListElement(String header, List<String> items,
                                 Function<String, Element> itemRenderer, int gap, HellowTheme theme) {
        this.header = header;
        this.items = items;
        this.itemRenderer = itemRenderer;
        this.gap = gap;
        this.theme = theme == null ? HellowTheme.DEFAULT : theme;
    }

    public int width(Font font) {
        int w = font.width(this.header) + TOGGLE_EXPANDED.length() * 6;
        for (String item : this.items) {
            w = Math.max(w, ITEM_INDENT + this.itemRenderer.apply(item).width(font));
        }
        return w;
    }

    public int height(Font font, int width) {
        int h = font.lineHeight;
        if (this.expanded) {
            int itemW = itemWidth(width);
            for (int i = 0; i < this.items.size(); i++) {
                h += this.itemRenderer.apply(this.items.get(i)).height(font, itemW);
                if (i < this.items.size() - 1) h += this.gap;
            }
        }
        return h;
    }

    public void render(RenderContext ctx) {
        Font font = ctx.font();
        String toggle = this.expanded ? TOGGLE_EXPANDED : TOGGLE_COLLAPSED;
        int headerColor = this.expanded ? this.theme.expandHeaderTextHover() : this.theme.expandHeaderText();
        ctx.g().drawString(font, toggle + this.header, ctx.x(), ctx.y(), headerColor, true);
        if (!this.expanded) return;

        int y = ctx.y() + font.lineHeight + this.gap;
        for (String item : this.items) {
            Element e = this.itemRenderer.apply(item);
            int h = e.height(font, itemWidth(ctx.width()));
            e.render(new RenderContext(ctx.g(), font, ctx.x() + ITEM_INDENT, y,
                    ctx.width() - ITEM_INDENT, ctx.mouseX(), ctx.mouseY()));
            y += h + this.gap;
        }
    }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        if (button != 0) return false;
        Font font = ctx.font();
        // Clicking the header row toggles expand/collapse.
        if (ctx.mouseY() >= ctx.y() && ctx.mouseY() <= ctx.y() + font.lineHeight) {
            this.expanded = !this.expanded;
            return true;
        }
        if (!this.expanded) return false;

        int y = ctx.y() + font.lineHeight + this.gap;
        for (String item : this.items) {
            Element e = this.itemRenderer.apply(item);
            int h = e.height(font, itemWidth(ctx.width()));
            RenderContext c = new RenderContext(ctx.g(), font, ctx.x() + ITEM_INDENT, y,
                    ctx.width() - ITEM_INDENT, ctx.mouseX(), ctx.mouseY());
            if (e.mouseClicked(c, button)) return true;
            y += h + this.gap;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(RenderContext ctx, double dX, double dY) {
        if (!this.expanded) return false;
        Font font = ctx.font();
        int y = ctx.y() + font.lineHeight + this.gap;
        for (String item : this.items) {
            Element e = this.itemRenderer.apply(item);
            int h = e.height(font, itemWidth(ctx.width()));
            RenderContext c = new RenderContext(ctx.g(), font, ctx.x() + ITEM_INDENT, y,
                    ctx.width() - ITEM_INDENT, ctx.mouseX(), ctx.mouseY());
            if (e.mouseScrolled(c, dX, dY)) return true;
            y += h + this.gap;
        }
        return false;
    }

    @Override
    public Component tooltipAt(RenderContext ctx) {
        Font font = ctx.font();
        if (ctx.mouseY() >= ctx.y() && ctx.mouseY() <= ctx.y() + font.lineHeight) return null;
        if (!this.expanded) return null;
        int y = ctx.y() + font.lineHeight + this.gap;
        for (String item : this.items) {
            Element e = this.itemRenderer.apply(item);
            int h = e.height(font, itemWidth(ctx.width()));
            Component tooltip = e.tooltipAt(new RenderContext(ctx.g(), font, ctx.x() + ITEM_INDENT, y,
                    ctx.width() - ITEM_INDENT, ctx.mouseX(), ctx.mouseY()));
            if (tooltip != null) return tooltip;
            y += h + this.gap;
        }
        return null;
    }

    @Override
    public boolean mouseDragged(RenderContext ctx, int button, double dragX, double dragY) {
        Font font = ctx.font();
        if (!this.expanded) return false;
        int y = ctx.y() + font.lineHeight + this.gap;
        for (String item : this.items) {
            Element e = this.itemRenderer.apply(item);
            int h = e.height(font, itemWidth(ctx.width()));
            RenderContext c = new RenderContext(ctx.g(), font, ctx.x() + ITEM_INDENT, y,
                    ctx.width() - ITEM_INDENT, ctx.mouseX(), ctx.mouseY());
            if (e.mouseDragged(c, button, dragX, dragY)) return true;
            y += h + this.gap;
        }
        return false;
    }

    public boolean expanded() { return this.expanded; }
    public void setExpanded(boolean expanded) { this.expanded = expanded; }

    /** Width an expanded item renders at (the container width minus the indent). */
    private static int itemWidth(int containerWidth) {
        return Math.max(0, containerWidth - ITEM_INDENT);
    }
}
