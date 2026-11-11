package com.hellow.layout;

public final class FlexMath {
    private FlexMath() {}

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
        int remainder = extra - allocated;
        for (int i = n - 1; i >= 0 && remainder != 0; i--) {
            if (weights[i] > 0) {
                out[i] += remainder;
                remainder = 0;
            }
        }
        return out;
    }

    public static int[] gridColumnWidths(int[] naturals, int available, int gap, int[] weights) {
        return rowWidths(naturals, available, gap, weights);
    }
}
