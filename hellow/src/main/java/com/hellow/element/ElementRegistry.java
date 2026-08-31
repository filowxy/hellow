package com.hellow.element;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Tag-based element factory registry — the extension point for custom element types.
 *
 * <p>Mods that want their own element to appear inside hand-written pages
 * ({@code config/hellow/pages/*.json}) register a factory under a tag here;
 * {@link com.hellow.pages.PageFileParser} falls back to this registry for any
 * element type it does not know natively (the page JSON's {@code type} is the tag).
 * A tag is either a plain lower-case name ({@code configEmbed}) or namespaced
 * ({@code ns:name}); the parser passes the JSON object of the element as the
 * factory's {@code args}.
 */
public final class ElementRegistry {
    private static final Pattern TAG = Pattern.compile("[a-z0-9_]+(:[a-z0-9_]+)?");
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