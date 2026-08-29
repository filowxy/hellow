package com.hellow.element;

import com.hellow.event.ElementEvents;
import com.hellow.event.ElementId;
import com.hellow.style.HellowTheme;
import net.minecraft.client.gui.Font;
import java.util.List;
import java.util.function.Consumer;

public final class EnumElement implements Element {
    private final String label;
    private final List<String> options;
    private int index;
    private final Consumer<String> onChange;
    private final ElementId elementId;
    private final HellowTheme theme;

    public EnumElement(String label, List<String> options, int index, Consumer<String> onChange) {
        this(label, options, index, onChange, null, HellowTheme.DEFAULT);
    }

    public EnumElement(String label, List<String> options, int index, Consumer<String> onChange, ElementId elementId) {
        this(label, options, index, onChange, elementId, HellowTheme.DEFAULT);
    }

    public EnumElement(String label, List<String> options, int index, Consumer<String> onChange,
                       ElementId elementId, HellowTheme theme) {
        this.label = label;
        this.options = options;
        this.index = options.isEmpty() ? 0 : Math.max(0, Math.min(options.size() - 1, index));
        this.onChange = onChange;
        this.elementId = elementId;
        this.theme = theme == null ? HellowTheme.DEFAULT : theme;
    }

    public int width(Font font) {
        int w = font.width(this.label) + 20;
        for (String o : this.options) w = Math.max(w, font.width(this.label) + font.width(o) + 30);
        return w;
    }

    public int height(Font font, int width) { return 20; }

    public void render(RenderContext ctx) {
        String cur = this.options.isEmpty() ? "" : this.options.get(this.index);
        ctx.g().drawString(ctx.font(), this.label, ctx.x(), ctx.y() + 6, this.theme.formLabel(), true);
        int vx = ctx.x() + ctx.width() - ctx.font().width(cur) - 4;
        ctx.g().drawString(ctx.font(), cur, vx, ctx.y() + 6, this.theme.formValue(), true);
    }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        if (button != 0 || this.options.isEmpty()) return false;
        if (ctx.mouseX() < ctx.x() || ctx.mouseX() > ctx.x() + ctx.width()) return false;
        if (ctx.mouseY() < ctx.y() || ctx.mouseY() > ctx.y() + 20) return false;
        this.index = (this.index + 1) % this.options.size();
        String next = this.options.get(this.index);
        if (this.onChange != null) this.onChange.accept(next);
        ElementEvents.postValueChanged(this.elementId, next);
        return true;
    }
}