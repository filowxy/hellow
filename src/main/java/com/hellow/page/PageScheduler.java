package com.hellow.page;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;

public final class PageScheduler {
    private static final Deque<Page> STACK = new ArrayDeque<>();
    private static final Set<Page> TOP_PAGES = new LinkedHashSet<>();

    private PageScheduler() {}

    public static synchronized void show(Page page, Screen parent) {
        if (page == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (page instanceof PageSpec spec && spec.layer() == PageLayer.TOP) {
            TOP_PAGES.add(page);
        } else {
            STACK.push(page);
        }
        mc.setScreen(new PageScreen(page, parent, page instanceof PageSpec spec ? spec.theme() : null));
    }

    public static synchronized void show(Page page) {
        show(page, Minecraft.getInstance().screen);
    }

    public static synchronized void requestShow(String pageId) {
        Page page = HellowRegistry.get(PageId.of(pageId));
        if (page != null) show(page);
    }

    public static synchronized void onPageClosed(Page page) {
        if (page == null) return;
        TOP_PAGES.removeIf(p -> p == page);
        STACK.removeIf(p -> p == page);
    }

    public static synchronized void closeTop() {
        Screen now = Minecraft.getInstance().screen;
        if (now instanceof PageScreen ps) {
            ps.onClose();
        }
    }

    public static synchronized void closeAll() {
        Screen now;
        while ((now = Minecraft.getInstance().screen) instanceof PageScreen ps) {
            ps.onClose();
        }
    }

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

    public static synchronized void clear() {
        STACK.clear();
        TOP_PAGES.clear();
    }
}
