package com.hellow.decor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Analytic particle path: no per-frame integration state. */
class SparkleParticleTest {

    private static SparkleParticle particle() {
        return new SparkleParticle(10f, 60f, 10f, 4f, 0.3f, 0.5f, 1_000L, 4_000L, 2);
    }

    @Test
    void particleRisesOverTime() {
        SparkleParticle p = particle();
        assertTrue(p.yAt(2_000L) < p.yAt(1_000L));
    }

    @Test
    void backSamplingMatchesTheEarlierPosition() {
        SparkleParticle p = particle();
        // Sampling "1 second ago" from t=3000 must equal the position at t=2000.
        assertTrue(Math.abs(p.xAt(3_000L, 1f) - p.xAt(2_000L)) < 0.0001f);
    }

    @Test
    void swayStaysWithinItsAmplitude() {
        SparkleParticle p = particle();
        for (long t = 1_000L; t <= 5_000L; t += 250L) {
            float dx = Math.abs(p.xAt(t) - 10f);
            assertTrue(dx <= 4.0001f, "sway exceeded amplitude at t=" + t);
        }
    }

    @Test
    void ageRetiresTheParticle() {
        SparkleParticle p = particle();
        assertFalse(p.deadAt(4_000L));
        assertTrue(p.deadAt(5_001L));
    }
}
