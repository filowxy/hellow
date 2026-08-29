package com.hellow.element;

import net.minecraft.client.gui.Font;

public interface Element extends Interactive {
    int width(Font font);
    int height(Font font, int width);
    void render(RenderContext ctx);
}
