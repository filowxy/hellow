package com.hellow.debug;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runnable checks for the bounded error collector and its report text.
 */
class ErrorCollectorTest {

    /** Stand-in origin object so the captured screen name is deterministic. */
    private static final class Origin {}

    private static RuntimeException boom() {
        return new RuntimeException("boom");
    }

    @Test
    void captureAddsEntry() {
        ErrorCollector.get().clear();
        ErrorCollector.get().capture("render", boom(), new Origin());
        assertEquals(1, ErrorCollector.get().count());
        DebugEntry e = ErrorCollector.get().entries().get(0);
        assertEquals("render", e.phase());
        assertEquals(Origin.class.getSimpleName(), e.screenClass());
        assertEquals(RuntimeException.class.getName(), e.errorClass());
        assertEquals("boom", e.message());
        assertNotNull(e.stack());
        assertFalse(e.stack().isEmpty());
    }

    @Test
    void nullThrowableIgnored() {
        ErrorCollector.get().clear();
        ErrorCollector.get().capture("render", null, this);
        assertEquals(0, ErrorCollector.get().count());
    }

    @Test
    void boundedCapacityEvictsOldest() {
        ErrorCollector.get().clear();
        for (int i = 0; i < 100; i++) {
            ErrorCollector.get().capture("tick", new IllegalArgumentException("n" + i), this);
        }
        assertEquals(64, ErrorCollector.get().count());
        // Oldest entries were evicted — the head line should be from a later one.
        DebugEntry first = ErrorCollector.get().entries().get(0);
        assertFalse(first.message().equals("n0"));
    }

    @Test
    void clearDropsEverything() {
        ErrorCollector.get().clear();
        ErrorCollector.get().capture("render", boom(), this);
        ErrorCollector.get().clear();
        assertEquals(0, ErrorCollector.get().count());
    }

    @Test
    void exportTextContainsEntries() {
        ErrorCollector.get().clear();
        ErrorCollector.get().capture("mouseClicked", boom(), this);
        String text = ErrorCollector.get().exportText();
        assertTrue(text.contains("boom"));
        assertTrue(text.contains(RuntimeException.class.getName()));
    }

    @Test
    void exportTextEmptyWhenNoEntries() {
        ErrorCollector.get().clear();
        String text = ErrorCollector.get().exportText();
        assertTrue(text.contains("entries: 0"));
    }

    @Test
    void entriesSnapshotIsCopy() {
        ErrorCollector.get().clear();
        ErrorCollector.get().capture("a", boom(), this);
        List<DebugEntry> snapshot = ErrorCollector.get().entries();
        ErrorCollector.get().clear();
        assertEquals(0, ErrorCollector.get().count());
        assertEquals(1, snapshot.size());
    }
}