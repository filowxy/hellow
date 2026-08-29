package com.tisam.config;

import com.google.gson.Gson;
import com.tisam.TisamConstants;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads, saves, and provides default values for TISAM's two JSON files:
 * <ul>
 *   <li>{@code <gameDir>/config/tisam/modules.json} — modpack author's recommended list.</li>
 *   <li>{@code <gameDir>/config/tisam/preferences.json} — per-player state.</li>
 * </ul>
 *
 * <p><b>Threading:</b> all public methods are synchronized on the singleton instance.
 * Reads happen on the mod-loading thread (safe), writes happen on the client game
 * thread (also safe). The lock is uncontended in practice.
 *
 * <p><b>Fallback strategy:</b> if a file is missing, a default is written to disk
 * and returned in memory. If a file exists but is malformed, the malformed file is
 * renamed to {@code *.broken-<timestamp>} and a fresh default is written — this
 * prevents a single bad edit from bricking the mod.
 */
public final class ConfigManager {
    private static final ConfigManager INSTANCE = new ConfigManager();

    public static ConfigManager get() { return INSTANCE; }

    private final Gson gson = ModuleEntry.lenientGson();

    private final Path configDir;
    private final Path modulesPath;
    private final Path prefsPath;

    private ModulesConfig modules;
    private PreferencesData prefs;

    private ConfigManager() {
        // FMLPaths.CONFIGDIR is a NeoForge-provided path that resolves to <gameDir>/config
        // at runtime. We resolve() it eagerly so the directory exists by the time anyone
        // calls getModules() / getPreferences().
        this.configDir = FMLPaths.CONFIGDIR.get().resolve(TisamConstants.CONFIG_DIR);
        this.modulesPath = configDir.resolve(TisamConstants.CONFIG_MODULES_FILE);
        this.prefsPath = configDir.resolve(TisamConstants.CONFIG_PREFERENCES_FILE);
    }

    /**
     * Initializes the config directory and loads both files. Safe to call multiple
     * times — subsequent calls are no-ops.
     */
    public synchronized void init() {
        if (modules != null && prefs != null) return;
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            TisamConstants.LOG.error("[tisam] Failed to create config directory {}", configDir, e);
        }
        this.modules = loadOrDefault(modulesPath, ModulesConfig.class, new ModulesConfig(), "modules.json");
        this.prefs = loadOrDefault(prefsPath, PreferencesData.class, new PreferencesData(), "preferences.json");
    }

    /** Returns the loaded modules config. Initializes on first access. */
    public synchronized ModulesConfig getModules() {
        if (modules == null) init();
        return modules;
    }

    /** Returns the loaded preferences. Initializes on first access. */
    public synchronized PreferencesData getPreferences() {
        if (prefs == null) init();
        return prefs;
    }

    /** Persists the current preferences back to disk. */
    public synchronized void savePreferences() {
        if (prefs == null) return;
        writeAtomic(prefsPath, gson.toJson(prefs), "preferences.json");
    }

    /** Persists the current modules config (only needed if TISAM itself mutates it). */
    public synchronized void saveModules() {
        if (modules == null) return;
        writeAtomic(modulesPath, gson.toJson(modules), "modules.json");
    }

    // -------- Internal helpers --------

    private <T> T loadOrDefault(Path path, Class<T> clazz, T defaultValue, String friendlyName) {
        if (!Files.exists(path)) {
            TisamConstants.LOG.info("[tisam] {} not found, writing default at {}", friendlyName, path);
            writeAtomic(path, gson.toJson(defaultValue), friendlyName);
            return defaultValue;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            T parsed = gson.fromJson(reader, clazz);
            if (parsed == null) {
                throw new IOException("Empty or null JSON");
            }
            return parsed;
        } catch (Exception e) {
            TisamConstants.LOG.error("[tisam] Failed to parse {}, falling back to defaults", friendlyName, e);
            quarantineBroken(path, friendlyName);
            writeAtomic(path, gson.toJson(defaultValue), friendlyName);
            return defaultValue;
        }
    }

    private void writeAtomic(Path path, String content, String friendlyName) {
        // Write to <path>.tmp then rename — prevents a half-written file if the
        // game crashes mid-flush. Path.isDirectory() guards against the rare case
        // where someone replaced the file path with a directory.
        try {
            Files.createDirectories(path.getParent());
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                w.write(content);
            }
            Files.move(tmp, path,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            TisamConstants.LOG.error("[tisam] Failed to write {}", friendlyName, e);
        }
    }

    private void quarantineBroken(Path path, String friendlyName) {
        try {
            String name = path.getFileName().toString();
            Path broken = path.resolveSibling(name + ".broken-" + System.currentTimeMillis());
            Files.move(path, broken);
            TisamConstants.LOG.warn("[tisam] Moved malformed {} to {}", friendlyName, broken);
        } catch (IOException e) {
            TisamConstants.LOG.error("[tisam] Failed to quarantine malformed {}", friendlyName, e);
        }
    }
}
