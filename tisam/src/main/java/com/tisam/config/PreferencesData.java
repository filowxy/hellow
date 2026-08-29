package com.tisam.config;

import com.google.gson.annotations.SerializedName;

/**
 * Per-player preferences persisted at {@code config/tisam/preferences.json}.
 *
 * <p>TISAM is the sole writer of this file. Missing fields fall back to safe
 * defaults so a hand-edited partial file still works.
 */
public final class PreferencesData {
    /**
     * {@code true} once the player has confirmed the permanent-skip disclaimer.
     * When set, TISAM never shows the first prompt again (unless {@code preview=true}).
     */
    @SerializedName("permanently_skipped")
    public boolean permanentlySkipped = false;

    /**
     * Total number of reminders the player has seen so far.
     * Drives the frequency-decay strategy; see {@link com.tisam.reminder.ReminderStrategy}.
     */
    @SerializedName("reminder_count")
    public int reminderCount = 0;

    /**
     * ISO date string of the most recent reminder. Informational only —
     * the decay logic does NOT depend on calendar time, only on launch count.
     */
    @SerializedName("last_reminder_date")
    public String lastReminderDate = "";

    /**
     * Game launches since the last reminder was shown. Together with
     * {@link #reminderCount} it drives the frequency-decay strategy (see
     * {@link com.tisam.reminder.ReminderStrategy}) — this is the count that
     * actually accumulates between reminders.
     */
    @SerializedName("launches_since_reminder")
    public int launchesSinceReminder = 0;

    public PreferencesData() {}
}
