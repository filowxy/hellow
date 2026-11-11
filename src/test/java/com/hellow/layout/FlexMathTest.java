package com.hellow.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

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
        assertArrayEquals(new int[]{15, 35},
                FlexMath.rowWidths(new int[]{10, 20}, 54, 4, new int[]{1, 3}));
    }

    @Test
    void roundingRemainderGoesToLastFlexChild() {
        assertArrayEquals(new int[]{15, 36},
                FlexMath.rowWidths(new int[]{10, 20}, 55, 4, new int[]{1, 3}));
    }

    @Test
    void neverGrowsWhenNoSpareWidth() {
        assertArrayEquals(new int[]{10, 20},
                FlexMath.rowWidths(new int[]{10, 20}, 34, 4, new int[]{1, 1}));
    }

    @Test
    void neverShrinksBelowNaturalWhenTight() {
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
        assertArrayEquals(new int[]{50, 40},
                FlexMath.gridColumnWidths(new int[]{40, 20}, 100, 10, new int[]{1, 2}));
    }

    @Test
    void gridRoundingRemainderGoesToLastFlexColumn() {
        assertArrayEquals(new int[]{50, 41},
                FlexMath.gridColumnWidths(new int[]{40, 20}, 101, 10, new int[]{1, 2}));
    }

    @Test
    void gridNoSpareWidthKeepsNaturals() {
        assertArrayEquals(new int[]{40, 20},
                FlexMath.gridColumnWidths(new int[]{40, 20}, 70, 10, new int[]{1, 1}));
    }
}
