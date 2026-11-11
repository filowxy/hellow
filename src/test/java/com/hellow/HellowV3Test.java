package com.hellow;

import com.hellow.element.FlexElement;
import com.hellow.element.SpacerElement;
import com.hellow.event.ElementId;
import com.hellow.state.State;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HellowV3Test {

    @Test
    void stateHoldsAndUpdatesValue() {
        State<String> s = State.of("a");
        assertEquals("a", s.get());
        s.set("b");
        assertEquals("b", s.get());
    }

    @Test
    void stateNotifiesOnlyOnChange() {
        State<Integer> s = State.of(1);
        AtomicInteger notified = new AtomicInteger();
        s.observe(v -> notified.incrementAndGet());
        s.set(1);
        assertEquals(0, notified.get());
        s.set(2);
        assertEquals(1, notified.get());
    }

    @Test
    void elementIdParsesCompoundAndBare() {
        ElementId compound = ElementId.of("page", "btn");
        assertEquals("page", compound.pageId());
        assertEquals("btn", compound.elementId());
        assertEquals("page:btn", compound.toString());

        ElementId bare = ElementId.of("just-an-id");
        assertEquals("", bare.pageId());
        assertEquals("just-an-id", bare.elementId());
    }

    @Test
    void flexRejectsNegativeWeight() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlexElement(new SpacerElement(0), -1));
    }

    @Test
    void flexKeepsChildAndWeight() {
        FlexElement flex = new FlexElement(new SpacerElement(0), 2);
        assertEquals(2, flex.weight());
        assertInstanceOf(SpacerElement.class, flex.child());
    }
}
