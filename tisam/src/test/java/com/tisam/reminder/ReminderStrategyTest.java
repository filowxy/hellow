package com.tisam.reminder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reminder decay behavior — the per-launch frequency table described in
 * 项目设计.md §二.
 *
 * <p>Default frequency: [1, 1, 3, 3, 3, 7, 7, 7, 7, 15]
 * <ul>
 *   <li>reminders 1-2  : every launch (interval 1)</li>
 *   <li>reminders 3-5  : every 3rd launch (interval 3)</li>
 *   <li>reminders 6-10 : every 7th launch (interval 7)</li>
 *   <li>reminders 11+  : every 15th launch (interval 15)</li>
 * </ul>
 */
class ReminderStrategyTest {
    private static final int[] DEFAULT_FREQ = {1, 1, 3, 3, 3, 7, 7, 7, 7, 15};

    @Test
    void firstReminderAlwaysShows() {
        assertTrue(ReminderStrategy.shouldRemind(0, 0, DEFAULT_FREQ),
                "First encounter should always show");
        assertTrue(ReminderStrategy.shouldRemind(0, 5, DEFAULT_FREQ),
                "reminderCount==0 ignores launchesSinceLast");
    }

    @Test
    void secondReminderFollowsDefault() {
        // After 1 reminder, interval is still 1 — next reminder fires 1 launch later.
        assertEquals(1, ReminderStrategy.nextInterval(1, DEFAULT_FREQ));
        // 0 launches since last reminder is NOT enough (need 1).
        assertTrue(!ReminderStrategy.shouldRemind(1, 0, DEFAULT_FREQ),
                "Should not remind immediately after the previous reminder");
        // 1 launch since last reminder is exactly enough.
        assertTrue(ReminderStrategy.shouldRemind(1, 1, DEFAULT_FREQ));
    }

    @Test
    void decaysToIntervalThreeAfterTwoReminders() {
        // reminderCount=2 means the 3rd reminder is due; interval should be 3.
        assertEquals(3, ReminderStrategy.nextInterval(2, DEFAULT_FREQ));
        // Player has waited 2 launches since the last reminder — not enough.
        assertTrue(!ReminderStrategy.shouldRemind(2, 2, DEFAULT_FREQ),
                "Should not remind yet — 2 launches < interval 3");
        // Player has waited 3 launches — due now.
        assertTrue(ReminderStrategy.shouldRemind(2, 3, DEFAULT_FREQ));
    }

    @Test
    void decaysToIntervalSevenAfterFiveReminders() {
        assertEquals(7, ReminderStrategy.nextInterval(5, DEFAULT_FREQ));
    }

    @Test
    void decaysToIntervalFifteenAfterTenReminders() {
        assertEquals(15, ReminderStrategy.nextInterval(10, DEFAULT_FREQ));
        // Beyond the array, the last element is reused.
        assertEquals(15, ReminderStrategy.nextInterval(99, DEFAULT_FREQ));
    }

    @Test
    void zeroIntervalIsClampedToOne() {
        // ponytail: a misconfigured frequency array must not lock the player into a
        // permanent-spam loop. 0 → 1.
        assertEquals(1, ReminderStrategy.nextInterval(0, new int[]{0, 0, 0}));
    }

    @Test
    void emptyFrequencyFallsBackToOne() {
        assertEquals(1, ReminderStrategy.nextInterval(5, new int[0]));
        assertEquals(1, ReminderStrategy.nextInterval(5, null));
    }
}
