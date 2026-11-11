package com.hellow.action;

import com.hellow.page.HellowRegistry;
import com.hellow.page.Page;
import com.hellow.page.PageId;
import com.hellow.page.PageScheduler;
import java.util.function.Consumer;

public final class Actions {
    private Actions() {}

    public static Action navigate(String targetPageId) {
        return () -> {
            Page target = HellowRegistry.get(PageId.of(targetPageId));
            if (target != null) PageScheduler.show(target);
        };
    }

    public static Action close() {
        return PageScheduler::closeTop;
    }

    public static Action message(Consumer<String> sink, String message) {
        return () -> sink.accept(message);
    }
}
