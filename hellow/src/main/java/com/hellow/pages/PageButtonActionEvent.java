package com.hellow.pages;

import net.neoforged.bus.api.Event;

/**
 * Posted on the NeoForge game bus when a file-driven page's button fires with an
 * {@code onPress} that is {@code emit:<name>} (or a bare name). Embedding mods can
 * subscribe to act on declarative buttons without Java page code.
 */
public final class PageButtonActionEvent extends Event {

    private final String action;

    public PageButtonActionEvent(String action) {
        this.action = action == null ? "" : action;
    }

    /** The emitted action name (after the {@code emit:} prefix). */
    public String action() {
        return this.action;
    }
}