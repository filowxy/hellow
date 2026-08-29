package com.hellow.event;

import net.neoforged.bus.api.Event;

/**
 * Posted on the NeoForge game bus when a value-bearing control
 * ({@link com.hellow.element.ToggleElement}, {@link com.hellow.element.SliderElement},
 * {@link com.hellow.element.TextInputElement}, {@link com.hellow.element.EnumElement})
 * changes value.
 *
 * <p>The new value is carried as a string; callers parse it back to their own type.
 */
public class HellowValueChangedEvent extends Event {
    private final ElementId id;
    private final String value;

    public HellowValueChangedEvent(ElementId id, String value) {
        this.id = id;
        this.value = value == null ? "" : value;
    }

    /** The page + element identity of the control that changed. */
    public ElementId id() { return this.id; }

    public String pageId() { return this.id.pageId(); }

    public String elementId() { return this.id.elementId(); }

    /** New value, as a string. */
    public String value() { return this.value; }

    @Override
    public String toString() {
        return "HellowValueChangedEvent[" + this.id + ", value=" + this.value + "]";
    }
}
