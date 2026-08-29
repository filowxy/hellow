package com.hellow.element;

import com.hellow.event.ElementEvents;
import com.hellow.event.ElementId;
import com.hellow.style.HellowTheme;
import net.minecraft.client.gui.Font;
import java.util.function.Consumer;

public final class ToggleElement implements Element {
    private final String label;
    private boolean value;
    private final Consumer<Boolean> onChange;
    private final ElementId elementId;
    private final HellowTheme theme;

    public ToggleElement(String label, boolean value, Consumer<Boolean> onChange) {
        this(label, value, onChange, null, HellowTheme.DEFAULT);
    }

    public ToggleElement(String label, boolean value, Consumer<Boolean> onChange, ElementId elementId) {
        this(label, value, onChange, elementId, HellowTheme.DEFAULT);
    }

    public ToggleElement(String label, boolean value, Consumer<Boolean> onChange, ElementId elementId, HellowTheme theme) {
        this.label = label;
        this.value = value;
        this.onChange = onChange;
        this.elementId = elementId;
        this.theme = theme == null ? HellowTheme.DEFAULT : theme;
    }

    public int width(Font font) { return font.width(this.label) + 34; }
    public int height(Font font, int width) { return 20; }

    public void render(RenderContext ctx) {
        ctx.g().drawString(ctx.font(), this.label, ctx.x(), ctx.y() + 6, this.theme.formLabel(), true);
        int bx = ctx.x() + ctx.width() - 24;
        ctx.g().fill(bx, ctx.y() + 4, bx + 20, ctx.y() + 16, this.theme.buttonBackground());
        ctx.g().renderOutline(bx, ctx.y() + 4, 20, 12, this.theme.formOutline());
        if (this.value) {
            ctx.g().fill(bx + 3, ctx.y() + 7, bx + 17, ctx.y() + 13, this.theme.toggleKnobOn());
        }
    }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        if (button != 0) return false;
        int bx = ctx.x() + ctx.width() - 24;
        if (ctx.mouseX() >= bx && ctx.mouseX() <= bx + 20 && ctx.mouseY() >= ctx.y() + 4 && ctx.mouseY() <= ctx.y() + 16) {
            this.value = !this.value;
            if (this.onChange != null) this.onChange.accept(this.value);
            ElementEvents.postValueChanged(this.elementId, String.valueOf(this.value));
            return true;
        }
        return false;
    }
}