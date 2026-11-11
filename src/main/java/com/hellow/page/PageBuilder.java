package com.hellow.page;

import com.hellow.element.Element;
import com.hellow.style.HellowTheme;

import java.util.Objects;

public final class PageBuilder {
    private final PageId id;
    private String titleKey;
    private PageLayer layer = PageLayer.NORMAL;
    private Element body;
    private HellowTheme theme;
    private WindowSpec window = WindowSpec.DEFAULT;

    private PageBuilder(PageId id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public static PageBuilder of(String id) {
        return new PageBuilder(PageId.of(id));
    }

    public static PageBuilder of(PageId id) {
        return new PageBuilder(id);
    }

    public PageBuilder title(String titleKey) {
        this.titleKey = titleKey;
        return this;
    }

    public PageBuilder layer(PageLayer layer) {
        this.layer = layer == null ? PageLayer.NORMAL : layer;
        return this;
    }

    public PageBuilder body(Element body) {
        this.body = body;
        return this;
    }

    public PageBuilder theme(HellowTheme theme) {
        this.theme = theme;
        return this;
    }

    /** Window geometry; {@code null} keeps {@link WindowSpec#DEFAULT}. */
    public PageBuilder window(WindowSpec window) {
        this.window = window == null ? WindowSpec.DEFAULT : window;
        return this;
    }

    public Page build() {
        return new PageSpec(this.id, this.titleKey, this.layer, null, null, this.body, this.theme, this.window);
    }
}
