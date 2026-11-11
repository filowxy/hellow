package com.hellow.page;

import com.hellow.element.Element;
import com.hellow.style.HellowTheme;
import com.hellow.trigger.Condition;
import com.hellow.trigger.FireStrategy;

public record PageSpec(
        PageId id,
        String titleKey,
        PageLayer layer,
        Condition condition,
        FireStrategy fireStrategy,
        Element body,
        HellowTheme theme,
        WindowSpec window) implements Page {

    public PageSpec(PageId id, String titleKey, PageLayer layer, Condition condition,
                    FireStrategy fireStrategy, Element body) {
        this(id, titleKey, layer, condition, fireStrategy, body, null, WindowSpec.DEFAULT);
    }

    public PageSpec(PageId id, String titleKey, PageLayer layer, Condition condition,
                    FireStrategy fireStrategy, Element body, HellowTheme theme) {
        this(id, titleKey, layer, condition, fireStrategy, body, theme, WindowSpec.DEFAULT);
    }
}
