package com.hellow.page;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

public final class PageStack {
    private final Deque<Page> stack = new ArrayDeque<>();

    public void push(Page page) {
        this.stack.push(page);
    }

    public void pop() {
        if (!this.stack.isEmpty()) this.stack.pop();
    }

    public void replaceTop(Page page) {
        this.pop();
        this.push(page);
    }

    public Optional<Page> top() {
        return this.stack.isEmpty() ? Optional.empty() : Optional.of(this.stack.peek());
    }

    public List<Page> snapshot() {
        return new ArrayList<>(this.stack);
    }

    public boolean isEmpty() {
        return this.stack.isEmpty();
    }

    public void clear() {
        this.stack.clear();
    }
}
