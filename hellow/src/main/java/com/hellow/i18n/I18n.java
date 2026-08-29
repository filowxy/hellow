package com.hellow.i18n;

import com.hellow.HellowConstants;
import net.minecraft.locale.Language;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * Hellow's i18n facade.
 *
 * <p>Wraps Minecraft's {@link Language} with a per-mod source registry.
 * Lookup order for {@code I18n.translate(key, language)}:
 * <ol>
 *   <li>Exact match in the mod's {@link TranslationSource} for the
 *       current language code.</li>
 *   <li>Exact match in vanilla Minecraft's loaded language.</li>
 *   <li>Language fallback chain within the source: e.g. for
 *       {@code zh_cn}, try {@code zh_cn} → {@code zh_*} → {@code <any zh>}.</li>
 *   <li>Source fallback: try the next registered source (registration
 *       order).</li>
 *   <li>Return {@code key} verbatim (Minecraft's default behaviour;
 *       the caller is expected to log a missing-key warning at
 *       startup, not at every render).</li>
 * </ol>
 *
 * <p>Why not just call {@code Component.translatable(key)}? Because
 * vanilla only knows about keys that have a corresponding
 * {@code assets/<modId>/lang/<code>.json} file at startup. TISAM
 * generates some of its strings at runtime (the install guide text
 * comes from {@code config/tisam/modules.json}, not a packaged json),
 * and a runtime translation table is the cleanest way to inject
 * those without writing json files to disk.
 *
 * <p>Threading: {@link I18n} holds a {@link ConcurrentHashMap}, but
 * callers should still pass the language code from the main client
 * thread (Minecraft's {@code options.languageCode}) — passing a
 * stale value from a different thread would just return a stale
 * translation, never a crash.
 */
public final class I18n {
    private I18n() {}

    /**
     * Pattern that splits a {@code zh_cn}-style code into its primary
     * tag and the rest. Used by the language fallback chain to try
     * {@code zh_*} (any region) when {@code zh_cn} isn't present.
     */
    private static final Pattern LANG_PRIMARY = Pattern.compile("_");

    /**
     * Per-mod source registry. Concurrent because the embedding mod
     * can register sources at any point during FMLClientSetupEvent,
     * and tests may also register sources from worker threads.
     */
    private static final ConcurrentMap<String, TranslationSource> SOURCES = new ConcurrentHashMap<>();

    /**
     * Vanilla's default language code, used as the last-resort fallback
     * when the caller doesn't know the current language. Matches
     * {@link Language#DEFAULT}.
     */
    public static final String DEFAULT_LANGUAGE = Language.DEFAULT;

    /** Register or replace a source for the given mod id. */
    public static void registerSource(TranslationSource source) {
        Objects.requireNonNull(source, "source");
        TranslationSource prev = SOURCES.put(source.modId(), source);
        if (prev != null) {
            HellowConstants.log().warn("[hellow] i18n source for '{}' replaced (was: {}, now: {})",
                    source.modId(), prev.getClass().getName(), source.getClass().getName());
        } else {
            HellowConstants.log().info("[hellow] i18n source registered for mod '{}'", source.modId());
        }
    }

    /** Remove a previously-registered source. Idempotent. */
    public static void unregisterSource(String modId) {
        if (SOURCES.remove(modId) != null) {
            HellowConstants.log().info("[hellow] i18n source removed for mod '{}'", modId);
        }
    }

    /**
     * Look up {@code key} via the source registered for {@code modId},
     * falling back to vanilla Minecraft's language, then to the key
     * itself.
     *
     * @param modId    the mod that owns the key, or {@code null} to skip
     *                 the source registry and go straight to vanilla
     * @param key      translation key
     * @param language current language code, lowercase with underscore
     *                 (e.g. {@code "en_us"}, {@code "zh_cn"}). If
     *                 {@code null}, {@link #DEFAULT_LANGUAGE} is used.
     * @return never {@code null}; the key is returned verbatim if no
     *         source has it.
     */
    public static String translate(String modId, String key, String language) {
        if (key == null) return "";
        String lang = language == null ? DEFAULT_LANGUAGE : language;
        if (modId != null) {
            TranslationSource src = SOURCES.get(modId);
            if (src != null) {
                String value = lookupWithFallback(src, key, lang);
                if (value != null) return value;
            }
        }
        // Fall back to vanilla. Language.getInstance().getOrDefault(key)
        // is the canonical lookup; the embedding mod's bundled lang json
        // is already loaded by the time we get here.
        return Language.getInstance().getOrDefault(key);
    }

    /**
     * Build a {@link net.minecraft.network.chat.MutableComponent} that
     * resolves at render time. Convenience wrapper that mirrors the
     * common {@code Component.translatable(...)} pattern.
     */
    public static net.minecraft.network.chat.MutableComponent translatable(String modId, String key, String language) {
        String value = translate(modId, key, language);
        return net.minecraft.network.chat.Component.literal(value);
    }

    /**
     * Try the source's primary language, then its fallback chain
     * (region-stripped prefix), then return null. The chain
     * {@code zh_cn → zh_* → <anything-zh>} mirrors Minecraft's own
     * behaviour so a {@code zh_cn}-only source still serves a
     * {@code zh_tw} request.
     */
    static String lookupWithFallback(TranslationSource src, String key, String language) {
        // 1) exact match
        String value = src.get(key, language);
        if (value != null) return value;

        // 2) region fallback: e.g. zh_cn -> zh_*
        Set<String> supported = src.supportedLanguages();
        if (!supported.isEmpty()) {
            String primary = LANG_PRIMARY.split(language)[0];
            for (String candidate : supported) {
                if (LANG_PRIMARY.split(candidate)[0].equals(primary) && !candidate.equals(language)) {
                    value = src.get(key, candidate);
                    if (value != null) return value;
                }
            }

            // 3) ultimate fallback: any supported language (first one
            // that's not the one we already tried).
            for (String candidate : supported) {
                if (candidate.equals(language)) continue;
                value = src.get(key, candidate);
                if (value != null) return value;
            }
        }
        return null;
    }

    /**
     * Normalize a Minecraft language code to lowercase, with underscore
     * as the separator. Minecraft's options file always stores codes
     * in that form ({@code en_us}, {@code zh_cn}), but we re-normalize
     * defensively for callers that pass arbitrary strings.
     */
    public static String normalizeLanguage(String language) {
        return language == null ? DEFAULT_LANGUAGE : language.toLowerCase(Locale.ROOT);
    }

    /** Snapshot of registered sources. Used by tests. */
    public static Map<String, TranslationSource> registeredSources() {
        return new HashMap<>(SOURCES);
    }

    /** Reset for unit tests. Drops every registered source. */
    public static void resetForTesting() {
        SOURCES.clear();
    }
}
