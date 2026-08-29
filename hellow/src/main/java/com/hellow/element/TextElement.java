package com.hellow.element;

import com.hellow.text.StringMode;
import com.hellow.text.TextRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * A text element rendered through hellow's {@link TextRenderer}, so the layout
 * mode is part of the element: single-line, auto-wrapping, ellipsis-clamped or
 * collapsible.
 *
 * <p>The mode decides both the measurement rules ({@link #height}) and the drawing
 * ({@link #render}): WRAP draws every line, the other modes draw a single line
 * (CLAMPED / COLLAPSIBLE truncate with an ellipsis when the width doesn't fit).
 * The embedding mod never touches raw coordinates or {@code Font} math for these
 * cases — it declares text + mode + color and hellow does the rest.
 */
public final class TextElement implements Element {
    private final Component text;
    private final int color;

    /**
     * Layout mode; {@code null} = legacy single-line (draw the whole text, overflow
     * allowed) — kept so pre-mode callers behave exactly as before.
     */
    private final StringMode mode;

    public TextElement(Component text, int color) {
        this(text, color, (StringMode) null);
    }

    /**
     * @param wrap {@code true} = WRAP (multi-line, height grows); {@code false} =
     *             legacy single-line without truncation. Backwards compatible.
     */
    public TextElement(Component text, int color, boolean wrap) {
        this(text, color, wrap ? StringMode.WRAP : null);
    }

    /** @param mode see {@link StringMode}; {@code null} = legacy single-line */
    public TextElement(Component text, int color, StringMode mode) {
        this.text = text;
        this.color = color;
        this.mode = mode;
    }

    /** Convenience factory for auto-wrapping text. */
    public static TextElement wrapped(Component text, int color) {
        return new TextElement(text, color, StringMode.WRAP);
    }

    /** Convenience factory for hard-clamped single-line text (table cells, URLs). */
    public static TextElement clamped(Component text, int color) {
        return new TextElement(text, color, StringMode.CLAMPED);
    }

    public int width(Font font) {
        return font.width(this.text);
    }

    public int height(Font font, int width) {
        if (this.mode == StringMode.WRAP) {
            TextRenderer.Layout layout = TextRenderer.layout(font, this.text, Math.max(1, width), StringMode.WRAP);
            return Math.max(1, layout.lineCount()) * font.lineHeight;
        }
        return font.lineHeight;
    }

    public void render(RenderContext ctx) {
        Font font = ctx.font();
        if (this.mode == null) {
            // Legacy single-line: draw the whole text even if it overflows.
            ctx.g().drawString(font, this.text, ctx.x(), ctx.y(), this.color, true);
            return;
        }
        TextRenderer.Layout layout = TextRenderer.layout(font, this.text, Math.max(1, ctx.width()), this.mode);
        if (layout.isEmpty()) return;
        if (this.mode == StringMode.WRAP) {
            int y = ctx.y();
            for (FormattedCharSequence line : layout.lines()) {
                ctx.g().drawString(font, line, ctx.x(), y, this.color, true);
                y += font.lineHeight;
            }
        } else {
            // Single-line modes: draw the (possibly ellipsis-truncated) first line.
            ctx.g().drawString(font, layout.lines().get(0), ctx.x(), ctx.y(), this.color, true);
        }
    }
}