package com.hellow.pages;

import net.neoforged.bus.api.Event;

public final class PageButtonActionEvent extends Event {

    private final String action;

    public PageButtonActionEvent(String action) {
        this.action = action == null ? "" : action;
    }

    public String action() {
        return this.action;
    }
}
