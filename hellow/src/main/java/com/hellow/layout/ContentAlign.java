package com.hellow.layout;

public enum ContentAlign {
    LEFT_TOP,    CENTER_TOP,    RIGHT_TOP,
    LEFT_MIDDLE,  CENTER_MIDDLE, RIGHT_MIDDLE,
    LEFT_BOTTOM,  CENTER_BOTTOM, RIGHT_BOTTOM;

    public int x(int boxX, int boxW, int contentW) {
        return switch (horizontal()) {
            case LEFT -> boxX;
            case CENTER -> boxX + (boxW - contentW) / 2;
            case RIGHT -> boxX + boxW - contentW;
        };
    }

    public int y(int boxY, int boxH, int contentH) {
        return switch (vertical()) {
            case TOP -> boxY;
            case MIDDLE -> boxY + (boxH - contentH) / 2;
            case BOTTOM -> boxY + boxH - contentH;
        };
    }

    private H horizontal() {
        return switch (this) {
            case LEFT_TOP, LEFT_MIDDLE, LEFT_BOTTOM -> H.LEFT;
            case CENTER_TOP, CENTER_MIDDLE, CENTER_BOTTOM -> H.CENTER;
            case RIGHT_TOP, RIGHT_MIDDLE, RIGHT_BOTTOM -> H.RIGHT;
        };
    }

    private V vertical() {
        return switch (this) {
            case LEFT_TOP, CENTER_TOP, RIGHT_TOP -> V.TOP;
            case LEFT_MIDDLE, CENTER_MIDDLE, RIGHT_MIDDLE -> V.MIDDLE;
            case LEFT_BOTTOM, CENTER_BOTTOM, RIGHT_BOTTOM -> V.BOTTOM;
        };
    }

    private enum H { LEFT, CENTER, RIGHT }
    private enum V { TOP, MIDDLE, BOTTOM }
}
