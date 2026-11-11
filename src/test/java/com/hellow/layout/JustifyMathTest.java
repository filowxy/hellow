package com.hellow.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Placement rules for {@link Justify}, exercised without a Font. */
class JustifyMathTest {

    @Test
    void equalWidthsFillTheContainerExactly() {
        int[] widths = JustifyMath.equalWidths(100, 3, 5);
        assertArrayEquals(new int[]{30, 30, 30}, widths);

        // 101 leaves one pixel over; the last slot absorbs it so the row is exact.
        int[] withRemainder = JustifyMath.equalWidths(101, 3, 5);
        assertArrayEquals(new int[]{30, 30, 31}, withRemainder);
    }

    @Test
    void equalWidthsNeverGoNegative() {
        int[] widths = JustifyMath.equalWidths(4, 3, 5);
        assertEquals(0, widths[0]);
        assertEquals(0, widths[1]);
        assertEquals(0, widths[2]);
    }

    @Test
    void startPacksAtTheLeftEdge() {
        assertArrayEquals(new int[]{0, 14}, JustifyMath.offsets(Justify.START, 100, new int[]{10, 20}, 4));
    }

    @Test
    void centerAndEndShiftTheWholeBlock() {
        // content 30 + gap 4 = 34, spare 66.
        assertArrayEquals(new int[]{33, 47}, JustifyMath.offsets(Justify.CENTER, 100, new int[]{10, 20}, 4));
        assertArrayEquals(new int[]{66, 80}, JustifyMath.offsets(Justify.END, 100, new int[]{10, 20}, 4));
    }

    @Test
    void spaceBetweenPutsAllSpareBetweenChildren() {
        int[] offsets = JustifyMath.offsets(Justify.SPACE_BETWEEN, 100, new int[]{10, 20}, 4);
        assertArrayEquals(new int[]{0, 80}, offsets);
        assertEquals(100, offsets[1] + 20);
    }

    @Test
    void spaceAroundSplitsSpareIntoHalfShares() {
        // spare 66 -> a quarter (16) before the first child, three quarters
        // (49) before the second, the rest after the last.
        assertArrayEquals(new int[]{16, 63}, JustifyMath.offsets(Justify.SPACE_AROUND, 100, new int[]{10, 20}, 4));
    }

    @Test
    void spaceEvenlyUsesOneUnitAtEachEdgeAndBetween() {
        // spare 66 over three equal gaps -> 22 per gap.
        assertArrayEquals(new int[]{22, 58}, JustifyMath.offsets(Justify.SPACE_EVENLY, 100, new int[]{10, 20}, 4));
    }

    @Test
    void singleChildSpaceBetweenStaysLeft() {
        assertArrayEquals(new int[]{0}, JustifyMath.offsets(Justify.SPACE_BETWEEN, 100, new int[]{10}, 4));
    }

    @Test
    void overflowNeverProducesNegativeSpare() {
        // content 304 > box 100: spare clamps to 0 and the second child simply
        // follows the first at its natural offset (204 = 200 + gap).
        assertArrayEquals(new int[]{0, 204}, JustifyMath.offsets(Justify.SPACE_BETWEEN, 100, new int[]{200, 100}, 4));
    }

    @Test
    void emptyRowIsEmpty() {
        assertEquals(0, JustifyMath.offsets(Justify.SPACE_EVENLY, 100, new int[0], 4).length);
        assertEquals(0, JustifyMath.equalWidths(100, 0, 4).length);
    }
}
