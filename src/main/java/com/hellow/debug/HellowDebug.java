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

public final class HellowDebug {

    private HellowDebug() {}

    public static void capture(String phase, Throwable t, Object origin) {
        ErrorCollector.get().capture(phase, t, origin);
    }

    public static List<DebugEntry> entries() {
        return ErrorCollector.get().entries();
    }

    public static int count() { return ErrorCollector.get().count(); }

    public static void clear() { ErrorCollector.get().clear(); }

    public static String exportText() {
        return ErrorCollector.get().exportText();
    }

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

    public static void openDebug() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.setScreen(new DebugScreen(mc.screen));
    }
}
