package com.hellow.element;

import com.hellow.event.ElementEvents;
import com.hellow.event.ElementId;
import com.hellow.style.HellowTheme;
import net.minecraft.client.gui.Font;
import java.util.function.Consumer;

public final class TextInputElement implements Element {
    private final String label;
    private String value;
    private final Consumer<String> onChange;
    private final ElementId elementId;
    private final HellowTheme theme;
    private boolean focused;

    public TextInputElement(String label, String value, Consumer<String> onChange) {
        this(label, value, onChange, null, HellowTheme.DEFAULT);
    }

    public TextInputElement(String label, String value, Consumer<String> onChange, ElementId elementId) {
        this(label, value, onChange, elementId, HellowTheme.DEFAULT);
    }

    public TextInputElement(String label, String value, Consumer<String> onChange, ElementId elementId, HellowTheme theme) {
        this.label = label;
        this.value = value == null ? "" : value;
        this.onChange = onChange;
        this.elementId = elementId;
        this.theme = theme == null ? HellowTheme.DEFAULT : theme;
    }

    public int width(Font font) { return font.width(this.label) + 120; }
    public int height(Font font, int width) { return 20; }

    public void render(RenderContext ctx) {
        ctx.g().drawString(ctx.font(), this.label, ctx.x(), ctx.y() + 6, this.theme.formLabel(), true);
        int bx = ctx.x() + ctx.width() - 100;
        ctx.g().fill(bx, ctx.y() + 3, ctx.x() + ctx.width(), ctx.y() + 17, this.theme.formFieldBackground());
        ctx.g().renderOutline(bx, ctx.y() + 3, 100, 14, this.focused ? this.theme.formOutlineFocused() : this.theme.formOutline());
        ctx.g().drawString(ctx.font(), this.value, bx + 3, ctx.y() + 6, this.theme.formValue(), true);
    }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        if (button != 0) return false;
        int bx = ctx.x() + ctx.width() - 100;
        this.focused = ctx.mouseX() >= bx && ctx.mouseX() <= ctx.x() + ctx.width() && ctx.mouseY() >= ctx.y() && ctx.mouseY() <= ctx.y() + 20;
        return this.focused;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.focused) return false;
        if (keyCode == 259 && !this.value.isEmpty()) {
            this.value = this.value.substring(0, this.value.length() - 1);
            if (this.onChange != null) this.onChange.accept(this.value);
            ElementEvents.postValueChanged(this.elementId, this.value);
            return true;
        }
        if (keyCode == 257) {
            this.focused = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!this.focused) return false;
        this.append(codePoint);
        return true;
    }

    public void append(char c) {
        if (c >= 32 && c != 127) {
            this.value += c;
            if (this.onChange != null) this.onChange.accept(this.value);
            ElementEvents.postValueChanged(this.elementId, this.value);
        }
    }
}