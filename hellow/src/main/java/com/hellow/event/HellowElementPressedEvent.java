package com.hellow.event;

import net.neoforged.bus.api.Event;

/**
 * Posted on the NeoForge game bus when an interactive element (e.g. a
 * {@link com.hellow.element.ButtonElement}) is pressed.
 *
 * <p>Any mod can subscribe with {@code @SubscribeEvent} on the game bus and react to
 * Hellow UI without depending on the UI code itself. The event carries the element
 * identity plus the element's type name (e.g. {@code "button"}).
 */
public class HellowElementPressedEvent extends Event {
    private final ElementId id;
    private final String elementType;

    public HellowElementPressedEvent(ElementId id, String elementType) {
        this.id = id;
        this.elementType = elementType == null ? "" : elementType;
    }

    /** The page + element identity of the pressed element. */
    public ElementId id() { return this.id; }

    public String pageId() { return this.id.pageId(); }

    public String elementId() { return this.id.elementId(); }

    /** Element type name, e.g. {@code "button"} or {@code "link"}. */
    public String elementType() { return this.elementType; }

    @Override
    public String toString() {
        return "HellowElementPressedEvent[" + this.id + ", type=" + this.elementType + "]";
    }
}
