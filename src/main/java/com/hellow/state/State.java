package com.hellow.state;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

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

    public State<T> observe(Consumer<T> listener) {
        this.listeners.add(listener);
        return this;
    }

    public int listenerCount() {
        return this.listeners.size();
    }
}
