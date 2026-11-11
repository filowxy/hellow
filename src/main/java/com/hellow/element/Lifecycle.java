package com.hellow.element;

import java.util.List;

public interface Lifecycle {
    default void onMount() {}
    default void onUnmount() {}
    default void tick() {}

    static void mount(List<? extends Element> children) {
        for (Element child : children) {
            mountOne(child);
        }
    }

    static void tickAll(List<? extends Element> children) {
        for (Element child : children) {
            tickOne(child);
        }
    }

    static void unmount(List<? extends Element> children) {
        for (Element child : children) {
            unmountOne(child);
        }
    }

    static void mountOne(Element child) {
        if (child instanceof Lifecycle lc) lc.onMount();
    }

    static void tickOne(Element child) {
        if (child instanceof Lifecycle lc) lc.tick();
    }

    static void unmountOne(Element child) {
        if (child instanceof Lifecycle lc) lc.onUnmount();
    }
}
