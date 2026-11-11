package com.hellow.event;

import net.neoforged.neoforge.common.NeoForge;

public final class ElementEvents {
    private ElementEvents() {}

    public static void postPressed(ElementId id, String elementType) {
        if (id == null) return;
        NeoForge.EVENT_BUS.post(new HellowElementPressedEvent(id, elementType));
    }

    public static void postValueChanged(ElementId id, String value) {
        if (id == null) return;
        NeoForge.EVENT_BUS.post(new HellowValueChangedEvent(id, value));
    }
}
