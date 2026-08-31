package com.everyconfig.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure-logic tests for {@link ConfigAdapterRegistry#firstWhere} (no Minecraft classes). */
class ConfigAdapterRegistryTest {

    @Test
    void picksFirstMatchInIterationOrder() {
        assertEquals(Optional.of("cloth_config"),
                ConfigAdapterRegistry.firstWhere(List.of("cloth_config", "yacl"), s -> true));
    }

    @Test
    void picksFirstItemSatisfyingPredicate() {
        assertEquals(Optional.of("yacl"),
                ConfigAdapterRegistry.firstWhere(
                        List.of("cloth_config", "yacl", "configured"), s -> s.startsWith("ya")));
    }

    @Test
    void emptyWhenNothingMatches() {
        assertTrue(ConfigAdapterRegistry.firstWhere(List.of("a", "b"), s -> false).isEmpty());
    }

    @Test
    void emptyWhenListEmpty() {
        assertTrue(ConfigAdapterRegistry.firstWhere(List.of(), s -> true).isEmpty());
    }

    // -------- orderedByKey (preference order for hand-written configEmbed pages) --------

    @Test
    void orderedByKeyFollowsPreferenceOverIteration() {
        List<String> items = List.of("cloth_config", "yacl");
        List<String> out = ConfigAdapterRegistry.orderedByKey(items, List.of("yacl", "cloth_config"), s -> s);
        assertEquals(List.of("yacl", "cloth_config"), out);
    }

    @Test
    void orderedByKeySkipsUnknownKeys() {
        List<String> items = List.of("cloth_config");
        List<String> out = ConfigAdapterRegistry.orderedByKey(items, List.of("unknown", "cloth_config"), s -> s);
        assertEquals(List.of("cloth_config"), out);
    }
}
