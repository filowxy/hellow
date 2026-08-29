package com.hellow.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Runnable checks for {@link FlexMath} — the pure flex width-allocation math that
 * {@code RowElement} delegates to. Kept free of Minecraft types so it runs on the
 * plain unit-test classpath.
 */
class FlexMathTest {

    @Test
    void noFlexReturnsNaturals() {
        assertArrayEquals(new int[]{10, 20},
                FlexMath.rowWidths(new int[]{10, 20}, 100, 4, new int[]{0, 0}));
    }

    @Test
    void emptyRowReturnsEmpty() {
        assertArrayEquals(new int[]{},
                FlexMath.rowWidths(new int[]{}, 100, 4, new int[]{}));
    }

    @Test
    void distributesSpareProportionally() {
        // naturals 10+20 + gap 4 = 34; available 54 -> extra 20; weights 1:3 -> +5, +15
        assertArrayEquals(new int[]{15, 35},
                FlexMath.rowWidths(new int[]{10, 20}, 54, 4, new int[]{1, 3}));
    }

    @Test
    void roundingRemainderGoesToLastFlexChild() {
        // extra 21 -> shares 5.25/15.75 truncate to 5/15, remainder 1 -> last child
        assertArrayEquals(new int[]{15, 36},
                FlexMath.rowWidths(new int[]{10, 20}, 55, 4, new int[]{1, 3}));
    }

    @Test
    void neverGrowsWhenNoSpareWidth() {
        // available == total natural -> no growth
        assertArrayEquals(new int[]{10, 20},
                FlexMath.rowWidths(new int[]{10, 20}, 34, 4, new int[]{1, 1}));
    }

    @Test
    void neverShrinksBelowNaturalWhenTight() {
        // available < total natural -> children stay at natural (may overflow, never clip)
        assertArrayEquals(new int[]{10, 20},
                FlexMath.rowWidths(new int[]{10, 20}, 20, 4, new int[]{1, 1}));
    }

    @Test
    void singleFlexChildFillsAllRemainder() {
        assertArrayEquals(new int[]{30},
                FlexMath.rowWidths(new int[]{10}, 30, 0, new int[]{2}));
    }

    @Test
    void gridAllZeroWeightsDegradesToNaturals() {
        assertArrayEquals(new int[]{40, 30, 20},
                FlexMath.gridColumnWidths(new int[]{40, 30, 20}, 200, 6, new int[]{0, 0, 0}));
    }

    @Test
    void gridSplitsSpareProportionally() {
        // naturals 40+20 + gap 10 = 70; available 100 -> extra 30; weights 1:2 -> +10, +20
        assertArrayEquals(new int[]{50, 40},
                FlexMath.gridColumnWidths(new int[]{40, 20}, 100, 10, new int[]{1, 2}));
    }

    @Test
    void gridRoundingRemainderGoesToLastFlexColumn() {
        // extra 31 -> shares 10.33/20.66 truncate to 10/20, remainder 1 -> last column
        assertArrayEquals(new int[]{50, 41},
                FlexMath.gridColumnWidths(new int[]{40, 20}, 101, 10, new int[]{1, 2}));
    }

    @Test
    void gridNoSpareWidthKeepsNaturals() {
        assertArrayEquals(new int[]{40, 20},
                FlexMath.gridColumnWidths(new int[]{40, 20}, 70, 10, new int[]{1, 1}));
    }
}
