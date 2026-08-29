package com.tisam.reminder;

/**
 * Decides when to show the first-prompt screen based on the player's reminder
 * count and the modpack author's configured frequency array.
 *
 * <p>Per design document §二 (项目设计.md):
 * <pre>
 *   reminders 1-2  : every launch         (interval 1)
 *   reminders 3-5  : every 3rd launch     (interval 3)
 *   reminders 6-10 : every 7th launch     (interval 7)
 *   reminders 11+  : every 15th launch    (interval 15)
 * </pre>
 *
 * <p>The frequency array is interpreted as a lookup table by index:
 * the {@code i}-th element is the interval (in launches) the player must
 * wait AFTER the {@code i}-th reminder before being shown the next one.
 * If {@code i} is past the array end, the last element is reused.
 */
public final class ReminderStrategy {
    private ReminderStrategy() {}

    /**
     * Returns the interval (in launches) the player must wait before the
     * next reminder, given the number of reminders already shown.
     *
     * @param reminderCount        how many reminders the player has seen.
     * @param frequency            the modpack author's decay array.
     * @return                     launches to wait before the next reminder.
     *                             Always at least 1 — a frequency entry of 0
     *                             would loop forever and is clamped defensively.
     */
    public static int nextInterval(int reminderCount, int[] frequency) {
        if (frequency == null || frequency.length == 0) {
            return 1; // ponytail: defensive — never let a misconfigured array spam the player
        }
        int idx = Math.min(reminderCount, frequency.length - 1);
        int value = frequency[idx];
        return value < 1 ? 1 : value;
    }

    /**
     * Convenience for the in-game check: should the prompt appear on this launch,
     * given the player's current reminder count and the launches since the last
     * reminder?
     *
     * <p>The first reminder ({@code reminderCount == 0}) always shows; this is
     * the player's first encounter with TISAM.
     *
     * @param reminderCount     reminders already shown.
     * @param launchesSinceLast how many game launches have happened since the
     *                          last reminder. Pass 0 if this is the first
     *                          launch after a reminder.
     * @param frequency         the decay array (see {@link #nextInterval}).
     */
    public static boolean shouldRemind(int reminderCount, int launchesSinceLast, int[] frequency) {
        if (reminderCount <= 0) return true;
        return launchesSinceLast >= nextInterval(reminderCount, frequency);
    }
}
