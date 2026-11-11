package com.hellow;

import com.hellow.page.Page;
import com.hellow.page.HellowRegistry;

public final class HellowPages {
    private HellowPages() {}

    public static void register(Page page) { HellowRegistry.register(page); }

    public static java.util.List<Page> registered() { return HellowRegistry.all(); }
}
