package com.hellow.style;

import com.anthonyhilyard.prism.text.DynamicColor;
import com.anthonyhilyard.prism.util.ConfigHelper;
import com.anthonyhilyard.prism.util.IColor;
import net.minecraft.network.chat.TextColor;

import java.util.List;
import java.util.stream.Collectors;

public final class HellowColors {
    private HellowColors() {}

    public static TextColor of(int argb) {
        return TextColor.fromRgb(argb & 0xFFFFFF);
    }

    public static TextColor animated(List<Integer> argbFrames, float durationSeconds) {
        if (argbFrames == null || argbFrames.isEmpty()) {
            throw new IllegalArgumentException("animated color needs at least one frame");
        }
        List<IColor> frames = argbFrames.stream()
                .map(argb -> DynamicColor.fromRgb(argb & 0xFFFFFF))
                .collect(Collectors.toList());
        return new DynamicColor(frames, Math.max(0.01f, durationSeconds));
    }

    public static TextColor parse(String spec) {
        if (spec == null || spec.isEmpty()) return null;
        IColor parsed = ConfigHelper.parseColor(spec);
        if (parsed == null) return null;
        return parsed instanceof DynamicColor dynamic ? dynamic : DynamicColor.fromColor(parsed);
    }
}
