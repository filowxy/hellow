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
        // A real cloth-config-based mod's config screen (REI's settings UI).
        register(new com.everyconfig.embed.ReiConfigAdapter());
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

    /**
     * First installed adapter among {@code preferredIds}, honoring the caller's
     * order; empty when none of the preferred libraries is installed. Unknown ids
     * are ignored.
     */
    public static Optional<ConfigAdapter> firstInstalledIn(List<String> preferredIds) {
        return firstWhere(orderedByKey(all(), preferredIds, ConfigAdapter::libraryId),
                ConfigAdapter::isInstalled);
    }

    /**
     * Pure selection helper (unit-testable without Minecraft): reorder {@code items}
     * so they follow {@code preferredKeys} order, dropping items whose key does not
     * appear in the preference list. Same shape as {@link #firstWhere}.
     */
    public static <T, K> List<T> orderedByKey(List<T> items, List<K> preferredKeys,
                                              java.util.function.Function<T, K> keyFn) {
        Map<K, T> byKey = new LinkedHashMap<>();
        for (T item : items) byKey.put(keyFn.apply(item), item);
        List<T> ordered = new ArrayList<>();
        for (K key : preferredKeys) {
            T item = byKey.get(key);
            if (item != null) ordered.add(item);
        }
        return ordered;
    }
}
