package com.hellow.element;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public record RenderContext(GuiGraphics g, Font font, int x, int y, int width, int mouseX, int mouseY) {}
