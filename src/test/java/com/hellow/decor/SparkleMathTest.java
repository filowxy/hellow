package com.hellow.decor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure sparkle arithmetic — no Minecraft types involved. */
class SparkleMathTest {

    @Test
    void speedCapKeepsAParticleInsideTheElementForItsWholeLife() {
        float lifeSeconds = 5f;
        float speed = SparkleMath.maxSpeed(70, lifeSeconds);
        assertTrue(speed * lifeSeconds <= 70 - 8 + 0.001f,
                "a spark must dissolve before crossing the element");
    }

    @Test
    void speedHasALowerBoundForTinyElements() {
        assertEquals(6f, SparkleMath.maxSpeed(5, 5f), 0.001f);
    }

    @Test
    void fadeEasesInAndOut() {
        assertEquals(0f, SparkleMath.fade(0, 2000), 0.0001f);
        assertEquals(1f, SparkleMath.fade(1000, 2000), 0.0001f);
        assertEquals(0f, SparkleMath.fade(2000, 2000), 0.0001f);
    }

    @Test
    void fadeToleratesOutOfRangeAges() {
        assertEquals(0f, SparkleMath.fade(-500, 2000), 0.0001f);
        assertEquals(0f, SparkleMath.fade(9000, 2000), 0.0001f);
        assertEquals(0f, SparkleMath.fade(10, 0), 0.0001f);
    }

    @Test
    void clampHandlesInvertedRanges() {
        assertEquals(5, SparkleMath.clamp(9, 1, 5));
        assertEquals(1, SparkleMath.clamp(-3, 1, 5));
        // An element narrower than the particle still yields min, never a crash.
        assertEquals(1, SparkleMath.clamp(4, 1, 0));
    }
}
