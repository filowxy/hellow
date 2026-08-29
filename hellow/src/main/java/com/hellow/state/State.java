package com.hellow.state;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A tiny observable value holder that turns Hellow's form controls into controlled
 * components: build a form backed by {@code State}s, let the user edit it, then read
 * {@link #get()} on submit.
 *
 * <p>Pure logic (no Minecraft classes) so it is unit-testable. Only notifies
 * listeners on actual value change.
 *
 * @param <T> the value type
 */
public final class State<T> {
    private T value;
    private final List<Consumer<T>> listeners = new CopyOnWriteArrayList<>();

    public State(T value) {
        this.value = value;
    }

    public static <T> State<T> of(T value) {
        return new State<>(value);
    }

    public T get() {
        return this.value;
    }

    public void set(T value) {
        if (Objects.equals(this.value, value)) return;
        this.value = value;
        for (Consumer<T> listener : this.listeners) listener.accept(value);
    }

    /** Register a change listener. Returns {@code this} for chaining. */
    public State<T> observe(Consumer<T> listener) {
        this.listeners.add(listener);
        return this;
    }

    /** Number of registered listeners (test helper). */
    public int listenerCount() {
        return this.listeners.size();
    }
}
