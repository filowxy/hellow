package com.hellow.debug;

import com.hellow.HellowConstants;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Public facade for Hellow's debug facilities: capture an error, read back the
 * report, export it to disk, or open the in-game {@link DebugScreen}.
 */
public final class HellowDebug {

    private HellowDebug() {}

    /** Record an error. Call this at Hellow's dispatch boundaries. */
    public static void capture(String phase, Throwable t, Object origin) {
        ErrorCollector.get().capture(phase, t, origin);
    }

    /** Snapshot of captured entries. */
    public static List<DebugEntry> entries() {
        return ErrorCollector.get().entries();
    }

    public static int count() { return ErrorCollector.get().count(); }

    public static void clear() { ErrorCollector.get().clear(); }

    /** Render the whole report as a single text block. */
    public static String exportText() {
        return ErrorCollector.get().exportText();
    }

    /**
     * Write the report under {@code config/hellow/debug/report-<timestamp>.log}.
     * Returns the written path, or {@code null} on failure.
     */
    public static Path exportFile() {
        try {
            Path dir = FMLPaths.CONFIGDIR.get().resolve("hellow/debug");
            Files.createDirectories(dir);
            String name = "report-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                    .format(LocalDateTime.now()) + ".log";
            Path target = dir.resolve(name);
            Files.writeString(target, exportText(), StandardCharsets.UTF_8);
            HellowConstants.log().info("[hellow] debug report exported to {}", target);
            return target;
        } catch (IOException e) {
            HellowConstants.log().error("[hellow] failed to export debug report", e);
            return null;
        }
    }

    /** Open the in-game debug report screen (no-op if called outside the game thread). */
    public static void openDebug() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.setScreen(new DebugScreen(mc.screen));
    }
}