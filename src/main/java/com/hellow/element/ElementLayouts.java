package com.hellow.element;

import com.hellow.layout.ElementLayout;
import net.minecraft.client.gui.Font;

/**
 * Resolves a child's {@link ElementLayout} without the caller knowing which
 * wrapper type produced it.
 *
 * <p>Resolution order: {@link StyledElement} (the full-featured form), then the
 * legacy {@link FlexElement} and {@link FillElement} wrappers, which map onto the
 * equivalent layout. Keeping the legacy branches is what lets existing callers
 * — TISAM included — compile and behave unchanged.
 */
public final class ElementLayouts {
    private ElementLayouts() {}

    public static ElementLayout of(Element child) {
        if (child instanceof StyledElement styled) return styled.layout();
        if (child instanceof FlexElement flex) return ElementLayout.weighted(flex.weight());
        if (child instanceof FillElement) return ElementLayout.FILL;
        return ElementLayout.NATURAL;
    }

    /** The child's natural width, clamped to its own bounds. */
    public static int naturalWidth(Element child, Font font) {
        return of(child).clampWidth(child.width(font));
    }

    /** The child's height at the width it will actually be drawn at. */
    public static int naturalHeight(Element child, Font font, int width) {
        return child.height(font, of(child).clampWidth(width));
    }
}
