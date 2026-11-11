package com.hellow.decor;

/**
 * One drifting particle. Position is fully analytic from {@code now - born}, so
 * no per-frame integration state exists and any number of elements can be drawn
 * in the same frame without sharing a clock.
 */
final class SparkleParticle {
    final float x;
    final float spawnY;
    final float vy;
    final float sway;
    final float swayFreq;
    final float phase;
    final long born;
    final long life;
    final int size;

    SparkleParticle(float x, float spawnY, float vy, float sway, float swayFreq, float phase,
                    long born, long life, int size) {
        this.x = x;
        this.spawnY = spawnY;
        this.vy = vy;
        this.sway = sway;
        this.swayFreq = swayFreq;
        this.phase = phase;
        this.born = born;
        this.life = life;
        this.size = size;
    }

    float yAt(long now) {
        return yAt(now, 0f);
    }

    /** Y on the exact path {@code backSeconds} earlier — used for the comet tail. */
    float yAt(long now, float backSeconds) {
        return this.spawnY - this.vy * ageSeconds(now, backSeconds);
    }

    float xAt(long now) {
        return xAt(now, 0f);
    }

    /** X on the exact path {@code backSeconds} earlier — used for the comet tail. */
    float xAt(long now, float backSeconds) {
        float t = ageSeconds(now, backSeconds);
        return this.x + this.sway * (float) Math.sin(t * (float) Math.PI * 2f * this.swayFreq + this.phase);
    }

    boolean deadAt(long now) {
        return now - this.born > this.life;
    }

    private float ageSeconds(long now, float backSeconds) {
        return Math.max(0f, (now - this.born) / 1000f - backSeconds);
    }
}
