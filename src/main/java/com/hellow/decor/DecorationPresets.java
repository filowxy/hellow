package com.hellow.decor;

import com.hellow.style.HellowTheme;
import com.hellow.style.HellowThemes;

import java.util.List;

/**
 * Theme-driven decoration presets.
 *
 * <p>Deliberately a factory of lists rather than new fields on {@link HellowTheme}:
 * the theme is a 26-component record that callers construct positionally, and
 * adding a component would break every existing {@code new HellowTheme(...)}.
 * Presets derive their colors from the slots a theme already has, so a custom
 * theme restyles the decorations for free.
 *
 * <p>Usage: {@code Elements.decorate(child, DecorationPresets.card(theme))}.
 */
public final class DecorationPresets {
    private DecorationPresets() {}

    private static HellowTheme theme(HellowTheme theme) {
        return theme == null ? HellowThemes.defaultTheme() : theme;
    }

    /** Hairline outline only. */
    public static List<Decoration> outline(HellowTheme theme) {
        HellowTheme t = theme(theme);
        return List.of(new SolidBorder(1, t.boxBorder()));
    }

    /** Panel fill plus a border — the standard container chrome. */
    public static List<Decoration> panel(HellowTheme theme) {
        HellowTheme t = theme(theme);
        return List.of(
                new Backdrop(t.panelBackground()),
                new SolidBorder(1, t.panelBorder()));
    }

    /** Gradient-ring card with a panel fill, in the style of TISAM's cards. */
    public static List<Decoration> card(HellowTheme theme) {
        HellowTheme t = theme(theme);
        return List.of(
                new GradientRing(1, t.buttonBorder(), t.buttonBorderHover(), t.panelBackground()));
    }

    /** Soft halo in the link color. */
    public static List<Decoration> glow(HellowTheme theme) {
        HellowTheme t = theme(theme);
        return List.of(new Glow(3, t.linkColor()));
    }

    /** Compact chip: button fill plus a border. */
    public static List<Decoration> chip(HellowTheme theme) {
        HellowTheme t = theme(theme);
        return List.of(
                new Backdrop(t.buttonBackground()),
                new SolidBorder(1, t.buttonBorder()));
    }

    /** Attention frame: title-colored halo plus a matching border. */
    public static List<Decoration> focus(HellowTheme theme) {
        HellowTheme t = theme(theme);
        return List.of(
                new Glow(2, t.titleColor()),
                new SolidBorder(1, t.titleColor()));
    }

    /** Drop shadow behind the element. */
    public static List<Decoration> shadow(HellowTheme theme) {
        HellowTheme t = theme(theme);
        return List.of(new Shadow(2, 2, 3, t.dimOverlay()));
    }

    /** Card chrome plus corner accents — for hero panels and empty-state boxes. */
    public static List<Decoration> framed(HellowTheme theme) {
        HellowTheme t = theme(theme);
        return List.of(
                new Backdrop(t.panelBackground()),
                new GradientRing(1, t.buttonBorder(), t.buttonBorderHover(), t.panelBackground()),
                new CornerAccents(8, 1, t.linkColor()));
    }
}
