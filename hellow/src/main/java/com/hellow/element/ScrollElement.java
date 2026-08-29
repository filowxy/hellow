package com.hellow.element;

import com.hellow.state.State;
import com.hellow.style.HellowTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class ScrollElement implements Element {
    /** Scroll offset advance per wheel notch (px). */
    private static final int SCROLL_STEP = 24;
    /** Per-tick velocity decay for the fling (inertia) after the wheel stops. */
    private static final float FLING_DECAY = 0.82f;
    /** Velocity below which the fling is considered done. */
    private static final float FLING_STOP = 1.0f;
    private static final int SCROLLBAR_WIDTH = 2;
    private static final int SCROLLBAR_MARGIN = 2;
    private static final int MIN_THUMB_HEIGHT = 8;

    private final List<Element> children;
    private final int gap;
    private final int viewportHeight;
    private float flingVelocity;

    /**
     * Optional external holder for the scroll offset. When provided, the scroll
     * position is read from / written to this state instead of the instance field,
     * so a rebuilt element tree (e.g. a re-created page) can restore the position
     * by passing the same state back.
     */
    private final State<Integer> scrollState;
    private final HellowTheme theme;
    private int scroll;
    private int cachedWidth = -1;
    private int[] cachedHeights;

    public ScrollElement(List<Element> children, int gap, int viewportHeight) {
        this(children, gap, viewportHeight, null);
    }

    /**
     * @param scrollState optional external holder (see {@link #scrollState}); may be
     *                    {@code null} to keep the position in the instance field
     */
    public ScrollElement(List<Element> children, int gap, int viewportHeight, State<Integer> scrollState) {
        this(children, gap, viewportHeight, scrollState, HellowTheme.DEFAULT);
    }

    public ScrollElement(List<Element> children, int gap, int viewportHeight,
                         State<Integer> scrollState, HellowTheme theme) {
        this.children = children;
        this.gap = gap;
        this.viewportHeight = Math.max(0, viewportHeight);
        this.scrollState = scrollState;
        this.theme = theme == null ? HellowTheme.DEFAULT : theme;
        this.scroll = scrollState == null ? 0 : scrollState.get();
    }

    public int width(Font font) {
        int w = 0;
        for (Element child : this.children) w = Math.max(w, child.width(font));
        return w;
    }

    public int height(Font font, int width) {
        return Math.min(contentHeight(font, width), this.viewportHeight);
    }

    public void render(RenderContext ctx) {
        Font font = ctx.font();
        ensureHeights(font, ctx.width());
        int y = ctx.y() - scroll();
        // Clip children to the viewport: a child whose top is inside the viewport but
        // whose bottom sticks out must not paint past the scroll region.
        boolean clipped = this.viewportHeight > 0;
        if (clipped) {
            ctx.g().enableScissor(ctx.x(), ctx.y(), ctx.x() + ctx.width(), ctx.y() + this.viewportHeight);
        }
        for (int i = 0; i < this.children.size(); i++) {
            int ch = this.cachedHeights[i];
            if (y + ch >= ctx.y() && y <= ctx.y() + this.viewportHeight) {
                this.children.get(i).render(new RenderContext(ctx.g(), font, ctx.x(), y, ctx.width(), ctx.mouseX(), ctx.mouseY()));
            }
            y += ch + this.gap;
            if (y >= ctx.y() + this.viewportHeight) break;
        }
        if (clipped) ctx.g().disableScissor();
        renderScrollbar(ctx);
    }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        // The scrollbar owns its strip first: rows span the full container width
        // (including the scrollbar column), so without this a row element's hit test
        // would swallow track clicks before they reach the scrollbar.
        if (button == 0 && inScrollbar(ctx) && maxScroll(ctx) > 0) {
            scrollTo(ctx, ctx.mouseY());
            return true;
        }
        // Only rows the pointer can actually be hovering get the click: a row whose
        // bottom sticks out below the viewport would otherwise answer clicks in the
        // empty strip past the scrolled-away content ("click through scissor").
        if (ctx.mouseY() < ctx.y() || ctx.mouseY() > ctx.y() + this.viewportHeight) return false;
        int y = ctx.y() - scroll();
        for (int i = 0; i < this.children.size(); i++) {
            int ch = heightAt(ctx.font(), ctx.width(), i);
            if (y + ch >= ctx.y() && y <= ctx.y() + this.viewportHeight) {
                RenderContext c = new RenderContext(ctx.g(), ctx.font(), ctx.x(), y, ctx.width(), ctx.mouseX(), ctx.mouseY());
                if (this.children.get(i).mouseClicked(c, button)) return true;
            }
            y += ch + this.gap;
            if (y >= ctx.y() + this.viewportHeight) break;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(RenderContext ctx, double dX, double dY) {
        if (ctx.mouseX() < ctx.x() || ctx.mouseX() > ctx.x() + ctx.width()) return false;
        if (ctx.mouseY() < ctx.y() || ctx.mouseY() > ctx.y() + this.viewportHeight) return false;
        int max = maxScroll(ctx);
        setScroll(Math.max(0, Math.min(max, scroll() - (int) dY * SCROLL_STEP)));
        // Seed the fling with the wheel speed (continuously refreshed while scrolling).
        this.flingVelocity = (float) -dY * SCROLL_STEP;
        return true;
    }

    /**
     * Advance the inertia fling by one client tick (called from the owning screen's
     * {@code tick()}). Returns true while still moving.
     */
    public boolean advanceFling(RenderContext ctx) {
        if (this.flingVelocity == 0f) return false;
        int max = maxScroll(ctx);
        setScroll(Math.max(0, Math.min(max, scroll() + (int) this.flingVelocity)));
        this.flingVelocity *= FLING_DECAY;
        if (Math.abs(this.flingVelocity) < FLING_STOP) this.flingVelocity = 0f;
        return this.flingVelocity != 0f;
    }

    @Override
    public boolean mouseDragged(RenderContext ctx, int button, double dragX, double dragY) {
        // Scrollbar first (same reasoning as mouseClicked).
        if (inScrollbar(ctx) && maxScroll(ctx) > 0) {
            scrollTo(ctx, ctx.mouseY());
            return true;
        }
        // Forward to visible children only while the pointer is inside the viewport
        // (e.g. a nested slider being dragged) — see mouseClicked.
        if (ctx.mouseY() < ctx.y() || ctx.mouseY() > ctx.y() + this.viewportHeight) return false;
        int y = ctx.y() - scroll();
        for (int i = 0; i < this.children.size(); i++) {
            int ch = heightAt(ctx.font(), ctx.width(), i);
            if (y + ch >= ctx.y() && y <= ctx.y() + this.viewportHeight) {
                RenderContext c = new RenderContext(ctx.g(), ctx.font(), ctx.x(), y, ctx.width(), ctx.mouseX(), ctx.mouseY());
                if (this.children.get(i).mouseDragged(c, button, dragX, dragY)) return true;
            }
            y += ch + this.gap;
            if (y >= ctx.y() + this.viewportHeight) break;
        }
        return false;
    }

    @Override
    public Component tooltipAt(RenderContext ctx) {
        // Nothing to hover outside the viewport — a half-visible row must not cast a
        // tooltip into the scrolled-away strip below it.
        if (ctx.mouseY() < ctx.y() || ctx.mouseY() > ctx.y() + this.viewportHeight) return null;
        int y = ctx.y() - scroll();
        for (int i = 0; i < this.children.size(); i++) {
            int ch = heightAt(ctx.font(), ctx.width(), i);
            if (y + ch >= ctx.y() && y <= ctx.y() + this.viewportHeight) {
                RenderContext c = new RenderContext(ctx.g(), ctx.font(), ctx.x(), y, ctx.width(), ctx.mouseX(), ctx.mouseY());
                Component tooltip = this.children.get(i).tooltipAt(c);
                if (tooltip != null) return tooltip;
            }
            y += ch + this.gap;
            if (y >= ctx.y() + this.viewportHeight) break;
        }
        return null;
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

    // -------- Scroll offset access (state-backed when provided) --------

    private int scroll() {
        return this.scrollState == null ? this.scroll : this.scrollState.get();
    }

    private void setScroll(int value) {
        this.scroll = value;
        if (this.scrollState != null) this.scrollState.set(value);
    }

    // -------- Scrollbar --------

    private int maxScroll(RenderContext ctx) {
        return Math.max(0, contentHeight(ctx.font(), ctx.width()) - this.viewportHeight);
    }

    private int trackX(RenderContext ctx) {
        return ctx.x() + ctx.width() - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN;
    }

    private boolean inScrollbar(RenderContext ctx) {
        return ctx.mouseX() >= trackX(ctx) && ctx.mouseX() <= trackX(ctx) + SCROLLBAR_WIDTH
                && ctx.mouseY() >= ctx.y() && ctx.mouseY() <= ctx.y() + this.viewportHeight;
    }

    private void scrollTo(RenderContext ctx, double mouseY) {
        int max = maxScroll(ctx);
        if (max <= 0) return;
        int content = contentHeight(ctx.font(), ctx.width());
        int thumbH = Math.max(MIN_THUMB_HEIGHT, this.viewportHeight * this.viewportHeight / content);
        int trackH = Math.max(1, this.viewportHeight - thumbH);
        float f = (float) (mouseY - ctx.y() - thumbH / 2.0) / trackH;
        setScroll(Math.max(0, Math.min(max, (int) (f * max))));
    }

    private void renderScrollbar(RenderContext ctx) {
        int max = maxScroll(ctx);
        if (max <= 0) return;
        int content = contentHeight(ctx.font(), ctx.width());
        int trackX = trackX(ctx);
        ctx.g().fill(trackX, ctx.y(), trackX + SCROLLBAR_WIDTH, ctx.y() + this.viewportHeight, this.theme.scrollbarTrack());
        int thumbH = Math.max(MIN_THUMB_HEIGHT, this.viewportHeight * this.viewportHeight / content);
        int thumbY = ctx.y() + (this.viewportHeight - thumbH) * scroll() / max;
        ctx.g().fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbH, this.theme.scrollbarThumb());
    }

    // -------- Content measurement --------

    private int contentHeight(Font font, int width) {
        ensureHeights(font, width);
        int h = 0;
        for (int ch : this.cachedHeights) h += ch;
        if (this.cachedHeights.length > 0) h += this.gap * (this.cachedHeights.length - 1);
        return h;
    }

    private int heightAt(Font font, int width, int i) {
        ensureHeights(font, width);
        return this.cachedHeights[i];
    }

    private void ensureHeights(Font font, int width) {
        if (this.cachedWidth == width) return;
        this.cachedWidth = width;
        this.cachedHeights = new int[this.children.size()];
        for (int i = 0; i < this.children.size(); i++) this.cachedHeights[i] = this.children.get(i).height(font, width);
    }
}
