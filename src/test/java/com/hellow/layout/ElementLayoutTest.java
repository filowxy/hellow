package com.hellow.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure layout-intent arithmetic — no Minecraft types involved. */
class ElementLayoutTest {

    @Test
    void naturalLayoutIsIdentity() {
        assertEquals(42, ElementLayout.NATURAL.clampWidth(42));
        assertEquals(0, ElementLayout.NATURAL.clampWidth(-7));
        assertEquals(0, ElementLayout.NATURAL.weight());
        assertTrue(!ElementLayout.NATURAL.fill());
    }

    @Test
    void maxWidthCapsAndMinWidthRaises() {
        ElementLayout capped = ElementLayout.NATURAL.withWidthBounds(ElementLayout.UNSET, 50);
        assertEquals(50, capped.clampWidth(120));
        assertEquals(20, capped.clampWidth(20));

        ElementLayout floored = ElementLayout.NATURAL.withWidthBounds(40, ElementLayout.UNSET);
        assertEquals(40, floored.clampWidth(10));
        assertEquals(80, floored.clampWidth(80));
    }

    @Test
    void contradictoryBoundsResolveToUpperBound() {
        // min > max is a caller mistake; the upper bound must win so the child
        // never renders wider than its declared ceiling.
        ElementLayout contradictory = ElementLayout.NATURAL.withWidthBounds(100, 40);
        assertEquals(40, contradictory.minWidth());
        assertEquals(40, contradictory.clampWidth(100));
    }

    @Test
    void negativeBoundsMeanUnset() {
        ElementLayout layout = new ElementLayout(0, false, -5, -9, null);
        assertEquals(ElementLayout.UNSET, layout.minWidth());
        assertEquals(ElementLayout.UNSET, layout.maxWidth());
        assertEquals(33, layout.clampWidth(33));
    }

    @Test
    void clampingIsIdempotent() {
        ElementLayout layout = ElementLayout.NATURAL.withWidthBounds(10, 30);
        assertEquals(30, layout.clampWidth(layout.clampWidth(99)));
    }

    @Test
    void alignFallsBackToContainer() {
        ContentAlign fallback = ContentAlign.CENTER_MIDDLE;
        assertSame(fallback, ElementLayout.NATURAL.alignOr(fallback));
        assertSame(ContentAlign.RIGHT_BOTTOM, ElementLayout.NATURAL.withAlign(ContentAlign.RIGHT_BOTTOM).alignOr(fallback));
    }

    @Test
    void negativeWeightIsTreatedAsZero() {
        assertEquals(0, ElementLayout.weighted(-3).weight());
    }
}
