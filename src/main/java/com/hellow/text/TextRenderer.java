package com.hellow.text;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TextRenderer {
    private TextRenderer() {}

    public static final String ELLIPSIS = "\u2026";

    public static Layout layout(Font font, Component text, int maxWidth, StringMode mode) {
        if (font == null) {
            return Layout.EMPTY;
        }

        if (text == null || text.getString().isEmpty()) {
            return Layout.EMPTY;
        }

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
        if (font.width(text) <= maxWidth) {
            return new Layout(List.of(text.getVisualOrderText()), false, false);
        }

        String full = text.getString();
        int ellipsisWidth = font.width(ELLIPSIS);
        int budget = maxWidth - ellipsisWidth;
        if (budget <= 0) {
            return new Layout(List.of(FormattedCharSequence.forward(ELLIPSIS, Style.EMPTY)), true, false);
        }

        int lo = 0;
        int hi = full.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (font.width(full.substring(0, mid)) <= budget) lo = mid;
            else hi = mid - 1;
        }
        String kept = full.substring(0, lo) + ELLIPSIS;
        return new Layout(
                List.of(FormattedCharSequence.forward(kept, Style.EMPTY)),
                true, false);
    }

    private static Layout collapsible(Font font, Component text, int maxWidth) {
        List<FormattedCharSequence> all = font.split(text, maxWidth);
        if (all.isEmpty()) return Layout.EMPTY;
        boolean truncated = all.size() > 1;
        return new Layout(List.of(all.get(0)), truncated, truncated);
    }

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

        public List<FormattedCharSequence> withEllipsis() {
            if (lines.isEmpty()) return lines;
            List<FormattedCharSequence> copy = new ArrayList<>(lines);
            FormattedCharSequence last = copy.remove(copy.size() - 1);
            copy.add(FormattedCharSequence.composite(last, FormattedCharSequence.forward(ELLIPSIS, Style.EMPTY)));
            return copy;
        }
    }
}
