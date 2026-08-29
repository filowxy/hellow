package com.tisam;

import com.tisam.config.ModuleEntry.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

/**
 * TISAM — global constants.
 *
 * <p>Per the project's Ponytail senior-dev rule: <b>no magic numbers or strings</b>.
 * Every literal that has semantic meaning lives here so it can be tuned in one place.
 */
public final class TisamConstants {
    private TisamConstants() {}

    /** Mod ID — must match {@code ModConfig.MOD_ID} in {@code build.gradle.kts}. */
    public static final String MOD_ID = "tisam";

    /** Human-readable display name. */
    public static final String MOD_NAME = "There Is Still A Mod";

    /** SLF4J logger. The leading prefix makes log lines trivial to grep. */
    public static final Logger LOG = LoggerFactory.getLogger("tisam");

    // -------- Config files --------

    /** Directory under game root where TISAM stores its config + prefs. */
    public static final String CONFIG_DIR = "tisam";

    /** Module recommendation list — modpack authors ship this. */
    public static final String CONFIG_MODULES_FILE = "modules.json";

    /** Per-player preferences — TISAM writes this. */
    public static final String CONFIG_PREFERENCES_FILE = "preferences.json";

    // -------- Default values (used when JSON files are missing fields) --------

    /** Default decay intervals: 1,1,3,3,3,7,7,7,7,15 launches between reminders. */
    public static final int[] DEFAULT_REMINDER_FREQUENCY = {1, 1, 3, 3, 3, 7, 7, 7, 7, 15};

    /** Default install-guide text shown in the third UI screen. */
    public static final String DEFAULT_INSTALL_GUIDE_TEXT =
            "1. Click the mod link above to open the official download page\n"
          + "2. Download the .jar file\n"
          + "3. Place the file into the .minecraft/mods folder\n"
          + "4. Restart the game";

    /** Default disclaimer text shown before permanent-skip takes effect. */
    public static final String DEFAULT_DISCLAIMER_TEXT =
            "Confirming will permanently silence TISAM. "
          + "You will no longer see any prompts about optional mods.\n\n"
          + "To re-enable reminders later, delete config/tisam/preferences.json "
          + "or set permanently_skipped to false.";

    /**
     * Per-content-type default install guide. v2.0 distinguishes four
     * content types, each with its own copy-paste instructions. Authors
     * may override per-entry via {@code ModuleEntry.installGuideText} or
     * globally via {@code ModulesConfig.installGuideText} (the latter is
     * kept for backward compatibility with v1.0 author configs).
     */
    public static final Map<ContentType, String> DEFAULT_INSTALL_GUIDE_BY_TYPE;
    static {
        EnumMap<ContentType, String> m = new EnumMap<>(ContentType.class);
        m.put(ContentType.MOD, DEFAULT_INSTALL_GUIDE_TEXT);
        m.put(ContentType.RESOURCEPACK,
                "1. Click the resource pack link above to open the official download page\n"
              + "2. Download the .zip file\n"
              + "3. Place the file into the .minecraft/resourcepacks folder\n"
              + "4. In-game, open Options → Resource Packs and enable it");
        m.put(ContentType.SHADERPACK,
                "1. Click the shader pack link above to open the official download page\n"
              + "2. Download the .zip file\n"
              + "3. Place the file into the .minecraft/shaderpacks folder\n"
              + "4. In-game, open Options → Video Settings → Shaders and pick it\n"
              + "Note: the modpack must already have Iris or OptiFine installed.");
        m.put(ContentType.DATAPACK,
                "1. Click the data pack link above to open the official download page\n"
              + "2. Download the .zip file\n"
              + "3. Place the file into the .minecraft/datapacks folder (global)\n"
              + "4. Reload the world; the modpack's Global Packs setup applies it automatically");
        DEFAULT_INSTALL_GUIDE_BY_TYPE = java.util.Collections.unmodifiableMap(m);
    }

    // -------- UI constants --------

    /** Cooldown (in seconds) before the disclaimer "Confirm" button activates. */
    public static final int DISCLAIMER_COOLDOWN_SECONDS = 5;

    /** Default screen background dim used for modals. */
    public static final int MODAL_BACKGROUND_COLOR = 0xCC000010;

    /** Standard in-game GUI scale denominator (240px is Minecraft's design baseline). */
    public static final int GUI_BASE_WIDTH = 240;
    public static final int GUI_BASE_HEIGHT = 160;

    // -------- v2.0 external-link safety --------

    /**
     * Maximum URL length (in characters) for which the external-link
     * confirmation modal exposes an "Open" button. URLs longer than this
     * only expose "Copy URL" and "Cancel" — see 最新更新.md §4 / §9.
     * Sixty characters fits the typical
     * {@code https://modrinth.com/mod/<slug>} pattern comfortably while
     * still flagging suspicious deep links that try to obfuscate the
     * target host.
     */
    public static final int EXTERNAL_LINK_URL_LIMIT = 60;

    // -------- TISAM UI palette (content colors; the modal chrome itself
    // -------- comes from HellowTheme.DEFAULT so it matches the dialogs
    // -------- TISAM always drew) --------

    /** Card background for normal-priority entries. */
    public static final int CARD_BG_NORMAL = 0xFF2A2A2A;
    /** Card border for normal-priority entries. */
    public static final int CARD_BORDER_NORMAL = 0xFF888888;
    /** Card background for featured entries. */
    public static final int CARD_BG_FEATURED = 0xFF3F3A1E;
    /** Card border for featured entries. */
    public static final int CARD_BORDER_FEATURED = 0xFFFFD700;
    /** Card background for critical entries. */
    public static final int CARD_BG_CRITICAL = 0xFF3F1E1E;
    /** Card border for critical entries. */
    public static final int CARD_BORDER_CRITICAL = 0xFFFF6655;
    /** Card background for locked (can_ignore=false) must-install entries. */
    public static final int CARD_BG_LOCKED = 0xFF241A3A;
    /** Card border for locked (must-install) entries — purple, topmost rank. */
    public static final int CARD_BORDER_LOCKED = 0xFFB08CFF;

    /** Card entry name text. */
    public static final int CARD_NAME_COLOR = 0xFFFFFFFF;
    /** Card entry description text (truncated). */
    public static final int CARD_DESC_COLOR = 0xFFAAAAAA;
    /** Card content-type label text. */
    public static final int CARD_TYPE_COLOR = 0xFF888888;

    /** URL link icon, idle. */
    public static final int URL_ICON_COLOR = 0xFF4488AA;
    /** URL link icon, hovered. */
    public static final int URL_ICON_HOVER = 0xFF55CCFF;

    /** First-prompt "N mods found" hint when the pack has featured/critical content. */
    public static final int PROMPT_HINT_HIGHLIGHT = 0xFFFFCC55;
    /** First-prompt "N mods found" hint otherwise. */
    public static final int PROMPT_HINT_NORMAL = 0xFF88CC88;
    /** Secondary/muted text (ESC hint, secondary body line). */
    public static final int TEXT_MUTED = 0xFFAAAAAA;
    /** Guide panel body text. */
    public static final int GUIDE_BODY_COLOR = 0xFFCCCCCC;

    /** Disclaimer accent — orange warning keeps the pre-migration look. */
    public static final int DISCLAIMER_WARN_COLOR = 0xFFFF8866;
    /** Disclaimer body text. */
    public static final int DISCLAIMER_TEXT_COLOR = 0xFFFFFFFF;
}
