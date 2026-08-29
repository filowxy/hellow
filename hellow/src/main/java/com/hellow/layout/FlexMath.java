package com.hellow.layout;

/**
 * Pure, Minecraft-free layout math shared by flex-capable containers.
 *
 * <p>Intentionally free of {@link net.minecraft.client.gui.Font} and MC types so it can
 * be unit-tested without the client classpath.
 *
 * <p>(ponytail: int-based proportional split; the integer rounding remainder is given
 * to the last flex child so the row truly fills. Upgrade path if fractional weights are
 * ever needed: compute shares in {@code long}/{@code double} and only truncate once.)
 */
public final class FlexMath {
    private FlexMath() {}

    /**
     * Distribute {@code available} width across children laid out side-by-side with
     * {@code gap} between them.
     *
     * @param naturals  each child's natural width (index-aligned with {@code weights})
     * @param available total width handed to the row
     * @param gap       spacing between consecutive children
     * @param weights   flex weight per child; 0 means "not flexible"
     * @return per-child render width, never smaller than the natural width
     */
    public static int[] rowWidths(int[] naturals, int available, int gap, int[] weights) {
        int n = naturals.length;
        int[] out = naturals.clone();
        if (n == 0) return out;

        int totalNat = 0;
        for (int w : naturals) totalNat += w;
        totalNat += gap * (n - 1);

        int weightSum = 0;
        for (int w : weights) weightSum += w;
        if (weightSum == 0) return out;

        int extra = Math.max(0, available - totalNat);
        int allocated = 0;
        for (int i = 0; i < n; i++) {
            if (weights[i] > 0) {
                int share = (int) (extra * weights[i] / (double) weightSum);
                out[i] = naturals[i] + share;
                allocated += share;
            }
        }
        // Give the rounding remainder to the last flex child so the row truly fills.
        int remainder = extra - allocated;
        for (int i = n - 1; i >= 0 && remainder != 0; i--) {
            if (weights[i] > 0) {
                out[i] += remainder;
                remainder = 0;
            }
        }
        return out;
    }

    /**
     * Distribute {@code available} width across grid columns with {@code gap} between them.
     * A grid row is laid out side-by-side exactly like a row, so this shares
     * {@link #rowWidths}; the separate entry point only documents grid semantics and stays
     * consistent if grid column math ever diverges.
     *
     * @param naturals  each column's natural width (max of its cells)
     * @param available total width handed to the grid
     * @param gap       spacing between consecutive columns
     * @param weights   flex weight per column; 0 means "not flexible"
     * @return per-column render width, never smaller than the natural width
     */
    public static int[] gridColumnWidths(int[] naturals, int available, int gap, int[] weights) {
        return rowWidths(naturals, available, gap, weights);
    }
}
