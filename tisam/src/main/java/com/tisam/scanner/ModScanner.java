package com.tisam.scanner;

import com.tisam.TisamConstants;
import com.tisam.config.ModuleEntry;
import com.tisam.config.ModuleEntry.ContentType;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scans the four Minecraft content directories and reports which
 * {@link ModuleEntry}s from {@code modules.json} are already installed.
 *
 * <p>v2.0 supports four content types (per 最新更新.md §2):
 * <table border="1">
 *   <caption>Content type → on-disk layout</caption>
 *   <tr><th>type</th><th>directory</th><th>file extension</th></tr>
 *   <tr><td>{@link ContentType#MOD MOD}</td><td>{@code mods/}</td><td>{@code .jar}</td></tr>
 *   <tr><td>{@link ContentType#RESOURCEPACK RESOURCEPACK}</td><td>{@code resourcepacks/}</td><td>{@code .zip}</td></tr>
 *   <tr><td>{@link ContentType#SHADERPACK SHADERPACK}</td><td>{@code shaderpacks/}</td><td>{@code .zip}</td></tr>
 *   <tr><td>{@link ContentType#DATAPACK DATAPACK}</td><td>{@code datapacks/}</td><td>{@code .zip}</td></tr>
 * </table>
 *
 * <p>Detection strategy (unchanged from v1.0): <b>match by file basename</b>.
 * A recommended mod with {@code id="footprint_particles"} is considered
 * installed if {@code mods/footprint_particles*.jar} exists. This is
 * intentionally simpler and more reliable than reading jar manifests, at
 * the cost of breaking if the user renames the file. Same strategy
 * applies to resourcepacks / shaderpacks / datapacks.
 *
 * <p>TISAM deliberately does not perform runtime environment diagnostics
 * (e.g. checking whether Iris is installed for shader packs). Modpack
 * authors are expected to ensure the prerequisite mod is bundled with
 * the pack — see 最新更新.md §2.
 */
public final class ModScanner {
    /** Suffix matcher: filename starts with {@code id} or {@code id-<rest>}. */
    private static final String VERSION_SEPARATOR = "-";

    private ModScanner() {}

    /**
     * Returns the set of recommended {@code (type:id)} tokens that are
     * already installed. We key on both type and id because the same id
     * could in principle exist as both a mod and a resource pack — the
     * scanner must not collapse them.
     *
     * <p>Callers that don't care about the type (most do) can call
     * {@link #scanInstalled(Path, List, ContentType)} with {@code null} to
     * get an id-only set, matching the v1.0 return shape.
     */
    public static Set<String> scanInstalled(Path gameDir, List<ModuleEntry> modules) {
        if (modules == null || modules.isEmpty()) return Collections.emptySet();
        EnumMap<ContentType, Set<String>> perType = new EnumMap<>(ContentType.class);
        for (ContentType t : ContentType.values()) {
            perType.put(t, scanType(gameDir, modules, t));
        }
        Set<String> out = new HashSet<>();
        for (Map.Entry<ContentType, Set<String>> e : perType.entrySet()) {
            for (String id : e.getValue()) out.add(e.getKey().name().toLowerCase(Locale.ROOT) + ":" + id);
        }
        return out;
    }

    /**
     * Returns the subset of {@code modules} that are NOT installed.
     * Convenience wrapper used by the title-screen flow.
     */
    public static List<ModuleEntry> findMissing(Path gameDir, List<ModuleEntry> modules) {
        if (modules == null || modules.isEmpty()) return Collections.emptyList();
        Set<String> installed = scanInstalled(gameDir, modules);
        return modules.stream()
                .filter(m -> m.id != null && !m.id.isEmpty())
                .filter(m -> !installed.contains(typeToken(m.type, m.id)))
                .collect(Collectors.toList());
    }

    /**
     * Returns the set of installed IDs for a single content type. Useful
     * for unit tests and for callers that already know which type they
     * care about.
     */
    public static Set<String> scanInstalled(Path gameDir, List<ModuleEntry> modules, ContentType type) {
        if (type == null) type = ContentType.MOD;
        Set<String> perType = scanType(gameDir, modules, type);
        // Re-key to the (type:id) form so callers mixing types don't
        // confuse two mods that happen to share an id.
        Set<String> out = new HashSet<>(perType.size());
        for (String id : perType) out.add(typeToken(type, id));
        return out;
    }

    /**
     * Returns the (lower-cased) installed basenames for a single type —
     * extension stripped, case-insensitive match. The returned ids are
     * the {@link ModuleEntry#id} values, not the on-disk filenames, so
     * callers can intersect with the recommendation list directly.
     */
    private static Set<String> scanType(Path gameDir, List<ModuleEntry> modules, ContentType type) {
        if (modules == null || modules.isEmpty()) return Collections.emptySet();
        ScanLayout layout = layoutFor(type);
        Path dir = gameDir.resolve(layout.dirName);
        if (!Files.isDirectory(dir)) {
            TisamConstants.LOG.debug("[tisam] content directory not found at {} (type={})", dir, type);
            return Collections.emptySet();
        }
        Set<String> installedBasenames;
        try {
            installedBasenames = listBasenames(dir, layout.extension);
        } catch (IOException e) {
            TisamConstants.LOG.error("[tisam] Failed to scan content directory {} (type={})", dir, type, e);
            return Collections.emptySet();
        }
        return modules.stream()
                // null-type entries default to MOD (see ModuleEntry; Gson can leave a
                // field null when the JSON explicitly writes "type": null) — otherwise
                // such entries never match any scan and are forever "missing".
                .filter(m -> (m.type == null ? ContentType.MOD : m.type) == type)
                .map(m -> m.id)
                .filter(id -> id != null && !id.isEmpty())
                .filter(id -> matchesAny(id, installedBasenames))
                .collect(Collectors.toCollection(HashSet::new));
    }

    /** The "type:id" token used in the cross-type set returned by {@link #scanInstalled}. */
    private static String typeToken(ContentType type, String id) {
        return (type == null ? ContentType.MOD : type).name().toLowerCase(Locale.ROOT) + ":" + id;
    }

    /** Where a given content type lives on disk. */
    private record ScanLayout(String dirName, String extension) {}

    private static ScanLayout layoutFor(ContentType type) {
        // ponytail: the layout table is the single source of truth — adding
        // a new ContentType means adding one row here, not three switch
        // statements in three places.
        return switch (type) {
            case MOD         -> new ScanLayout("mods",         ".jar");
            case RESOURCEPACK -> new ScanLayout("resourcepacks", ".zip");
            case SHADERPACK  -> new ScanLayout("shaderpacks",  ".zip");
            case DATAPACK    -> new ScanLayout("datapacks",    ".zip");
        };
    }

    /**
     * Returns true if {@code id} matches any installed basename. The match
     * is case-insensitive prefix-based: a mod with id {@code footprint_particles}
     * is considered installed if a file named {@code footprint_particles-1.0.jar},
     * {@code Footprint_Particles-2.0-snapshot.jar}, etc., exists.
     *
     * <p>This is intentionally lenient: most mods on Modrinth / CurseForge
     * carry version suffixes, and we want detection to keep working when the
     * player updates the mod. The downside is that id {@code foo} will also
     * match a file named {@code foo_bar-1.0.jar} — modpack authors should
     * pick sufficiently specific ids.
     */
    private static boolean matchesAny(String id, Set<String> installedBasenames) {
        String idLower = id.toLowerCase(Locale.ROOT);
        for (String basename : installedBasenames) {
            if (basename.equals(idLower) || basename.startsWith(idLower + VERSION_SEPARATOR)) {
                return true;
            }
        }
        return false;
    }

    /** Lists all basenames (lowercased, extension stripped) in the given directory. */
    private static Set<String> listBasenames(Path dir, String extension) throws IOException {
        Set<String> out = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*" + extension)) {
            for (Path file : stream) {
                String name = file.getFileName().toString();
                String base = name.substring(0, name.length() - extension.length());
                out.add(base.toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }
}
