package com.hellow.layout;

/**
 * Declarative layout intent for a single element: how much spare space it may
 * absorb, whether it should stretch across its container, the width bounds it
 * must stay inside, and how it aligns inside the slot its parent gives it.
 *
 * <p>This is data, not a wrapper type. Containers ask
 * {@code ElementLayouts.of(child)} and never inspect the child's concrete class,
 * so a new layout capability does not require a new wrapper element. The legacy
 * {@link com.hellow.element.FlexElement} / {@link com.hellow.element.FillElement}
 * wrappers keep working: the resolver maps them onto the matching layout.
 *
 * <p>Deliberately Minecraft-free so the arithmetic is unit-testable without the
 * client classpath.
 *
 * <p>(ponytail: width bounds only. Height bounds would need a clip to be honest —
 * clamping a measured height just draws the child outside its slot — so they stay
 * out until a clipping container exists. Margin stays {@code BoxElement}'s job.)
 *
 * @param weight   share of leftover main-axis space; 0 = not flexible
 * @param fill     stretch over the container's spare cross-axis space
 * @param minWidth lower width bound in pixels, {@link #UNSET} for none
 * @param maxWidth upper width bound in pixels, {@link #UNSET} for none
 * @param align    alignment inside the parent's slot, {@code null} = inherit the
 *                 parent container's alignment
 */
public record ElementLayout(int weight, boolean fill, int minWidth, int maxWidth, ContentAlign align) {

    /** Sentinel for "no bound". Negative widths are never meaningful. */
    public static final int UNSET = -1;

    /** Natural size, no flexibility, inherit alignment. */
    public static final ElementLayout NATURAL = new ElementLayout(0, false, UNSET, UNSET, null);

    /** Stretch over the container's spare space. */
    public static final ElementLayout FILL = new ElementLayout(0, true, UNSET, UNSET, null);

    public ElementLayout {
        weight = Math.max(0, weight);
        if (minWidth < 0) minWidth = UNSET;
        if (maxWidth < 0) maxWidth = UNSET;
        // A min above the max is a contradiction; the upper bound wins.
        if (minWidth != UNSET && maxWidth != UNSET && minWidth > maxWidth) minWidth = maxWidth;
    }

    /** A weighted layout with no fill and no bounds. */
    public static ElementLayout weighted(int weight) {
        return new ElementLayout(weight, false, UNSET, UNSET, null);
    }

    public static ElementLayout fill(boolean fill) {
        return fill ? FILL : NATURAL;
    }

    /**
     * Clamp a natural width to this layout's bounds. Never negative, and
     * idempotent so a parent and the wrapper can both apply it safely.
     */
    public int clampWidth(int natural) {
        int w = Math.max(0, natural);
        if (this.minWidth != UNSET) w = Math.max(w, this.minWidth);
        if (this.maxWidth != UNSET) w = Math.min(w, this.maxWidth);
        return w;
    }

    /** The child's alignment, or the container's when the child did not set one. */
    public ContentAlign alignOr(ContentAlign fallback) {
        return this.align == null ? fallback : this.align;
    }

    public ElementLayout withWeight(int weight) {
        return new ElementLayout(weight, this.fill, this.minWidth, this.maxWidth, this.align);
    }

    public ElementLayout withFill(boolean fill) {
        return new ElementLayout(this.weight, fill, this.minWidth, this.maxWidth, this.align);
    }

    public ElementLayout withWidthBounds(int minWidth, int maxWidth) {
        return new ElementLayout(this.weight, this.fill, minWidth, maxWidth, this.align);
    }

    public ElementLayout withAlign(ContentAlign align) {
        return new ElementLayout(this.weight, this.fill, this.minWidth, this.maxWidth, align);
    }
}
