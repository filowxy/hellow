package com.hellow;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HellowConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "hellow/hellow.json";

    private static volatile HellowConfig instance;

    public boolean debug = false;

    private HellowConfig() {}

    public static HellowConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    public boolean debug() {
        return this.debug;
    }

    static Path configFile() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }

    private static HellowConfig load() {
        Path file = configFile();
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, GSON.toJson(new HellowConfig()), StandardCharsets.UTF_8);
                HellowConstants.log().info("[hellow] wrote default config to {}", file);
                return new HellowConfig();
            }
            HellowConfig parsed = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), HellowConfig.class);
            return parsed == null ? new HellowConfig() : parsed;
        } catch (Exception e) {
            HellowConstants.log().warn("[hellow] could not read {} ({}); using defaults", file, e.toString());
            return new HellowConfig();
        }
    }
}
