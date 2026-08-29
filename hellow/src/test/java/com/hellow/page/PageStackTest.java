package com.hellow.page;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PageStackTest {

    @Test
    void pushAndPopMaintainsOrder() {
        PageStack stack = new PageStack();
        Page a = new PageSpec(PageId.of("a"), "A", PageLayer.NORMAL, null, null, null);
        Page b = new PageSpec(PageId.of("b"), "B", PageLayer.MODAL, null, null, null);

        stack.push(a);
        stack.push(b);

        assertTrue(stack.top().isPresent());
        assertEquals("b", stack.top().get().id().value());

        stack.pop();
        assertEquals("a", stack.top().get().id().value());

        stack.pop();
        assertTrue(stack.isEmpty());
    }

    @Test
    void replaceTopSwapsOnlyTop() {
        PageStack stack = new PageStack();
        Page a = new PageSpec(PageId.of("a"), "A", PageLayer.NORMAL, null, null, null);
        Page b = new PageSpec(PageId.of("b"), "B", PageLayer.MODAL, null, null, null);

        stack.push(a);
        stack.replaceTop(b);

        assertEquals("b", stack.top().get().id().value());
        stack.pop();
        assertTrue(stack.isEmpty());
    }
}
