package com.hellow.trigger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FireStrategyTest {

    @Test
    void decayingFiresOnZeroThenAfterIntervals() {
        FireStrategy s = FireStrategy.decaying(new int[]{1, 1, 3});
        assertTrue(s.shouldFire());
        s.markFired();

        assertFalse(s.shouldFire());
        s.onConditionMatch();
        assertTrue(s.shouldFire());
        s.markFired();

        s.onConditionMatch();
        assertFalse(s.shouldFire());
        s.onConditionMatch();
        assertFalse(s.shouldFire());
        s.onConditionMatch();
        assertTrue(s.shouldFire());
    }

    @Test
    void onceFiresExactlyOnce() {
        FireStrategy s = FireStrategy.once();
        assertTrue(s.shouldFire());
        s.markFired();
        assertFalse(s.shouldFire());
    }

    @Test
    void everyTimeAlwaysFires() {
        FireStrategy s = FireStrategy.everyTime();
        assertTrue(s.shouldFire());
        s.markFired();
        assertTrue(s.shouldFire());
    }
}
