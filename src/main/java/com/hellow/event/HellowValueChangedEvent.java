package com.hellow.event;

import net.neoforged.bus.api.Event;

public class HellowValueChangedEvent extends Event {
    private final ElementId id;
    private final String value;

    public HellowValueChangedEvent(ElementId id, String value) {
        this.id = id;
        this.value = value == null ? "" : value;
    }

    public ElementId id() { return this.id; }
    public String pageId() { return this.id.pageId(); }
    public String elementId() { return this.id.elementId(); }
    public String value() { return this.value; }

    @Override
    public String toString() {
        return "HellowValueChangedEvent[" + this.id + ", value=" + this.value + "]";
    }
}
