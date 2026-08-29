package com.hellow.event;

import net.neoforged.neoforge.common.NeoForge;

/**
 * Posting helpers for Hellow's element events. Elements call these on the client game
 * thread; the events land on the NeoForge game bus where any mod can subscribe.
 *
 * <p>(ponytail: posting is a synchronous no-op when nothing subscribes, so the
 * call-path cost is negligible. Hellow's own catch-all bus listener also sees these
 * events, but no trigger rule matches them unless a caller registers one.)
 */
public final class ElementEvents {
    private ElementEvents() {}

    /** Post a {@link HellowElementPressedEvent} for a pressed element. No-op if {@code id} is null. */
    public static void postPressed(ElementId id, String elementType) {
        if (id == null) return;
        NeoForge.EVENT_BUS.post(new HellowElementPressedEvent(id, elementType));
    }

    /** Post a {@link HellowValueChangedEvent} for a changed control. No-op if {@code id} is null. */
    public static void postValueChanged(ElementId id, String value) {
        if (id == null) return;
        NeoForge.EVENT_BUS.post(new HellowValueChangedEvent(id, value));
    }
}
