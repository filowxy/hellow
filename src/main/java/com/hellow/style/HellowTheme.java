package com.hellow.style;

public record HellowTheme(
        HellowColor dimOverlay,
        HellowColor panelBackground,
        HellowColor panelBorder,
        HellowColor titleColor,
        HellowColor dialogBackground,
        HellowColor dialogBorder,
        HellowColor buttonBackground,
        HellowColor buttonBackgroundHover,
        HellowColor buttonBorder,
        HellowColor buttonBorderHover,
        HellowColor buttonText,
        HellowColor boxBorder,
        HellowColor textColor,
        HellowColor linkColor,
        HellowColor expandHeaderText,
        HellowColor expandHeaderTextHover,
        HellowColor scrollbarTrack,
        HellowColor scrollbarThumb,
        HellowColor formLabel,
        HellowColor formFieldBackground,
        HellowColor formOutline,
        HellowColor formOutlineFocused,
        HellowColor formValue,
        HellowColor toggleKnobOn,
        HellowColor sliderTrack,
        HellowColor sliderKnob) {

    public static final HellowTheme DEFAULT = new HellowTheme(
            HellowColor.of(0x66000000),
            HellowColor.of(0xFF1E1E1E),
            HellowColor.of(0xFFFFFFFF),
            HellowColor.of(0xFFFFFF55),
            HellowColor.of(0xFF2A2A2A),
            HellowColor.of(0xFFFFFFFF),
            HellowColor.of(0xFF444444),
            HellowColor.of(0xFF555555),
            HellowColor.of(0xFFFFFFFF),
            HellowColor.of(0xFFFFFF55),
            HellowColor.of(0xFFFFFFFF),
            HellowColor.of(0xFF888888),
            HellowColor.of(0xFFFFFFFF),
            HellowColor.of(0xFF55AAFF),
            HellowColor.of(0xFFFFFFFF),
            HellowColor.of(0xFFFFFF55),
            HellowColor.of(0xFF333333),
            HellowColor.of(0xFFAAAAAA),
            HellowColor.of(0xFFFFFFFF),
            HellowColor.of(0xFF222222),
            HellowColor.of(0xFFFFFFFF),
            HellowColor.of(0xFFFFFF55),
            HellowColor.of(0xFFFFAA55),
            HellowColor.of(0xFF55FF55),
            HellowColor.of(0xFF444444),
            HellowColor.of(0xFFFFFFFF)
    );

    public static HellowTheme flat(
            int dimOverlay, int panelBackground, int panelBorder, int titleColor,
            int dialogBackground, int dialogBorder, int buttonBackground, int buttonBackgroundHover,
            int buttonBorder, int buttonBorderHover, int buttonText, int boxBorder,
            int textColor, int linkColor, int expandHeaderText, int expandHeaderTextHover,
            int scrollbarTrack, int scrollbarThumb, int formLabel, int formFieldBackground,
            int formOutline, int formOutlineFocused, int formValue, int toggleKnobOn,
            int sliderTrack, int sliderKnob) {
        return new HellowTheme(
                HellowColor.of(dimOverlay), HellowColor.of(panelBackground), HellowColor.of(panelBorder),
                HellowColor.of(titleColor), HellowColor.of(dialogBackground), HellowColor.of(dialogBorder),
                HellowColor.of(buttonBackground), HellowColor.of(buttonBackgroundHover),
                HellowColor.of(buttonBorder), HellowColor.of(buttonBorderHover), HellowColor.of(buttonText),
                HellowColor.of(boxBorder), HellowColor.of(textColor), HellowColor.of(linkColor),
                HellowColor.of(expandHeaderText), HellowColor.of(expandHeaderTextHover),
                HellowColor.of(scrollbarTrack), HellowColor.of(scrollbarThumb), HellowColor.of(formLabel),
                HellowColor.of(formFieldBackground), HellowColor.of(formOutline),
                HellowColor.of(formOutlineFocused), HellowColor.of(formValue), HellowColor.of(toggleKnobOn),
                HellowColor.of(sliderTrack), HellowColor.of(sliderKnob));
    }

    public HellowTheme withTitle(HellowColor titleColor) {
        return new HellowTheme(dimOverlay, panelBackground, panelBorder, titleColor,
                dialogBackground, dialogBorder, buttonBackground, buttonBackgroundHover,
                buttonBorder, buttonBorderHover, buttonText, boxBorder, textColor, linkColor,
                expandHeaderText, expandHeaderTextHover, scrollbarTrack, scrollbarThumb,
                formLabel, formFieldBackground, formOutline, formOutlineFocused, formValue,
                toggleKnobOn, sliderTrack, sliderKnob);
    }
}
