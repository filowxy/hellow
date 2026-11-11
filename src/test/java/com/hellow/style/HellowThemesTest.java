package com.hellow.style;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class HellowThemesTest {

    private static HellowTheme themeWithTitle(int argb) {
        return HellowTheme.DEFAULT.withTitle(HellowColor.of(argb));
    }

    @Test
    void registerAndResolveNamedTheme() {
        HellowThemes.clear();
        HellowTheme custom = themeWithTitle(0xFF123456);
        HellowThemes.register("test_theme", custom);
        assertSame(custom, HellowThemes.get("test_theme"));
        assertSame(custom, HellowThemes.resolve("test_theme"));
    }

    @Test
    void unknownThemeResolvesToNull() {
        HellowThemes.clear();
        assertNull(HellowThemes.resolve("nope"));
        assertNull(HellowThemes.get(null));
    }

    @Test
    void setDefaultOverridesFallbackAndNullResets() {
        HellowThemes.clear();
        assertSame(HellowTheme.DEFAULT, HellowThemes.defaultTheme());
        HellowTheme custom = themeWithTitle(0xFFABCDEF);
        HellowThemes.setDefault(custom);
        assertSame(custom, HellowThemes.defaultTheme());
        HellowThemes.setDefault(null);
        assertSame(HellowTheme.DEFAULT, HellowThemes.defaultTheme());
    }

    @Test
    void registeredThemeKeepsItsColors() {
        HellowThemes.clear();
        HellowTheme custom = themeWithTitle(0xFF123456);
        HellowThemes.register("colors", custom);
        assertEquals(0xFF123456, HellowThemes.resolve("colors").titleColor().argb());
        HellowThemes.clear();
    }
}
