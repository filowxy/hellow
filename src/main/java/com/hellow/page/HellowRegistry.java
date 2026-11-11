package com.hellow.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HellowRegistry {
    private static final Map<String, Page> PAGES = new ConcurrentHashMap<>();

    private HellowRegistry() {}

    public static void register(Page page) { PAGES.put(page.id().value(), page); }

    public static List<Page> all() { return new ArrayList<>(PAGES.values()); }

    public static Page get(PageId id) { return PAGES.get(id.value()); }

    public static void clear() { PAGES.clear(); }
}
