package com.tisam.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

/**
 * One entry in the recommended-content list.
 *
 * <p>As of v2.0, an entry can recommend any of the four content types
 * Minecraft supports: {@link ContentType#MOD MOD}, {@link ContentType#RESOURCEPACK
 * RESOURCEPACK}, {@link ContentType#SHADERPACK SHADERPACK}, or
 * {@link ContentType#DATAPACK DATAPACK}. {@code type} defaults to {@code MOD}
 * so v1.0 modpack configs (no {@code type} field) keep working unchanged.
 *
 * <p>All fields except {@code id} and {@code name} are optional — Gson tolerates
 * missing JSON keys; consumers fall back to safe defaults. See the design document
 * (项目设计.md §三 / 最新更新.md §6) for the full field reference.
 */
public final class ModuleEntry {
    /**
     * What kind of Minecraft content this entry recommends.
     *
     * <p>Each type maps to a different on-disk directory and a different file
     * extension. TISAM scans the right directory per type — see
     * {@link com.tisam.scanner.ModScanner}.
     */
    public enum ContentType {
        @SerializedName("mod")         MOD,
        @SerializedName("resourcepack") RESOURCEPACK,
        @SerializedName("shaderpack")  SHADERPACK,
        @SerializedName("datapack")    DATAPACK;

        /**
         * Lenient parser: unknown / missing / null → {@link #MOD}. Modpack
         * authors will mistype values; we want a working config, not a crash.
         */
        public static ContentType parse(String s) {
            if (s == null || s.isEmpty()) return MOD;
            try {
                return ContentType.valueOf(s.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return MOD;
            }
        }
    }

    /**
     * Recommendation priority. Determines sort order and visual treatment in the
     * mod list screen.
     *
     * <p>Order from most-prominent to least-prominent:
     * <ol>
     *   <li>{@link #FEATURED} — gold border + star icon, pinned to top
     *   <li>{@link #CRITICAL} — red border + warning icon, "强烈推荐" label
     *   <li>{@link #NORMAL}   — default look (default if not specified)
     *   <li>{@link #OPTIONAL} — dim border, down-arrow icon, "可忽略" label
     * </ol>
     *
     * <p>Author guidance: keep FEATURED for 1-2 absolute must-haves; CRITICAL for
     * mods that fix serious bugs; OPTIONAL for nice-to-haves. Empty/no priority
     * defaults to NORMAL.
     */
    public enum Priority {
        @SerializedName("featured")  FEATURED,
        @SerializedName("critical")  CRITICAL,
        @SerializedName("normal")    NORMAL,
        @SerializedName("optional")  OPTIONAL;

        /**
         * Parse a string into a Priority, case-insensitive, lenient on unknown
         * values (returns NORMAL). The lenient fallback is important because
         * modpack authors will mistype the value at least once.
         */
        public static Priority parse(String s) {
            if (s == null || s.isEmpty()) return NORMAL;
            try {
                return Priority.valueOf(s.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return NORMAL;
            }
        }
    }

    /**
     * Sort key. Locked entries ({@code can_ignore=false}, must-install) always rank
     * above every priority — they are TISAM's topmost "get this now" tier. Among the
     * rest, order per 最新更新.md §4 is {@code critical} → {@code featured} →
     * {@code normal} → {@code optional}. This is NOT the enum ordinal order (FEATURED
     * < CRITICAL in the enum), so it is spelled out explicitly.
     */
    public static final Comparator<ModuleEntry> BY_PRIORITY = Comparator.comparingInt(m -> {
        if (!m.canIgnore) return 0;               // locked / must-install
        Priority p = m.priority == null ? Priority.NORMAL : m.priority;
        return switch (p) {
            case CRITICAL -> 1;
            case FEATURED -> 2;
            case NORMAL -> 3;
            case OPTIONAL -> 4;
        };
    });

    @SerializedName("id")
    public String id;

    @SerializedName("name")
    public String name;

    @SerializedName("description")
    public String description = "";

    @SerializedName("license")
    public String license = "ARR";

    @SerializedName("url")
    public String url;

    /**
     * Whether the player is allowed to permanently ignore this single mod.
     * {@code true} by default — most recommended mods are skippable.
     */
    @SerializedName("can_ignore")
    public boolean canIgnore = true;

    /**
     * Recommendation priority. Default = NORMAL.
     */
    @SerializedName("priority")
    public Priority priority = Priority.NORMAL;

    /**
     * Content type. Default = MOD so v1.0 configs without a {@code type}
     * field continue to work.
     */
    @SerializedName("type")
    public ContentType type = ContentType.MOD;

    /**
     * Per-entry override for the install guide text. If empty, TISAM falls
     * back to the type-specific default (see {@code TisamConstants.DEFAULT_INSTALL_GUIDE_BY_TYPE}).
     */
    @SerializedName("install_guide_text")
    public String installGuideText = "";

    public ModuleEntry() {}

    public ModuleEntry(String id, String name, String description, String license,
                       String url, boolean canIgnore) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.description = description == null ? "" : description;
        this.license = license == null ? "ARR" : license;
        this.url = Objects.requireNonNull(url, "url");
        this.canIgnore = canIgnore;
        this.priority = Priority.NORMAL;
        this.type = ContentType.MOD;
        this.installGuideText = "";
    }

    /** Compact string for log lines; never includes the URL (privacy-friendly). */
    @Override
    public String toString() {
        return "ModuleEntry{id='" + id + "', name='" + name + "', type=" + type
                + ", priority=" + priority + "}";
    }

    /**
     * Gson configured for this module's config files. Centralized here (not in
     * ConfigManager) so unit tests get the exact same instance without touching
     * ConfigManager, whose static init depends on the FML runtime.
     */
    public static Gson lenientGson() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .registerTypeAdapter(ModuleEntry.class, new Deserializer())
                .create();
    }

    /**
     * Lenient JSON deserializer: {@code type} and {@code priority} are routed
     * through the tolerant {@code parse()} methods instead of Gson's strict enum
     * binding. A modpack author typo like {@code "type": "datapackx"} used to
     * throw and quarantine the entire modules.json; now it degrades to
     * {@link ContentType#MOD MOD} / {@link Priority#NORMAL NORMAL} and the rest
     * of the file still loads.
     */
    public static final class Deserializer implements JsonDeserializer<ModuleEntry> {
        @Override
        public ModuleEntry deserialize(JsonElement json, Type typeOfT,
                                       com.google.gson.JsonDeserializationContext context) {
            JsonObject o = json.getAsJsonObject();
            ModuleEntry e = new ModuleEntry();
            e.id = str(o, "id");
            e.name = nvl(str(o, "name"), "");
            e.description = nvl(str(o, "description"), "");
            e.license = nvl(str(o, "license"), "ARR");
            e.url = str(o, "url");
            e.canIgnore = bool(o, "can_ignore", true);
            e.priority = Priority.parse(str(o, "priority"));
            e.type = ContentType.parse(str(o, "type"));
            e.installGuideText = nvl(str(o, "install_guide_text"), "");
            return e;
        }

        private static String str(JsonObject o, String key) {
            JsonElement el = o.get(key);
            return el == null || el.isJsonNull() ? null : el.getAsString();
        }

        private static String nvl(String s, String fallback) {
            return s == null ? fallback : s;
        }

        private static boolean bool(JsonObject o, String key, boolean fallback) {
            JsonElement el = o.get(key);
            return el == null || el.isJsonNull() ? fallback : el.getAsBoolean();
        }
    }
}
