package com.hellow.element;

import net.minecraft.network.chat.Component;

public interface Interactive {
    default boolean mouseClicked(RenderContext ctx, int button) { return false; }
    default boolean mouseScrolled(RenderContext ctx, double deltaX, double deltaY) { return false; }
    default boolean mouseDragged(RenderContext ctx, int button, double dragX, double dragY) { return false; }
    default boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }
    default boolean charTyped(char codePoint, int modifiers) { return false; }

    /**
     * Tooltip to display when the pointer is over this element, or {@code null} when
     * there is nothing to show. The pointer position comes from {@code ctx}.
     */
    default Component tooltipAt(RenderContext ctx) { return null; }
}
