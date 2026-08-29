package com.hellow.page;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Hellow's window manager.
 *
 * <p>Keeps a stack of the pages currently open as windows, so closing one returns to
 * the screen it was opened from, and so callers can query/interact with the stack via
 * {@link #closeTop()}, {@link #closeAll()} and {@link #isShowing(Page)}.
 *
 * <p>Two layers exist:
 * <ul>
 *   <li>the {@link PageStack}-style modal stack ({@link Deque}) for normal/modal pages;</li>
 *   <li>a set of {@code TOP} pages that float above everything (never popped by closing
 *       the page below them).</li>
 * </ul>
 *
 * <p>{@link PageScreen} reports its own close through {@link #onPageClosed(Page)}, which
 * keeps {@link #isShowing(Page)} accurate when the user presses ESC.
 */
public final class PageScheduler {
    private static final Deque<Page> STACK = new ArrayDeque<>();
    private static final Set<Page> TOP_PAGES = new LinkedHashSet<>();

    private PageScheduler() {}

    /**
     * Open {@code page} as a window, remembering {@code parent} so that closing the
     * window returns to it. TOP-layer pages are tracked separately from the modal stack.
     */
    public static synchronized void show(Page page, Screen parent) {
        if (page == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (page instanceof PageSpec spec && spec.layer() == PageLayer.TOP) {
            TOP_PAGES.add(page);
        } else {
            STACK.push(page);
        }
        mc.setScreen(new PageScreen(page, parent));
    }

    /** Open {@code page} parented to the currently-active screen. */
    public static synchronized void show(Page page) {
        show(page, Minecraft.getInstance().screen);
    }

    /** Open a page by its registered id (no-op if the id is unknown). */
    public static synchronized void requestShow(String pageId) {
        Page page = HellowRegistry.get(PageId.of(pageId));
        if (page != null) show(page);
    }

    /**
     * Called by {@link PageScreen#onClose()} / {@code removed()} when a page window
     * actually closes, so the window stack stays in sync. The screen itself handles
     * returning to its parent.
     *
     * <p>(ponytail: stack membership uses reference identity, not {@code equals()} —
     * {@link PageSpec} is a record so two equal-valued instances would otherwise
     * collide in contains/remove and delete the wrong window.)
     */
    public static synchronized void onPageClosed(Page page) {
        if (page == null) return;
        TOP_PAGES.removeIf(p -> p == page);
        STACK.removeIf(p -> p == page);
    }

    /** Close the topmost page window (ESC-equivalent). No-op if none is open. */
    public static synchronized void closeTop() {
        Screen now = Minecraft.getInstance().screen;
        if (now instanceof PageScreen ps) {
            ps.onClose();
        }
    }

    /** Close every open page window, unwinding the whole stack. */
    public static synchronized void closeAll() {
        Screen now;
        while ((now = Minecraft.getInstance().screen) instanceof PageScreen ps) {
            ps.onClose();
        }
    }

    /** Whether {@code page} is currently open in any window layer (reference identity). */
    public static synchronized boolean isShowing(Page page) {
        if (page == null) return false;
        for (Page p : STACK) {
            if (p == page) return true;
        }
        for (Page p : TOP_PAGES) {
            if (p == page) return true;
        }
        return false;
    }

    /** Drop all window state. Intended for tests and mod teardown. */
    public static synchronized void clear() {
        STACK.clear();
        TOP_PAGES.clear();
    }
}
