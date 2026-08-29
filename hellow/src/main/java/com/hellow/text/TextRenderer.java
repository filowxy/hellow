package com.hellow.text;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Layout engine for the three {@link StringMode}s.
 *
 * <p>Thin wrapper over Minecraft's {@link Font#split(Component, int)},
 * which already handles Unicode shaping, RTL, and the language fallback
 * chain. Hellow adds three things on top:
 * <ol>
 *   <li>Single-line vs multi-line semantics per mode (split returns
 *       one or more lines depending on width; Hellow picks which mode
 *       keeps all lines vs which trims to the first).</li>
 *   <li>An ellipsis suffix ('…') that the embedding mod can render
 *       inline. We never modify the original sequence — the suffix is
 *       a separate {@link FormattedCharSequence} the caller appends at
 *       draw time.</li>
 *   <li>A {@link #wasTruncated} flag so the embedding mod can show
 *       the COLLAPSIBLE expand button only when there's actually more
 *       text to reveal.</li>
 * </ol>
 *
 * <p>This class is stateless. Callers can reuse it from any thread that
 * has access to a {@link Font} (which is main-thread only in practice,
 * since {@code Minecraft.getInstance().font} is loaded lazily on the
 * client thread).
 */
public final class TextRenderer {
    private TextRenderer() {}

    /** Single-line ellipsis used by COLLAPSIBLE and CLAMPED modes. */
    public static final String ELLIPSIS = "\u2026";

    /**
     * Layout a {@link Component} into one or more lines according to
     * {@code mode}. The font is the active client font, normally
     * {@code Minecraft.getInstance().font}.
     *
     * @return a {@link Layout} carrying the lines, the truncation flag,
     *         and an optional "expand hint" — for COLLAPSIBLE the
     *         hint is the same as {@code wasTruncated}; for WRAP and
     *         CLAMPED it is always {@code false}.
     */
    public static Layout layout(Font font, Component text, int maxWidth, StringMode mode) {
        if (font == null) {
            // Defensive: caller forgot the font. Return an empty layout
            // rather than NPE — the embedding mod will simply not draw
            // anything for this region, which is the same fallback
            // Minecraft itself uses when the font isn't ready yet.
            return Layout.EMPTY;
        }

        if (text == null || text.getString().isEmpty()) {
            return Layout.EMPTY;
        }

        // Guard the width: font.split() misbehaves on <= 0 (empty or full-string
        // results depending on version). A 1px budget degrades gracefully.
        int safeWidth = Math.max(1, maxWidth);
        return switch (mode) {
            case WRAP -> wrap(font, text, safeWidth);
            case CLAMPED -> clamp(font, text, safeWidth);
            case COLLAPSIBLE -> collapsible(font, text, safeWidth);
        };
    }

    private static Layout wrap(Font font, Component text, int maxWidth) {
        List<FormattedCharSequence> lines = font.split(text, maxWidth);
        return new Layout(List.copyOf(lines), false, false);
    }

    private static Layout clamp(Font font, Component text, int maxWidth) {
        // CLAMPED = one line, truncated with ellipsis if it doesn't fit.
        // Strategy: try the full string on one line; if it fits, return
        // it. Otherwise trim characters one at a time and add the
        // ellipsis. Cheap enough for short cells (the design doc's
        // URL use-case is ~60 chars max).
        if (font.width(text) <= maxWidth) {
            return new Layout(List.of(text.getVisualOrderText()), false, false);
        }

        // Build a truncated visual sequence character by character.
        // Avoids font.split() because split always wraps on word
        // boundaries, which is the wrong behaviour for CLAMPED.
        String full = text.getString();
        int ellipsisWidth = font.width(ELLIPSIS);
        int budget = maxWidth - ellipsisWidth;
        if (budget <= 0) {
            // Container is too narrow even for the ellipsis. Return the
            // ellipsis alone as the best we can do.
            return new Layout(List.of(FormattedCharSequence.forward(ELLIPSIS, Style.EMPTY)), true, false);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < full.length(); i++) {
            String candidate = sb.toString() + full.charAt(i);
            if (font.width(candidate) > budget) break;
            sb.append(full.charAt(i));
        }
        sb.append(ELLIPSIS);
        return new Layout(
                List.of(FormattedCharSequence.forward(sb.toString(), Style.EMPTY)),
                true, false);
    }

    private static Layout collapsible(Font font, Component text, int maxWidth) {
        // COLLAPSIBLE = first line that fits, with an expand affordance
        // if there's more text. font.split() gives us the wrapped form;
        // we take line 0 and report whether the line count > 1.
        List<FormattedCharSequence> all = font.split(text, maxWidth);
        if (all.isEmpty()) return Layout.EMPTY;
        boolean truncated = all.size() > 1;
        return new Layout(List.of(all.get(0)), truncated, truncated);
    }

    /**
     * Result of a {@link #layout} call. Immutable; safe to pass to
     * other threads or store in fields.
     */
    public static final class Layout {
        public static final Layout EMPTY = new Layout(Collections.emptyList(), false, false);

        private final List<FormattedCharSequence> lines;
        private final boolean truncated;
        private final boolean canExpand;

        public Layout(List<FormattedCharSequence> lines, boolean truncated, boolean canExpand) {
            this.lines = lines;
            this.truncated = truncated;
            this.canExpand = canExpand;
        }

        public List<FormattedCharSequence> lines() { return lines; }
        public boolean wasTruncated() { return truncated; }
        public boolean canExpand() { return canExpand; }
        public int lineCount() { return lines.size(); }
        public boolean isEmpty() { return lines.isEmpty(); }

        /**
         * Append an ellipsis to the last line. Returns a new list —
         * the original {@code lines} is left untouched so COLLAPSIBLE
         * callers can hand the same layout to a "show full" toggle
         * without re-splitting.
         */
        public List<FormattedCharSequence> withEllipsis() {
            if (lines.isEmpty()) return lines;
            List<FormattedCharSequence> copy = new ArrayList<>(lines);
            FormattedCharSequence last = copy.remove(copy.size() - 1);
            copy.add(FormattedCharSequence.composite(last, FormattedCharSequence.forward(ELLIPSIS, Style.EMPTY)));
            return copy;
        }
    }
}
