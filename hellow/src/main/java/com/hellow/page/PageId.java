package com.hellow.page;

public record PageId(String value) {
    public static PageId of(String value) {
        return new PageId(value);
    }
}
