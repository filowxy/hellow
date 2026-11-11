package com.hellow.style;

import com.hellow.HellowConstants;
import net.minecraft.network.chat.TextColor;

import java.util.List;

public interface HellowColor {
    int argb();

    static HellowColor of(int argb) {
        return () -> argb;
    }

    static HellowColor of(TextColor color) {
        return () -> {
            int v = color.getValue();
            return (v & 0xFF000000) == 0 ? v | 0xFF000000 : v;
        };
    }

    static HellowColor animated(List<Integer> argbFrames, float durationSeconds) {
        if (argbFrames == null || argbFrames.isEmpty()) {
            throw new IllegalArgumentException("animated color needs at least one frame");
        }
        final long periodMs = Math.max(1, (long) (durationSeconds * 1000));
        final int[] frames = argbFrames.stream().mapToInt(c -> c & 0xFFFFFF).toArray();
        if (frames.length == 1) return of(0xFF000000 | frames[0]);
        return () -> {
            float pos = (System.currentTimeMillis() % periodMs) / (float) periodMs * frames.length;
            int i = (int) pos % frames.length;
            float t = pos - (int) pos;
            return 0xFF000000 | lerp(frames[i], frames[(i + 1) % frames.length], t);
        };
    }

    private static int lerp(int from, int to, float t) {
        int r = (int) ((from >> 16 & 0xFF) + ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * t);
        int g = (int) ((from >> 8 & 0xFF) + ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * t);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return r << 16 | g << 8 | b;
    }

    static HellowColor parse(String prismSpec) {
        TextColor parsed = HellowColors.parse(prismSpec);
        if (parsed == null) {
            HellowConstants.log().warn("[hellow] unparsable color spec '{}', falling back to white", prismSpec);
            return of(0xFFFFFFFF);
        }
        return of(parsed);
    }
}
