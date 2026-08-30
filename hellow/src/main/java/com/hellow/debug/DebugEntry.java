package com.hellow.debug;

/**
 * One captured UI error.
 *
 * @param timeMillis   when the error happened (wall clock)
 * @param screenClass  simple name of the screen that was rendering/processing, if known
 * @param phase        where it blew up ("render", "mouseClicked", ...)
 * @param errorClass   throwable class name
 * @param message      throwable message (may be null)
 * @param stack        captured stack trace as text
 */
public record DebugEntry(long timeMillis, String screenClass, String phase,
                         String errorClass, String message, String stack) {

    /** One-line summary suitable for a list row. */
    public String headLine() {
        return "[" + phase + "] " + errorClass
                + (message == null || message.isEmpty() ? "" : ": " + message);
    }
}