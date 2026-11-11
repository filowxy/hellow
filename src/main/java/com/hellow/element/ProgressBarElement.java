package com.hellow.element;

import com.hellow.style.HellowTheme;
import net.minecraft.client.gui.Font;

public final class ProgressBarElement implements Element {
    private static final int TRACK_WIDTH = 80;
    private static final int TRACK_HEIGHT = 8;
    private static final int HEIGHT = 20;

    private final String label;
    private int value;
    private final int max;
    private final HellowTheme theme;

    public ProgressBarElement(String label, int value, int max) {
        this(label, value, max, HellowTheme.DEFAULT);
    }

    public ProgressBarElement(String label, int value, int max, HellowTheme theme) {
        this.label = label == null ? "" : label;
        this.max = Math.max(1, max);
        this.value = Math.max(0, Math.min(this.max, value));
        this.theme = theme == null ? HellowTheme.DEFAULT : theme;
    }

    public int value() { return this.value; }

    public void setValue(int value) {
        this.value = Math.max(0, Math.min(this.max, value));
    }

    public int width(Font font) { return font.width(this.label) + TRACK_WIDTH + 24; }
    public int height(Font font, int width) { return HEIGHT; }

    public void render(RenderContext ctx) {
        ctx.g().drawString(ctx.font(), this.label, ctx.x(), ctx.y() + 6, this.theme.formLabel().argb(), true);
        int trackX = ctx.x() + ctx.width() - TRACK_WIDTH - 4;
        int trackY = ctx.y() + (HEIGHT - TRACK_HEIGHT) / 2;
        ctx.g().fill(trackX, trackY, trackX + TRACK_WIDTH, trackY + TRACK_HEIGHT, this.theme.sliderTrack().argb());
        int fillW = (int) ((float) this.value / this.max * TRACK_WIDTH);
        if (fillW > 0) {
            ctx.g().fill(trackX, trackY, trackX + fillW, trackY + TRACK_HEIGHT, this.theme.sliderKnob().argb());
        }
    }
}
