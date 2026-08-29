package com.hellow.element;

public record ElementSpec(String tag, Object args) {
    public Element resolve() { return ElementRegistry.create(this); }
}