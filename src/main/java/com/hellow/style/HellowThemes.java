package com.hellow.style;

import com.hellow.HellowConstants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HellowThemes {
    private static final Map<String, HellowTheme> NAMED = new ConcurrentHashMap<>();
    private static volatile HellowTheme DEFAULT = HellowTheme.DEFAULT;

    private HellowThemes() {}

    public static void register(String name, HellowTheme theme) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("theme name required");
        if (theme == null) throw new IllegalArgumentException("theme required");
        NAMED.put(name, theme);
        HellowConstants.log().info("[hellow] theme '{}' registered", name);
    }

    public static void unregister(String name) {
        if (name != null) NAMED.remove(name);
    }

    public static HellowTheme get(String name) {
        return name == null ? null : NAMED.get(name);
    }

    public static HellowTheme resolve(String name) {
        if (name == null) return null;
        HellowTheme theme = NAMED.get(name);
        if (theme == null) {
            HellowConstants.log().warn("[hellow] unknown theme '{}', falling back to the default theme", name);
            return null;
        }
        return theme;
    }

    public static void setDefault(HellowTheme theme) {
        DEFAULT = theme == null ? HellowTheme.DEFAULT : theme;
    }

    public static HellowTheme defaultTheme() {
        return DEFAULT;
    }

    public static void clear() {
        NAMED.clear();
        DEFAULT = HellowTheme.DEFAULT;
    }
}
