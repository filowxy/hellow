package com.hellow.i18n;

import java.util.Set;

/**
 * One mod's bundle of translations.
 *
 * <p>Hellow supports two registration modes per the design doc:
 * <ul>
 *   <li><b>API map mode (mode A):</b> the mod calls
 *       {@link I18n#registerSource(String, TranslationSource)} with a
 *       source that returns translations from a {@code Map<String, String>}
 *       it owns. Use this for mods that generate strings at runtime
 *       (e.g. TISAM's per-modpack install guide, which is read from
 *       {@code config/tisam/modules.json}).</li>
 *   <li><b>Packaged file mode (mode B):</b> the mod packs its
 *       translations under {@code assets/<modId>/lang/<code>.json}
 *       and Hellow loads them via the classpath. This is delegated to
 *       vanilla Minecraft's translation loader — Hellow just supplies
 *       the lookup table. v1.0 ships a thin wrapper; mods that want
 *       fine control can register their own {@link TranslationSource}.</li>
 * </ul>
 *
 * <p>Implementations must be safe to call from any thread that the
 * game schedules on — {@link I18n} does no internal locking.
 */
public interface TranslationSource {

    /**
     * The mod id this source provides translations for. Used as the
     * key in {@link I18n}'s registry.
     */
    String modId();

    /**
     * Look up a translation key.
     *
     * @param key      fully-qualified key, e.g. {@code "tisam.title.button"}
     * @param language current language code, lowercase with underscore
     *                 (e.g. {@code "en_us"}, {@code "zh_cn"})
     * @return the translated string, or {@code null} if this source
     *         has no entry for the key in any supported language
     *         (i.e. {@link I18n} should try the next source / fallback).
     */
    String get(String key, String language);

    /**
     * Languages this source has explicit entries for. Hellow uses
     * this to do exact-match lookups; for languages not in this set
     * the fallback chain (see {@link I18n}) takes over.
     */
    Set<String> supportedLanguages();
}
