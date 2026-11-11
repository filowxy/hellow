package com.hellow.element;

import net.minecraft.client.gui.Font;

public final class SpacerElement implements Element {
    private final int height;

    public SpacerElement(int height) {
        this.height = height;
    }

    public int width(Font font) {
        return 0;
    }

    public int height(Font font, int width) {
        return this.height;
    }

    public void render(RenderContext ctx) {}
}
