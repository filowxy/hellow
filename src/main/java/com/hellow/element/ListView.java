package com.hellow.element;

import com.hellow.state.State;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.function.Function;

public final class ListView implements Element, Lifecycle {
    private static final int SCROLLBAR_WIDTH = 2;
    private static final int SCROLLBAR_MARGIN = 2;
    private static final int MIN_THUMB_HEIGHT = 8;
    private static final int SCROLLBAR_TRACK_COLOR = 0xFF333333;
    private static final int SCROLLBAR_THUMB_COLOR = 0xFFAAAAAA;
    private static final int SCROLL_STEP = 24;
    private static final float FLING_DECAY = 0.82f;
    private static final float FLING_STOP = 1.0f;

    private final int itemCount;
    private final int itemHeight;
    private final int gap;
    private final int viewportHeight;
    private final Function<Integer, Element> itemProvider;
    private float flingVelocity;
    private final State<Integer> scrollState;
    private int scroll;

    public ListView(int itemCount, int itemHeight, int gap, int viewportHeight,
                    Function<Integer, Element> itemProvider) {
        this(itemCount, itemHeight, gap, viewportHeight, itemProvider, null);
    }

    public ListView(int itemCount, int itemHeight, int gap, int viewportHeight,
                    Function<Integer, Element> itemProvider, State<Integer> scrollState) {
        this.itemCount = Math.max(0, itemCount);
        this.itemHeight = Math.max(1, itemHeight);
        this.gap = gap;
        this.viewportHeight = Math.max(0, viewportHeight);
        this.itemProvider = itemProvider;
        this.scrollState = scrollState;
        this.scroll = scrollState == null ? 0 : scrollState.get();
    }

    public ListView(int itemHeight, int gap, int viewportHeight, List<Element> items) {
        this(items.size(), itemHeight, gap, viewportHeight, items::get, null);
    }

    public ListView(int itemHeight, int gap, int viewportHeight, List<Element> items, State<Integer> scrollState) {
        this(items.size(), itemHeight, gap, viewportHeight, items::get, scrollState);
    }

    public int width(Font font) {
        if (this.itemCount == 0) return 0;
        Element first = this.itemProvider.apply(0);
        return first == null ? 0 : first.width(font);
    }

    public int height(Font font, int width) {
        return Math.min(contentHeight(), this.viewportHeight);
    }

    public void render(RenderContext ctx) {
        int stride = this.itemHeight + this.gap;
        int first = firstVisibleIndex();
        int y = ctx.y() - scroll() + first * stride;
        boolean clipped = this.viewportHeight > 0;
        if (clipped) {
            ctx.g().enableScissor(ctx.x(), ctx.y(), ctx.x() + ctx.width(), ctx.y() + this.viewportHeight);
        }
        for (int i = first; i < this.itemCount && y < ctx.y() + this.viewportHeight; i++) {
            this.itemProvider.apply(i).render(
                    new RenderContext(ctx.g(), ctx.font(), ctx.x(), y, ctx.width(), ctx.mouseX(), ctx.mouseY()));
            y += stride;
        }
        if (clipped) ctx.g().disableScissor();
        renderScrollbar(ctx);
    }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        if (button == 0 && inScrollbar(ctx) && maxScroll() > 0) {
            scrollTo(ctx, ctx.mouseY());
            return true;
        }
        return forwardMouse(ctx, (item, c) -> item.mouseClicked(c, button));
    }

    @Override
    public boolean mouseScrolled(RenderContext ctx, double dX, double dY) {
        if (ctx.mouseX() < ctx.x() || ctx.mouseX() > ctx.x() + ctx.width()) return false;
        if (ctx.mouseY() < ctx.y() || ctx.mouseY() > ctx.y() + this.viewportHeight) return false;
        int max = Math.max(0, contentHeight() - this.viewportHeight);
        setScroll(Math.max(0, Math.min(max, scroll() - (int) dY * SCROLL_STEP)));
        this.flingVelocity = (float) -dY * SCROLL_STEP;
        return true;
    }

    public boolean advanceFling() {
        if (this.flingVelocity == 0f) return false;
        int max = Math.max(0, contentHeight() - this.viewportHeight);
        setScroll(Math.max(0, Math.min(max, scroll() + (int) this.flingVelocity)));
        this.flingVelocity *= FLING_DECAY;
        if (Math.abs(this.flingVelocity) < FLING_STOP) this.flingVelocity = 0f;
        return this.flingVelocity != 0f;
    }

    @Override
    public boolean mouseDragged(RenderContext ctx, int button, double dragX, double dragY) {
        if (inScrollbar(ctx) && maxScroll() > 0) {
            scrollTo(ctx, ctx.mouseY());
            return true;
        }
        return forwardMouse(ctx, (item, c) -> item.mouseDragged(c, button, dragX, dragY));
    }

    @Override
    public boolean mouseReleased(RenderContext ctx, int button) {
        if (inScrollbar(ctx) && maxScroll() > 0) return false;
        return forwardMouse(ctx, (item, c) -> item.mouseReleased(c, button));
    }

    @Override
    public Component tooltipAt(RenderContext ctx) {
        int stride = this.itemHeight + this.gap;
        int first = firstVisibleIndex();
        int y = ctx.y() - scroll() + first * stride;
        for (int i = first; i < this.itemCount && y < ctx.y() + this.viewportHeight; i++) {
            Component tooltip = this.itemProvider.apply(i).tooltipAt(
                    new RenderContext(ctx.g(), ctx.font(), ctx.x(), y, ctx.width(), ctx.mouseX(), ctx.mouseY()));
            if (tooltip != null) return tooltip;
            y += stride;
        }
        return null;
    }

    @Override
    public void tick() {
        advanceFling();
    }

    private interface MouseHandler {
        boolean apply(Element item, RenderContext ctx);
    }

    private boolean forwardMouse(RenderContext ctx, MouseHandler handler) {
        int stride = this.itemHeight + this.gap;
        int first = firstVisibleIndex();
        int y = ctx.y() - scroll() + first * stride;
        for (int i = first; i < this.itemCount && y < ctx.y() + this.viewportHeight; i++) {
            RenderContext c = new RenderContext(ctx.g(), ctx.font(), ctx.x(), y, ctx.width(), ctx.mouseX(), ctx.mouseY());
            if (handler.apply(this.itemProvider.apply(i), c)) return true;
            y += stride;
        }
        return false;
    }

    private int firstVisibleIndex() {
        int stride = this.itemHeight + this.gap;
        return stride <= 0 ? 0 : Math.min(this.itemCount, scroll() / stride);
    }

    private int contentHeight() {
        if (this.itemCount == 0) return 0;
        return this.itemCount * this.itemHeight + (this.itemCount - 1) * this.gap;
    }

    private int scroll() {
        return this.scrollState == null ? this.scroll : this.scrollState.get();
    }

    private void setScroll(int value) {
        this.scroll = value;
        if (this.scrollState != null) this.scrollState.set(value);
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - this.viewportHeight);
    }

    private int trackX(RenderContext ctx) {
        return ctx.x() + ctx.width() - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN;
    }

    private boolean inScrollbar(RenderContext ctx) {
        return ctx.mouseX() >= trackX(ctx) && ctx.mouseX() <= trackX(ctx) + SCROLLBAR_WIDTH
                && ctx.mouseY() >= ctx.y() && ctx.mouseY() <= ctx.y() + this.viewportHeight;
    }

    private void scrollTo(RenderContext ctx, double mouseY) {
        int max = maxScroll();
        if (max <= 0) return;
        int thumbH = Math.max(MIN_THUMB_HEIGHT, this.viewportHeight * this.viewportHeight / contentHeight());
        int trackH = Math.max(1, this.viewportHeight - thumbH);
        float f = (float) (mouseY - ctx.y() - thumbH / 2.0) / trackH;
        setScroll(Math.max(0, Math.min(max, (int) (f * max))));
    }

    private void renderScrollbar(RenderContext ctx) {
        int max = maxScroll();
        if (max <= 0) return;
        int trackX = trackX(ctx);
        ctx.g().fill(trackX, ctx.y(), trackX + SCROLLBAR_WIDTH, ctx.y() + this.viewportHeight, SCROLLBAR_TRACK_COLOR);
        int thumbH = Math.max(MIN_THUMB_HEIGHT, this.viewportHeight * this.viewportHeight / contentHeight());
        int thumbY = ctx.y() + (this.viewportHeight - thumbH) * scroll() / max;
        ctx.g().fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbH, SCROLLBAR_THUMB_COLOR);
    }
}
