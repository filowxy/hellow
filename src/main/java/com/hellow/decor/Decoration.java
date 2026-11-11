package com.hellow.decor;

import com.hellow.element.RenderContext;

/**
 * One optional visual layer wrapped around an element. Decorations are drawn
 * around the element's resolved rectangle and never take part in layout, so any
 * element — leaves and containers alike — can be decorated without touching its
 * class.
 *
 * <p>{@link #before} runs under the content (backdrops, borders, shadows),
 * {@link #after} runs over it (sparkles, highlights).
 *
 * <p>{@link #stateKey()} is what makes an animated decoration survive a rebuild:
 * {@code ListView} recreates its rows every frame, so a decoration that kept its
 * particles in an instance field would reset them sixty times a second. Returning
 * a stable key lets the effect keep its state in a registry keyed by that string.
 */
public interface Decoration {
    /** Draw under the element content. */
    default void before(RenderContext ctx, int x, int y, int width, int height) {}

    /** Draw over the element content. */
    default void after(RenderContext ctx, int x, int y, int width, int height) {}

    /** Stable identity for persisted animation state, or {@code null} for none. */
    default String stateKey() {
        return null;
    }
}
