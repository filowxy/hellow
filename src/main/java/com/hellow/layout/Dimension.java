package com.hellow.layout;

public record Dimension(int width, int height) {
    public static final Dimension ZERO = new Dimension(0, 0);
}
