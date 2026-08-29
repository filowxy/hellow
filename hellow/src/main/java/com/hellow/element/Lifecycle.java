package com.hellow.element;

public interface Lifecycle {
    default void onMount() {}
    default void onUnmount() {}
    default void tick() {}
}