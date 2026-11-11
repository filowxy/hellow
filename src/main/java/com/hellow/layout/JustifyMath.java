package com.hellow.layout;

/**
 * Pure, Minecraft-free placement math for {@link Justify}. Kept separate from
 * {@code RowElement} so the distribution rules are unit-testable without the
 * client classpath.
 *
 * <p>Offsets are computed analytically from the leftover space rather than by
 * accumulating rounded gaps, which keeps every mode monotonic and lets a single
 * implementation cover all of them.
 *
 * <p>(ponytail: integer division leaves up to {@code n-1} unallocated pixels in
 * the distribution modes. Sub-pixel precision is not observable at GUI scale, so
 * the remainder is simply absorbed by the right edge. Upgrade path: compute in
 * {@code long} and shed the remainder into the final gap if it ever matters.)
 */
public final class JustifyMath {
    private JustifyMath() {}

    /**
     * Equal slot widths for {@link Justify#EQUAL}: every child gets the same
     * share of {@code available} minus the gaps, and the last slot absorbs the
     * rounding remainder so the row fills the container exactly.
     */
    public static int[] equalWidths(int available, int count, int gap) {
        int[] out = new int[Math.max(0, count)];
        if (out.length == 0) return out;
        int gaps = gap * (out.length - 1);
        int share = Math.max(0, (available - gaps) / out.length);
        java.util.Arrays.fill(out, share);
        out[out.length - 1] = Math.max(0, out[out.length - 1] + available - gaps - share * out.length);
        return out;
    }

    /**
     * X offsets (relative to the row's left edge) for children whose final
     * widths are already decided.
     */
    public static int[] offsets(Justify justify, int boxWidth, int[] widths, int gap) {
        int n = widths.length;
        int[] out = new int[n];
        if (n == 0) return out;
        int content = 0;
        for (int w : widths) content += Math.max(0, w);
        int spare = Math.max(0, boxWidth - content - gap * (n - 1));
        int cursor = 0;
        for (int i = 0; i < n; i++) {
            out[i] = cursor + lead(justify, spare, i, n);
            cursor += widths[i] + gap;
        }
        return out;
    }

    /** Space placed before child {@code index} by the given distribution mode. */
    private static int lead(Justify justify, int spare, int index, int count) {
        return switch (justify) {
            case START, EQUAL -> 0;
            case CENTER -> spare / 2;
            case END -> spare;
            case SPACE_BETWEEN -> count <= 1 ? 0 : (int) ((long) spare * index / (count - 1));
            // A half-share before the first child and after the last: (2i+1)/(2n).
            case SPACE_AROUND -> (int) ((long) spare * (2L * index + 1) / (2L * count));
            // One equal gap at both edges and between every pair: (i+1)/(n+1).
            case SPACE_EVENLY -> (int) ((long) spare * (index + 1) / (count + 1));
        };
    }
}
