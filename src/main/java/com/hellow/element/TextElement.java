package com.hellow.element;

import com.hellow.style.HellowColor;
import com.hellow.text.StringMode;
import com.hellow.text.TextRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class TextElement implements Element {
    private final Component text;
    private final HellowColor color;
    private final StringMode mode;
    private Font cachedFont;
    private int cachedWidth = -1;
    private TextRenderer.Layout cachedLayout;

    public TextElement(Component text, int color) {
        this(text, HellowColor.of(color), (StringMode) null);
    }

    public TextElement(Component text, HellowColor color) {
        this(text, color, (StringMode) null);
    }

    public TextElement(Component text, int color, boolean wrap) {
        this(text, HellowColor.of(color), wrap ? StringMode.WRAP : null);
    }

    public TextElement(Component text, int color, StringMode mode) {
        this(text, HellowColor.of(color), mode);
    }

    public TextElement(Component text, HellowColor color, StringMode mode) {
        this.text = text;
        this.color = color;
        this.mode = mode;
    }

    public static TextElement wrapped(Component text, int color) {
        return new TextElement(text, color, StringMode.WRAP);
    }

    public static TextElement clamped(Component text, int color) {
        return new TextElement(text, color, StringMode.CLAMPED);
    }

    public int width(Font font) {
        return font.width(this.text);
    }

    public int height(Font font, int width) {
        if (this.mode == StringMode.WRAP) {
            return Math.max(1, layout(font, Math.max(1, width)).lineCount()) * font.lineHeight;
        }
        return font.lineHeight;
    }

    public void render(RenderContext ctx) {
        Font font = ctx.font();
        int argb = this.color.argb();
        if (this.mode == null) {
            ctx.g().drawString(font, this.text, ctx.x(), ctx.y(), argb, true);
            return;
        }
        TextRenderer.Layout layout = layout(font, Math.max(1, ctx.width()));
        if (layout.isEmpty()) return;
        if (this.mode == StringMode.WRAP) {
            int y = ctx.y();
            for (FormattedCharSequence line : layout.lines()) {
                ctx.g().drawString(font, line, ctx.x(), y, argb, true);
                y += font.lineHeight;
            }
        } else {
            ctx.g().drawString(font, layout.lines().get(0), ctx.x(), ctx.y(), argb, true);
        }
    }

    private TextRenderer.Layout layout(Font font, int width) {
        if (this.cachedFont != font || this.cachedWidth != width) {
            this.cachedFont = font;
            this.cachedWidth = width;
            this.cachedLayout = TextRenderer.layout(font, this.text, width, this.mode);
        }
        return this.cachedLayout;
    }
}
