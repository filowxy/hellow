package com.tisam.scanner;

import com.tisam.config.ModuleEntry;
import com.tisam.config.ModuleEntry.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ModScanner — v2.0 multi-type scan coverage.
 *
 * <p>v2.0 added four content types (mod / resourcepack / shaderpack /
 * datapack), each living in its own on-disk directory with a different
 * file extension. The scanner must distinguish between them so the same
 * id can exist as both a mod and a resource pack.
 */
class ModScannerTest {
    @Test
    void findsInstalledModsByJarName(@TempDir Path gameDir) throws IOException {
        Path mods = gameDir.resolve("mods");
        Files.createDirectories(mods);
        Files.createFile(mods.resolve("footprint_particles-1.0.jar"));
        Files.createFile(mods.resolve("leaf_optimizer-fabric.jar"));
        // Unrelated mod — should not match.
        Files.createFile(mods.resolve("jei-19.21.0.247.jar"));

        List<ModuleEntry> modules = List.of(
                new ModuleEntry("footprint_particles", "Footprint", "desc", "ARR",
                        "https://example.com/a", true),
                new ModuleEntry("leaf_optimizer", "LeafOpt", "desc", "MIT",
                        "https://example.com/b", true),
                new ModuleEntry("missing_mod", "Missing", "desc", "MIT",
                        "https://example.com/c", true)
        );

        // v2.0: scanInstalled returns type:id tokens, not bare ids.
        Set<String> installed = ModScanner.scanInstalled(gameDir, modules);
        assertEquals(2, installed.size(), "Should detect two installed mods");
        assertTrue(installed.contains("mod:footprint_particles"),
                "Detected footprint_particles should be keyed mod:<id>");
        assertTrue(installed.contains("mod:leaf_optimizer"));
        assertFalse(installed.contains("mod:missing_mod"));
    }

    @Test
    void findMissingExcludesInstalled(@TempDir Path gameDir) throws IOException {
        Path mods = gameDir.resolve("mods");
        Files.createDirectories(mods);
        Files.createFile(mods.resolve("leaf_optimizer.jar"));

        List<ModuleEntry> modules = List.of(
                new ModuleEntry("footprint_particles", "FP", "d", "ARR", "u", true),
                new ModuleEntry("leaf_optimizer", "LO", "d", "MIT", "u", true)
        );

        List<ModuleEntry> missing = ModScanner.findMissing(gameDir, modules);
        assertEquals(1, missing.size());
        assertEquals("footprint_particles", missing.get(0).id);
    }

    @Test
    void missingModsDirectoryReturnsEmpty(@TempDir Path gameDir) {
        // No mods/ directory created — scanInstalled should return empty, not throw.
        List<ModuleEntry> modules = List.of(
                new ModuleEntry("any", "Any", "d", "ARR", "u", true)
        );
        assertEquals(0, ModScanner.scanInstalled(gameDir, modules).size());
        // Play-it-safe policy: when we can't tell, assume the mod IS missing so the
        // player gets a chance to install it. Better one extra prompt than missing
        // a recommendation entirely.
        assertEquals(1, ModScanner.findMissing(gameDir, modules).size());
    }

    @Test
    void caseInsensitiveMatching(@TempDir Path gameDir) throws IOException {
        Path mods = gameDir.resolve("mods");
        Files.createDirectories(mods);
        // Unusual casing — should still match.
        Files.createFile(mods.resolve("FooTprint_Particles.jar"));

        List<ModuleEntry> modules = List.of(
                new ModuleEntry("footprint_particles", "FP", "d", "ARR", "u", true)
        );

        Set<String> installed = ModScanner.scanInstalled(gameDir, modules);
        assertTrue(installed.contains("mod:footprint_particles"));
    }

    // -------- v2.0 multi-type tests --------

    @Test
    void scansFourContentDirectories(@TempDir Path gameDir) throws IOException {
        // One installed file per type. createDirs is needed because
        // Files.createFile() throws NoSuchFileException if the parent is
        // missing (ponytail: this is a Windows-only quirk — on POSIX
        // createFile() silently creates the missing parent).
        Files.createDirectories(gameDir.resolve("mods"));
        Files.createDirectories(gameDir.resolve("resourcepacks"));
        Files.createDirectories(gameDir.resolve("shaderpacks"));
        Files.createDirectories(gameDir.resolve("datapacks"));
        Files.createFile(gameDir.resolve("mods/footprint_particles-1.0.jar"));
        Files.createFile(gameDir.resolve("resourcepacks/ve_pbr_materials.zip"));
        Files.createFile(gameDir.resolve("shaderpacks/blocky_shader.zip"));
        Files.createFile(gameDir.resolve("datapacks/ve_advancements.zip"));

        List<ModuleEntry> modules = List.of(
                entry("footprint_particles", ContentType.MOD),
                entry("ve_pbr_materials",   ContentType.RESOURCEPACK),
                entry("blocky_shader",      ContentType.SHADERPACK),
                entry("ve_advancements",    ContentType.DATAPACK),
                entry("not_installed",      ContentType.MOD)
        );

        List<ModuleEntry> missing = ModScanner.findMissing(gameDir, modules);
        assertEquals(1, missing.size(), "Only not_installed should be reported missing");
        assertEquals("not_installed", missing.get(0).id);
    }

    @Test
    void sameIdUnderDifferentTypesIsIndependent(@TempDir Path gameDir) throws IOException {
        // Both a mod and a resource pack named "shared" exist on disk. The
        // scanner must report both as installed (no collision).
        Files.createDirectories(gameDir.resolve("mods"));
        Files.createDirectories(gameDir.resolve("resourcepacks"));
        Files.createFile(gameDir.resolve("mods/shared.jar"));
        Files.createFile(gameDir.resolve("resourcepacks/shared.zip"));

        List<ModuleEntry> modules = List.of(
                entry("shared", ContentType.MOD),
                entry("shared", ContentType.RESOURCEPACK)
        );

        Set<String> installed = ModScanner.scanInstalled(gameDir, modules);
        assertEquals(2, installed.size(), "mod:shared and resourcepack:shared should both be installed");
        assertTrue(installed.contains("mod:shared"));
        assertTrue(installed.contains("resourcepack:shared"));

        // findMissing returns the empty list — both are satisfied.
        assertEquals(0, ModScanner.findMissing(gameDir, modules).size());
    }

    @Test
    void resourcepackExtensionIsZipNotJar(@TempDir Path gameDir) throws IOException {
        // A mod file in the resourcepacks/ folder must NOT be detected as a
        // resource pack — the scanner respects the per-type extension.
        Files.createDirectories(gameDir.resolve("resourcepacks"));
        Files.createFile(gameDir.resolve("resourcepacks/some_resourcepack.jar"));

        List<ModuleEntry> modules = List.of(
                entry("some_resourcepack", ContentType.RESOURCEPACK)
        );

        assertEquals(1, ModScanner.findMissing(gameDir, modules).size(),
                ".jar in resourcepacks/ should not count as installed");
    }

    @Test
    void perTypeScanIsolatesTypes(@TempDir Path gameDir) throws IOException {
        Files.createDirectories(gameDir.resolve("mods"));
        Files.createDirectories(gameDir.resolve("resourcepacks"));
        Files.createFile(gameDir.resolve("mods/footprint_particles-1.0.jar"));
        Files.createFile(gameDir.resolve("resourcepacks/footprint_particles.zip"));

        List<ModuleEntry> modules = List.of(
                entry("footprint_particles", ContentType.MOD),
                entry("footprint_particles", ContentType.RESOURCEPACK)
        );

        // Single-type scan: only mod should be installed (not the .zip).
        Set<String> modsOnly = ModScanner.scanInstalled(gameDir, modules, ContentType.MOD);
        assertTrue(modsOnly.contains("mod:footprint_particles"));
        assertFalse(modsOnly.contains("resourcepack:footprint_particles"));

        Set<String> rpOnly = ModScanner.scanInstalled(gameDir, modules, ContentType.RESOURCEPACK);
        assertTrue(rpOnly.contains("resourcepack:footprint_particles"));
        assertFalse(rpOnly.contains("mod:footprint_particles"));
    }

    @Test
    void missingDirsAreQuietlyTreatedAsEmpty(@TempDir Path gameDir) {
        // No content directories at all — every entry is missing.
        List<ModuleEntry> modules = List.of(
                entry("a", ContentType.MOD),
                entry("b", ContentType.RESOURCEPACK),
                entry("c", ContentType.SHADERPACK),
                entry("d", ContentType.DATAPACK)
        );
        // No exception; everything is missing.
        assertEquals(4, ModScanner.findMissing(gameDir, modules).size());
    }

    private static ModuleEntry entry(String id, ContentType type) {
        ModuleEntry m = new ModuleEntry(id, id, "d", "ARR", "https://x/" + id, true);
        m.type = type;
        return m;
    }
}
