package com.hellow.page;

import com.hellow.element.Element;

import java.util.Objects;

/**
 * Fluent builder for {@link PageSpec} — the declarative alternative to constructing
 * the record directly.
 *
 * <pre>{@code
 * Page page = PageBuilder.of("my_page")
 *         .title("my_mod.page.my_page")
 *         .layer(PageLayer.MODAL)
 *         .body(Elements.column(6, Elements.text("Hi!")))
 *         .build();
 * }</pre>
 *
 * <p>Conditional/auto-opening pages (with a {@link com.hellow.trigger.Condition} and a
 * {@link com.hellow.trigger.FireStrategy}) are not supported here on purpose — the
 * event-driven way to open pages is the {@code Hellow.when...} family, which keeps the
 * trigger and the content separate.
 */
public final class PageBuilder {
    private final PageId id;
    private String titleKey;
    private PageLayer layer = PageLayer.NORMAL;
    private Element body;

    private PageBuilder(PageId id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public static PageBuilder of(String id) {
        return new PageBuilder(PageId.of(id));
    }

    public static PageBuilder of(PageId id) {
        return new PageBuilder(id);
    }

    /** Translation key (or literal text) for the page title. */
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

    public Page build() {
        return new PageSpec(this.id, this.titleKey, this.layer, null, null, this.body);
    }
}
