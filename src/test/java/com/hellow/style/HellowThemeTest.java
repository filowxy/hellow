package com.hellow.style;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HellowThemeTest {

    @Test
    void defaultThemeMatchesLegacyColors() {
        HellowTheme t = HellowTheme.DEFAULT;
        assertEquals(0x66000000, t.dimOverlay().argb());
        assertEquals(0xFF1E1E1E, t.panelBackground().argb());
        assertEquals(0xFF2A2A2A, t.dialogBackground().argb());
        assertEquals(0xFFFFFF55, t.titleColor().argb());
        assertEquals(0xFF444444, t.buttonBackground().argb());
        assertEquals(0xFF555555, t.buttonBackgroundHover().argb());
        assertEquals(0xFF55AAFF, t.linkColor().argb());
        assertEquals(0xFF55FF55, t.toggleKnobOn().argb());
        assertEquals(0xFFFFAA55, t.formValue().argb());
    }

    @Test
    void flatFactoryWrapsIntsFaithfully() {
        HellowTheme custom = HellowTheme.flat(
                0xAA112233, 0xFF101010, 0xFF202020, 0xFF303030, 0xFF404040, 0xFF505050,
                0xFF606060, 0xFF707070, 0xFF808080, 0xFF909090, 0xFFA0A0A0, 0xFFB0B0B0,
                0xFFC0C0C0, 0xFFD0D0D0, 0xFFE0E0E0, 0xFFF1F1F1, 0xFFF2F2F2, 0xFFF3F3F3,
                0xFFF4F4F4, 0xFFF5F5F5, 0xFFF6F6F6, 0xFFF7F7F7, 0xFFF8F8F8, 0xFFF9F9F9,
                0xFFFAFAFA, 0xFFFBFBFB);
        assertEquals(0xAA112233, custom.dimOverlay().argb());
        assertEquals(0xFFFBFBFB, custom.sliderKnob().argb());
        assertEquals(0x66000000, HellowTheme.DEFAULT.dimOverlay().argb());
    }
}
