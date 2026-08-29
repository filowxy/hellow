package com.hellow.style;

/**
 * Immutable color theme for Hellow's rendering. Every color Hellow draws comes from
 * here, so callers can re-theme the page/modal chrome and widgets by passing a theme
 * instance into the theme-aware constructors of {@code PageScreen}, {@code HellowModal}
 * and the element widgets, or by using the theme overloads of {@code Elements}.
 *
 * <p>{@link #DEFAULT} reproduces the colors Hellow shipped with before theming existed,
 * so passing no theme is a zero-visual-change upgrade.
 *
 * <p>(ponytail: only colors are parameterized; geometry (paddings, heights, gaps) and
 * hover math stay hardcoded, so this record stays a flat list of ints instead of a
 * full "style" object.)
 *
 * @param dimOverlay            full-screen dim behind windows (ARGB)
 * @param panelBackground       page content panel fill
 * @param panelBorder           page content panel outline
 * @param titleColor            window title text
 * @param dialogBackground      modal dialog fill
 * @param dialogBorder          modal dialog outline
 * @param buttonBackground      button fill
 * @param buttonBackgroundHover button fill while hovered
 * @param buttonBorder          button outline
 * @param buttonBorderHover     button outline while hovered
 * @param buttonText            button label
 * @param boxBorder             default {@code Elements.box} border
 * @param textColor             default {@code Elements.text} color
 * @param linkColor             default link color
 * @param expandHeaderText      collapsible list header
 * @param expandHeaderTextHover collapsible list header while expanded/open
 * @param scrollbarTrack        scrollbar track fill
 * @param scrollbarThumb        scrollbar thumb fill
 * @param formLabel             form widget label text
 * @param formFieldBackground   text-input field fill
 * @param formOutline           text-input outline
 * @param formOutlineFocused    text-input outline while focused
 * @param formValue             form value text (text input, enum value)
 * @param toggleKnobOn          toggle knob while on
 * @param sliderTrack           slider track fill
 * @param sliderKnob            slider knob fill
 */
public record HellowTheme(
        int dimOverlay,
        int panelBackground,
        int panelBorder,
        int titleColor,
        int dialogBackground,
        int dialogBorder,
        int buttonBackground,
        int buttonBackgroundHover,
        int buttonBorder,
        int buttonBorderHover,
        int buttonText,
        int boxBorder,
        int textColor,
        int linkColor,
        int expandHeaderText,
        int expandHeaderTextHover,
        int scrollbarTrack,
        int scrollbarThumb,
        int formLabel,
        int formFieldBackground,
        int formOutline,
        int formOutlineFocused,
        int formValue,
        int toggleKnobOn,
        int sliderTrack,
        int sliderKnob) {

    /** Default theme: the exact colors Hellow used before theming (zero visual change). */
    public static final HellowTheme DEFAULT = new HellowTheme(
            0xCC000000, // dim overlay
            0xFF1E1E1E, // panel background
            0xFFFFFFFF, // panel border
            0xFFFFFF55, // title
            0xFF2A2A2A, // dialog background
            0xFFFFFFFF, // dialog border
            0xFF444444, // button background
            0xFF555555, // button background hover
            0xFFFFFFFF, // button border
            0xFFFFFF55, // button border hover
            0xFFFFFFFF, // button text
            0xFF888888, // box border
            0xFFFFFFFF, // text
            0xFF55AAFF, // link
            0xFFFFFFFF, // expandable header
            0xFFFFFF55, // expandable header expanded
            0xFF333333, // scrollbar track
            0xFFAAAAAA, // scrollbar thumb
            0xFFFFFFFF, // form label
            0xFF222222, // form field background
            0xFFFFFFFF, // form outline
            0xFFFFFF55, // form outline focused
            0xFFFFAA55, // form value
            0xFF55FF55, // toggle knob on
            0xFF444444, // slider track
            0xFFFFFFFF  // slider knob
    );
}