package com.hellow.decor;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bounded store of particle swarms keyed by a stable string.
 *
 * <p>Why a registry instead of a field on the decoration: virtualized lists
 * rebuild their row elements every frame, so instance state is discarded sixty
 * times a second. A stable key (a card id, say) keeps the swarm alive across
 * those rebuilds.
 *
 * <p>Bounded on purpose: decorations can be created per frame by mistake, and an
 * unbounded map would leak. The least-recently-used swarm is evicted past
 * {@code maxEntries}, which at worst restarts one off-screen effect.
 */
final class SparkleState {
    private final int maxEntries;
    private final Map<String, ArrayDeque<SparkleParticle>> fields;

    SparkleState(int maxEntries) {
        this.maxEntries = Math.max(1, maxEntries);
        this.fields = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ArrayDeque<SparkleParticle>> eldest) {
                return size() > SparkleState.this.maxEntries;
            }
        };
    }

    synchronized ArrayDeque<SparkleParticle> field(String key) {
        return this.fields.computeIfAbsent(key, k -> new ArrayDeque<>());
    }

    synchronized int size() {
        return this.fields.size();
    }

    synchronized void clear() {
        this.fields.clear();
    }
}
