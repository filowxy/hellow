package com.hellow.debug;

public record DebugEntry(long timeMillis, String screenClass, String phase,
                         String errorClass, String message, String stack) {

    public String headLine() {
        return "[" + phase + "] " + errorClass
                + (message == null || message.isEmpty() ? "" : ": " + message);
    }
}
