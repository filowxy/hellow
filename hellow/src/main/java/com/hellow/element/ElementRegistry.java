package com.hellow.element;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Experimental tag-based element factory registry.
 *
 * <p>(ponytail: half-finished — {@link #register(String, ElementFactory)} is never
 * called and no default factories are registered; layout elements are constructed
 * directly via {@code Elements} / their classes instead. Kept only as a possible future
 * basis for a DSL/JSON page format. Do not build on it.)
 */
public final class ElementRegistry {
    private static final Pattern TAG = Pattern.compile("[a-z0-9_]+:[a-z0-9_]+");
    private static final Map<String, ElementFactory> FACTORIES = new ConcurrentHashMap<>();

    private ElementRegistry() {}

    public static void register(String tag, ElementFactory factory) {
        if (tag == null || !TAG.matcher(tag).matches()) {
            throw new IllegalArgumentException("bad element tag: " + tag);
        }
        if (factory == null) throw new IllegalArgumentException("factory required");
        if (FACTORIES.putIfAbsent(tag, factory) != null) {
            throw new DuplicateElementTagException(tag);
        }
    }

    public static Element create(String tag, Object args) {
        ElementFactory factory = FACTORIES.get(tag);
        if (factory == null) throw new IllegalArgumentException("unknown element tag: " + tag);
        return factory.create(new ElementSpec(tag, args));
    }

    public static Element create(ElementSpec spec) { return create(spec.tag(), spec.args()); }
    public static boolean contains(String tag) { return FACTORIES.containsKey(tag); }
    public static void clear() { FACTORIES.clear(); }
}