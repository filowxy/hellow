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

public final class I18n {
    private I18n() {}

    private static final Pattern LANG_PRIMARY = Pattern.compile("_");

    private static final ConcurrentMap<String, TranslationSource> SOURCES = new ConcurrentHashMap<>();

    public static final String DEFAULT_LANGUAGE = Language.DEFAULT;

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

    public static void unregisterSource(String modId) {
        if (SOURCES.remove(modId) != null) {
            HellowConstants.log().info("[hellow] i18n source removed for mod '{}'", modId);
        }
    }

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
        return Language.getInstance().getOrDefault(key);
    }

    public static net.minecraft.network.chat.MutableComponent translatable(String modId, String key, String language) {
        String value = translate(modId, key, language);
        return net.minecraft.network.chat.Component.literal(value);
    }

    static String lookupWithFallback(TranslationSource src, String key, String language) {
        String value = src.get(key, language);
        if (value != null) return value;

        Set<String> supported = src.supportedLanguages();
        if (!supported.isEmpty()) {
            String primary = LANG_PRIMARY.split(language)[0];
            for (String candidate : supported) {
                if (LANG_PRIMARY.split(candidate)[0].equals(primary) && !candidate.equals(language)) {
                    value = src.get(key, candidate);
                    if (value != null) return value;
                }
            }

            for (String candidate : supported) {
                if (candidate.equals(language)) continue;
                value = src.get(key, candidate);
                if (value != null) return value;
            }
        }
        return null;
    }

    public static String normalizeLanguage(String language) {
        return language == null ? DEFAULT_LANGUAGE : language.toLowerCase(Locale.ROOT);
    }

    public static Map<String, TranslationSource> registeredSources() {
        return new HashMap<>(SOURCES);
    }

    public static void resetForTesting() {
        SOURCES.clear();
    }
}
