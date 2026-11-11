package com.hellow.decor;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Registry behavior that keeps virtualized rows from resetting their sparks. */
class SparkleStateTest {

    @Test
    void sameKeyKeepsTheSameSwarm() {
        SparkleState state = new SparkleState(8);
        ArrayDeque<SparkleParticle> first = state.field("card-a");
        first.add(new SparkleParticle(1f, 2f, 3f, 4f, 5f, 6f, 0L, 1000L, 1));
        ArrayDeque<SparkleParticle> second = state.field("card-a");
        assertSame(first, second);
        assertEquals(1, second.size());
    }

    @Test
    void distinctKeysGetDistinctSwarms() {
        SparkleState state = new SparkleState(8);
        state.field("a");
        state.field("b");
        assertEquals(2, state.size());
    }

    @Test
    void leastRecentlyUsedSwarmsAreEvictedPastTheCap() {
        SparkleState state = new SparkleState(16);
        for (int i = 0; i < 100; i++) {
            state.field("key-" + i);
        }
        assertEquals(16, state.size());
    }

    @Test
    void clearDropsEverything() {
        SparkleState state = new SparkleState(8);
        state.field("a");
        state.clear();
        assertEquals(0, state.size());
    }
}
