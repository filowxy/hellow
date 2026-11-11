package com.hellow.decor;

/**
 * Pure particle arithmetic shared by {@link Sparkles}. Kept free of Minecraft
 * types so the behavior can be unit-tested without the client classpath.
 */
final class SparkleMath {
    static final long LIFE_MIN_MS = 2000;
    static final long LIFE_MAX_MS = 5000;

    private SparkleMath() {}

    /**
     * Upper bound on upward speed so a spark cannot traverse the full height in
     * its lifetime. Never below 6 px/s: a floor keeps the effect alive in a very
     * short element even though the bound is then clamped at draw time.
     */
    static float maxSpeed(int height, float lifeSeconds) {
        return Math.max(6f, (height - 8f) / Math.max(0.001f, lifeSeconds));
    }

    /** Ease-in, ease-out opacity for a particle's lifetime, in {@code 0..1}. */
    static float fade(long ageMs, long lifeMs) {
        if (lifeMs <= 0) return 0f;
        float t = Math.max(0f, Math.min(1f, ageMs / (float) lifeMs));
        return (float) Math.sin(t * Math.PI);
    }

    /** Clamp into {@code [min, max]}, tolerating an inverted range. */
    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(Math.max(min, max), value));
    }
}
