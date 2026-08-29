package com.hellow.element;

import com.hellow.state.State;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import java.util.function.Function;

/**
 * A virtualized-style vertical list with a fixed row height: items are built on
 * demand via {@code itemProvider} and only the rows inside the viewport are
 * constructed/rendered each frame. This replaces the "build 500 element objects up
 * front" pattern for long lists (e.g. a mod list).
 *
 * <p>(ponytail: rows are re-built per frame, so row elements should be cheap to
 * construct — building a {@link ButtonElement} etc. per visible row is fine. If rows
 * become expensive, cache them by index; the viewport culling already bounds the
 * per-frame work to the visible rows.)
 */
public final class ListView implements Element {
    private static final int SCROLLBAR_WIDTH = 2;
    private static final int SCROLLBAR_MARGIN = 2;
    private static final int MIN_THUMB_HEIGHT = 8;
    private static final int SCROLLBAR_TRACK_COLOR = 0xFF333333;
    private static final int SCROLLBAR_THUMB_COLOR = 0xFFAAAAAA;

    /** Scroll offset advance per wheel notch (px) — feels responsive without jumping. */
    private static final int SCROLL_STEP = 24;
    /** Per-tick velocity decay for the fling (inertia) after the wheel stops. */
    private static final float FLING_DECAY = 0.82f;
    /** Velocity below which the fling is considered done. */
    private static final float FLING_STOP = 1.0f;

    private final int itemCount;
    private final int itemHeight;
    private final int gap;
    private final int viewportHeight;
    private final Function<Integer, Element> itemProvider;
    private float flingVelocity;

    /**
     * Optional external holder for the scroll offset. When provided, the position is
     * read from / written to this state instead of the instance field, so a rebuilt
     * element tree can restore the position by passing the same state back.
     */
    private final State<Integer> scrollState;
    private int scroll;

    public ListView(int itemCount, int itemHeight, int gap, int viewportHeight,
                    Function<Integer, Element> itemProvider) {
        this(itemCount, itemHeight, gap, viewportHeight, itemProvider, null);
    }

    /**
     * @param scrollState optional external holder (see {@link #scrollState}); may be
     *                    {@code null} to keep the position in the instance field
     */
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

    public int width(Font font) {
        // Best-effort natural width from the first item (defensive: a provider that
        // yields null for index 0 must not NPE the layout pass).
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
        // Clip rows to the viewport: a row whose top is inside the viewport but whose
        // bottom sticks out would otherwise paint past the list ("list overflows").
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
        // The scrollbar owns its strip first: row children span the full container
        // width (including the scrollbar column), so without this a row element's
        // isMouseOver would swallow track clicks before they reach the scrollbar.
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
        // Seed the fling with the wheel speed (continuously refreshed while scrolling).
        this.flingVelocity = (float) -dY * SCROLL_STEP;
        return true;
    }

    /**
     * Advance the inertia fling by one client tick (called from the owning screen's
     * {@code tick()}). Returns true while the list is still moving, so callers can
     * skip extra work while idle.
     */
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
        // Scrollbar first (same reasoning as mouseClicked).
        if (inScrollbar(ctx) && maxScroll() > 0) {
            scrollTo(ctx, ctx.mouseY());
            return true;
        }
        return forwardMouse(ctx, (item, c) -> item.mouseDragged(c, button, dragX, dragY));
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

    // -------- Scroll offset access (state-backed when provided) --------

    private int scroll() {
        return this.scrollState == null ? this.scroll : this.scrollState.get();
    }

    private void setScroll(int value) {
        this.scroll = value;
        if (this.scrollState != null) this.scrollState.set(value);
    }

    // -------- Scrollbar (same look as ScrollElement) --------

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
