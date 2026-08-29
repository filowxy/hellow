package com.hellow.page;

import com.hellow.element.Element;
import com.hellow.trigger.Condition;
import com.hellow.trigger.FireStrategy;

public record PageSpec(
        PageId id,
        String titleKey,
        PageLayer layer,
        Condition condition,
        FireStrategy fireStrategy,
        Element body) implements Page {
}
