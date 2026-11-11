package com.hellow.i18n;

import java.util.Set;

public interface TranslationSource {

    String modId();

    String get(String key, String language);

    Set<String> supportedLanguages();
}
