package com.hellow.decor;

import com.hellow.element.RenderContext;
import com.hellow.style.HellowColor;

import java.util.ArrayDeque;
import java.util.Random;

/**
 * Drifting particles inside the element rectangle — the generic version of the
 * card sparks TISAM hand-wrote.
 *
 * <p>Two constraints are baked in because both were learned the hard way there:
 *
 * <ul>
 *   <li>Motion is analytic ({@code now - born} sampled directly), never integrated
 *       with a per-frame delta. Several decorated elements render in the same
 *       frame, and a shared delta clock would freeze every swarm but the first.</li>
 *   <li>State lives in {@link SparkleState}, keyed by {@link #stateKey()}, so a
 *       virtualized list rebuilding its rows does not reset the particles.</li>
 * </ul>
 *
 * <p>Spawn speed is capped so a spark dissolves before it can reach the opposite
 * edge; age alone therefore retires it, with no bounds bookkeeping.
 */
public final class Sparkles implements Decoration {
    private static final int MAX_SWARMS = 128;
    private static final SparkleState STATE = new SparkleState(MAX_SWARMS);

    private final int count;
    private final int size;
    private final float speed;
    private final HellowColor tint;
    private final String stateKey;
    /** Fallback swarm for decorations without a key (resets when rebuilt). */
    private final ArrayDeque<SparkleParticle> anonymous = new ArrayDeque<>();
    private final Random random = new Random();

    /**
     * @param count    steady-state particles per element
     * @param size     particle edge in pixels (1 or 2; 2 gets a comet tail)
     * @param speed    velocity multiplier, {@code 1} = the reference pace
     * @param tint     particle color; its alpha is the peak opacity
     * @param stateKey stable id so the swarm survives a rebuild, or {@code null}
     */
    public Sparkles(int count, int size, float speed, HellowColor tint, String stateKey) {
        this.count = Math.max(0, count);
        this.size = Math.max(1, Math.min(4, size));
        this.speed = Math.max(0.05f, speed);
        this.tint = tint == null ? HellowColor.of(0xFFFFFFFF) : tint;
        this.stateKey = stateKey;
    }

    /** Convenience: 1px particles at the reference pace. */
    public static Sparkles of(int count, HellowColor tint, String stateKey) {
        return new Sparkles(count, 1, 1f, tint, stateKey);
    }

    @Override
    public String stateKey() {
        return this.stateKey;
    }

    /** Drops every persisted swarm (call when the owning screen goes away). */
    public static void clearAll() {
        STATE.clear();
    }

    static int swarmCount() {
        return STATE.size();
    }

    @Override
    public void after(RenderContext ctx, int x, int y, int width, int height) {
        if (this.count == 0 || width <= 2 || height <= 2) return;
        ArrayDeque<SparkleParticle> swarm = this.stateKey == null ? this.anonymous : STATE.field(this.stateKey);
        long now = System.currentTimeMillis();
        synchronized (swarm) {
            swarm.removeIf(spark -> spark.deadAt(now));
            while (swarm.size() < this.count) {
                swarm.add(spawn(width, height, now));
            }
            for (SparkleParticle spark : swarm) {
                draw(ctx, spark, x, y, width, height, now);
            }
        }
    }

    private SparkleParticle spawn(int width, int height, long now) {
        long life = SparkleMath.LIFE_MIN_MS + this.random.nextLong(SparkleMath.LIFE_MAX_MS - SparkleMath.LIFE_MIN_MS);
        float lifeSeconds = life / 1000f;
        float vy = Math.min(SparkleMath.maxSpeed(height, lifeSeconds),
                (4f + this.random.nextFloat() * 4f) * this.speed);
        float span = Math.max(1f, width - 6f);
        return new SparkleParticle(
                3f + this.random.nextFloat() * span,
                height - 4f,
                vy,
                2f + this.random.nextFloat() * 4f,
                0.2f + this.random.nextFloat() * 0.3f,
                this.random.nextFloat() * (float) (Math.PI * 2),
                now,
                life,
                this.random.nextInt(4) == 0 ? Math.min(2, this.size + 1) : this.size);
    }

    private void draw(RenderContext ctx, SparkleParticle spark, int x, int y, int width, int height, long now) {
        int argb = this.tint.argb();
        int alpha = (argb >>> 24) & 0xFF;
        int a = (int) (alpha * SparkleMath.fade(now - spark.born, spark.life));
        if (a <= 0) return;
        int rgb = argb & 0xFFFFFF;
        int px = SparkleMath.clamp((int) spark.xAt(now), 1, width - spark.size - 1);
        int py = SparkleMath.clamp((int) spark.yAt(now), 1, height - spark.size - 1);
        ctx.g().fill(x + px, y + py, x + px + spark.size, y + py + spark.size, a << 24 | rgb);
        if (spark.size <= 1) return;
        // Comet tail: re-sample the exact path at earlier times at a fixed
        // spatial spacing, so slow sparks do not get an invisible sub-pixel tail.
        float step = 2f / Math.max(spark.vy, 0.5f);
        for (int i = 1; i <= 3; i++) {
            float back = i * step;
            int tailAlpha = (int) (a * (1f - i * 0.18f));
            if (tailAlpha <= 0) continue;
            int tailSize = i == 1 ? spark.size : 1;
            int tx = SparkleMath.clamp((int) spark.xAt(now, back), 1, width - tailSize - 1);
            int ty = SparkleMath.clamp((int) spark.yAt(now, back), 1, height - tailSize - 1);
            ctx.g().fill(x + tx, y + ty, x + tx + tailSize, y + ty + tailSize, tailAlpha << 24 | rgb);
        }
    }
}
