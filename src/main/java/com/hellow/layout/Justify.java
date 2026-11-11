package com.hellow.layout;

/**
 * How a {@link com.hellow.element.RowElement} hands out its spare width along
 * the main axis. {@link #START} is the historical behavior: children keep their
 * natural width, the whole block starts at the left edge, and only weighted
 * children absorb the leftover space.
 *
 * <p>{@link #EQUAL} overrides child widths instead of only shifting them, which
 * is what tab rows and toolbars actually want — see {@code JustifyMath.equalWidths}.
 */
public enum Justify {
    /** Pack at the left edge (default; matches pre-existing behavior). */
    START,
    /** Center the whole block. */
    CENTER,
    /** Pack at the right edge. */
    END,
    /** Equal gaps between children; no gap at the edges. */
    SPACE_BETWEEN,
    /** Half-sized gaps at both edges, full gaps between children. */
    SPACE_AROUND,
    /** One equal gap between every pair of children and at both edges. */
    SPACE_EVENLY,
    /** Every child gets the same slot width; the row fills the container exactly. */
    EQUAL;

    /** True when this mode overrides child widths instead of only offsetting them. */
    public boolean overridesWidths() {
        return this == EQUAL;
    }
}
