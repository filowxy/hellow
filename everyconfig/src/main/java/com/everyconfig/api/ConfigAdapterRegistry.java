package com.everyconfig.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Registry of config-library adapters, in registration order (the order
 * {@link #embedFirst} / {@link #firstInstalled} use to pick a library).
 */
public final class ConfigAdapterRegistry {
    private static final Map<String, ConfigAdapter> ADAPTERS = new LinkedHashMap<>();
    private static boolean registered;

    private ConfigAdapterRegistry() {}

    /**
     * Populate the built-in adapters once. Kept lazy (not a static initializer) so
     * loading this class in a Minecraft-free test JVM never touches adapter classes
     * (which reference Minecraft types).
     */
    private static synchronized void ensureRegistered() {
        if (registered) return;
        registered = true;
        // Reference adapters; further libraries are added the same way.
        register(new com.everyconfig.embed.ClothConfigAdapter());
        register(new com.everyconfig.embed.YaclAdapter());
    }

    public static synchronized void register(ConfigAdapter adapter) {
        if (adapter == null || adapter.libraryId() == null || adapter.libraryId().isEmpty()) return;
        ADAPTERS.put(adapter.libraryId(), adapter);
    }

    public static Optional<ConfigAdapter> get(String libraryId) {
        ensureRegistered();
        return Optional.ofNullable(ADAPTERS.get(libraryId));
    }

    /** Every registered adapter, in registration order. */
    public static List<ConfigAdapter> all() {
        ensureRegistered();
        return List.copyOf(ADAPTERS.values());
    }

    /**
     * Pure selection helper (unit-testable without Minecraft): returns the first
     * item in {@code items} (iteration order) accepted by {@code test}.
     */
    public static <T> Optional<T> firstWhere(List<T> items, Predicate<T> test) {
        for (T item : items) {
            if (test.test(item)) return Optional.of(item);
        }
        return Optional.empty();
    }

    /** Adapters whose library mod is present at runtime. */
    public static List<ConfigAdapter> installed() {
        ensureRegistered();
        List<ConfigAdapter> out = new ArrayList<>();
        for (ConfigAdapter a : ADAPTERS.values()) if (a.isInstalled()) out.add(a);
        return Collections.unmodifiableList(out);
    }

    /** First installed adapter (registration order), if any. */
    public static Optional<ConfigAdapter> firstInstalled() {
        return firstWhere(all(), ConfigAdapter::isInstalled);
    }
}
