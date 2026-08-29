package com.hellow.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContentAlignTest {

    @Test
    void centerMiddlePutsContentInCenterOfBox() {
        assertEquals(45, ContentAlign.CENTER_MIDDLE.x(0, 100, 10));
        assertEquals(40, ContentAlign.CENTER_MIDDLE.y(0, 100, 20));
    }

    @Test
    void leftTopPutsContentAtBoxOrigin() {
        assertEquals(5, ContentAlign.LEFT_TOP.x(5, 100, 10));
        assertEquals(5, ContentAlign.LEFT_TOP.y(5, 100, 20));
    }

    @Test
    void rightBottomPutsContentAtBoxEnd() {
        assertEquals(90, ContentAlign.RIGHT_BOTTOM.x(0, 100, 10));
        assertEquals(80, ContentAlign.RIGHT_BOTTOM.y(0, 100, 20));
    }
}
