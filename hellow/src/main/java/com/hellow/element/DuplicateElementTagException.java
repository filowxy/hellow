package com.hellow.element;

public final class DuplicateElementTagException extends RuntimeException {
    public DuplicateElementTagException(String tag) { super("duplicate element tag: " + tag); }
}