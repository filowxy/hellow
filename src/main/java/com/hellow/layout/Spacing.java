package com.hellow.layout;

public record Spacing(int left, int top, int right, int bottom) {
    public static final Spacing NONE = new Spacing(0, 0, 0, 0);

    public static Spacing all(int v) { return new Spacing(v, v, v, v); }
    public static Spacing symmetric(int horizontal, int vertical) {
        return new Spacing(horizontal, vertical, horizontal, vertical);
    }

    public int horizontal() { return left + right; }
    public int vertical() { return top + bottom; }
}
