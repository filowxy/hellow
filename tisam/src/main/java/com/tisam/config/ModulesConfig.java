package com.tisam.config;

import com.google.gson.annotations.SerializedName;
import com.tisam.TisamConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-level schema for {@code config/tisam/modules.json}.
 *
 * <p>This is the file the modpack author ships. Gson tolerates missing fields;
 * every field has a sensible default so a partial JSON file still works.
 */
public final class ModulesConfig {
    /**
     * Config file format version (author-facing metadata). Currently not used to
     * gate behavior — always 1 so far — but keeping the field means Gson round-trips
     * it instead of silently dropping the key when TISAM rewrites the file.
     */
    @SerializedName("version")
    public int version = 1;

    @SerializedName("modules")
    public List<ModuleEntry> modules = new ArrayList<>();

    /**
     * Decay intervals (in launches) between reminders.
     * Default = [1,1,3,3,3,7,7,7,7,15] per 项目设计.md §二.
     */
    @SerializedName("reminder_frequency")
    public List<Integer> reminderFrequency = new ArrayList<>();

    @SerializedName("install_guide_text")
    public String installGuideText = TisamConstants.DEFAULT_INSTALL_GUIDE_TEXT;

    /**
     * Text shown on the disclaimer screen before permanent-skip takes effect.
     * Default = TisamConstants.DEFAULT_DISCLAIMER_TEXT.
     */
    @SerializedName("disclaimer_text")
    public String disclaimerText = TisamConstants.DEFAULT_DISCLAIMER_TEXT;

    /**
     * When {@code true}, TISAM forces the first prompt on every launch
     * (ignores the frequency decay and the permanentlySkipped flag).
     * Intended for modpack-author previews. Default = false.
     */
    @SerializedName("preview")
    public boolean preview = false;

    public ModulesConfig() {}

    /**
     * Returns the effective decay frequency list, falling back to the project
     * default if the JSON file specified an empty or null list.
     */
    public int[] effectiveFrequency() {
        if (reminderFrequency == null || reminderFrequency.isEmpty()) {
            return TisamConstants.DEFAULT_REMINDER_FREQUENCY.clone();
        }
        int[] out = new int[reminderFrequency.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = reminderFrequency.get(i);
        }
        return out;
    }
}
