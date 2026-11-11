package com.hellow.event;

import net.neoforged.bus.api.Event;

public class HellowElementPressedEvent extends Event {
    private final ElementId id;
    private final String elementType;

    public HellowElementPressedEvent(ElementId id, String elementType) {
        this.id = id;
        this.elementType = elementType == null ? "" : elementType;
    }

    public ElementId id() { return this.id; }
    public String pageId() { return this.id.pageId(); }
    public String elementId() { return this.id.elementId(); }
    public String elementType() { return this.elementType; }

    @Override
    public String toString() {
        return "HellowElementPressedEvent[" + this.id + ", type=" + this.elementType + "]";
    }
}
