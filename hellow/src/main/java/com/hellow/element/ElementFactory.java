package com.hellow.element;

@FunctionalInterface
public interface ElementFactory {
    Element create(ElementSpec spec);
}