package com.hellow.debug;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Bounded, thread-safe sink for UI errors. Hellow catches render/event exceptions
 * at its dispatch boundaries and records them here; the game keeps running instead
 * of hard-crashing, and the author can inspect or export the report.
 */
public final class ErrorCollector {
    /** Upper bound — a runaway exception loop must not grow memory forever. */
    private static final int CAPACITY = 64;

    private static final ErrorCollector INSTANCE = new ErrorCollector();

    public static ErrorCollector get() { return INSTANCE; }

    private final List<DebugEntry> entries = new ArrayList<>();

    private ErrorCollector() {}

    /**
     * Record an error. {@code origin} is the screen/element that was processing
     * the event; {@code phase} is a short label like "render" or "mouseClicked".
     */
    public synchronized void capture(String phase, Throwable t, Object origin) {
        if (t == null) return;
        DebugEntry entry = new DebugEntry(
                System.currentTimeMillis(),
                origin == null ? null : origin.getClass().getSimpleName(),
                phase == null ? "" : phase,
                t.getClass().getName(),
                t.getMessage(),
                stackText(t));
        if (entries.size() >= CAPACITY) entries.remove(0);
        entries.add(entry);
    }

    /** Snapshot of all captured entries (newest last). */
    public synchronized List<DebugEntry> entries() {
        return new ArrayList<>(entries);
    }

    public synchronized int count() { return entries.size(); }

    /** Drop every captured entry. */
    public synchronized void clear() { entries.clear(); }

    /**
     * Render the whole report as a text block. Pure text (no Minecraft types),
     * so it can be unit-tested on the plain JUnit classpath.
     */
    public synchronized String exportText() {
        StringBuilder sb = new StringBuilder(512);
        sb.append("Hellow debug report (").append(java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append(")\n");
        sb.append("entries: ").append(entries.size()).append('\n');
        sb.append("========================================\n");
        int i = 1;
        for (DebugEntry e : entries) {
            sb.append('\n').append('[').append(i++).append("] ").append(e.headLine()).append('\n');
            sb.append("  at: ").append(e.timeMillis()).append("  screen: ")
              .append(e.screenClass() == null ? "?" : e.screenClass()).append('\n');
            sb.append(e.stack());
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String stackText(Throwable t) {
        StringWriter sw = new StringWriter(512);
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}