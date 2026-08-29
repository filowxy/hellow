package com.hellow.element;

import com.hellow.event.ElementEvents;
import com.hellow.event.ElementId;
import com.hellow.style.HellowTheme;
import net.minecraft.client.gui.Font;
import java.util.function.Consumer;

public final class SliderElement implements Element {
    private final String label;
    private final int min;
    private final int max;
    private int value;
    private final Consumer<Integer> onChange;
    private final ElementId elementId;
    private final HellowTheme theme;
    /**
     * True while a drag that STARTED on this slider is in progress. Hellow has no
     * mouseReleased callback, so this is (re)set on every mouseClicked: pressing
     * anywhere re-evaluates it, which kills the "press elsewhere, drag across the
     * slider and it jumps" misfire — containers fan mouseDragged to every visible
     * child, and without this flag a slider dragged over reactively.
     */
    private boolean dragging;

    public SliderElement(String label, int min, int max, int value, Consumer<Integer> onChange) {
        this(label, min, max, value, onChange, null, HellowTheme.DEFAULT);
    }

    public SliderElement(String label, int min, int max, int value, Consumer<Integer> onChange, ElementId elementId) {
        this(label, min, max, value, onChange, elementId, HellowTheme.DEFAULT);
    }

    public SliderElement(String label, int min, int max, int value, Consumer<Integer> onChange,
                         ElementId elementId, HellowTheme theme) {
        this.label = label;
        this.min = min;
        this.max = Math.max(min, max);
        this.value = Math.max(min, Math.min(this.max, value));
        this.onChange = onChange;
        this.elementId = elementId;
        this.theme = theme == null ? HellowTheme.DEFAULT : theme;
    }

    public int width(Font font) { return font.width(this.label) + 120; }
    public int height(Font font, int width) { return 20; }

    public void render(RenderContext ctx) {
        ctx.g().drawString(ctx.font(), this.label, ctx.x(), ctx.y() + 6, this.theme.formLabel(), true);
        int sx = ctx.x() + ctx.width() - 100;
        ctx.g().fill(sx, ctx.y() + 9, ctx.x() + ctx.width() - 20, ctx.y() + 12, this.theme.sliderTrack());
        float f = this.max == this.min ? 0f : (float)(this.value - this.min) / (this.max - this.min);
        int kx = sx + (int)(f * 80);
        ctx.g().fill(kx - 1, ctx.y() + 6, kx + 1, ctx.y() + 15, this.theme.sliderKnob());
    }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        int sx = ctx.x() + ctx.width() - 100;
        this.dragging = button == 0 && ctx.mouseX() >= sx && ctx.mouseX() <= sx + 80
                && ctx.mouseY() >= ctx.y() + 6 && ctx.mouseY() <= ctx.y() + 15;
        if (!this.dragging) return false;
        setFromMouse(ctx, sx);
        return true;
    }

    @Override
    public boolean mouseScrolled(RenderContext ctx, double dX, double dY) {
        if (ctx.mouseX() < ctx.x() + ctx.width() - 100 || ctx.mouseX() > ctx.x() + ctx.width() - 20) return false;
        if (ctx.mouseY() < ctx.y() + 6 || ctx.mouseY() > ctx.y() + 15) return false;
        int next = this.value + (dY > 0 ? 1 : -1);
        next = Math.max(this.min, Math.min(this.max, next));
        if (next != this.value) {
            this.value = next;
            if (this.onChange != null) this.onChange.accept(this.value);
            ElementEvents.postValueChanged(this.elementId, String.valueOf(this.value));
        }
        return true;
    }

    @Override
    public boolean mouseDragged(RenderContext ctx, int button, double dragX, double dragY) {
        // Only a drag that started on this slider scrubs it (see {@link #dragging}).
        if (button != 0 || !this.dragging) return false;
        int sx = ctx.x() + ctx.width() - 100;
        setFromMouse(ctx, sx);
        return true;
    }

    private void setFromMouse(RenderContext ctx, int sx) {
        float f = Math.max(0f, Math.min(1f, (ctx.mouseX() - sx) / 80f));
        this.value = this.min + Math.round(f * (this.max - this.min));
        if (this.onChange != null) this.onChange.accept(this.value);
        ElementEvents.postValueChanged(this.elementId, String.valueOf(this.value));
    }
}