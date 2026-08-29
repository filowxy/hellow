package com.hellow.element;

import com.hellow.action.Action;
import com.hellow.event.ElementId;
import com.hellow.layout.ContentAlign;
import com.hellow.layout.Spacing;
import com.hellow.state.State;
import com.hellow.style.HellowTheme;
import com.hellow.text.StringMode;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Fluent static factories for Hellow's {@link Element} types.
 *
 * <p>This is the "easy mode" of the element system: instead of constructing
 * {@code new ColumnElement(...)} by hand, callers compose trees like
 *
 * <pre>{@code
 * Elements.column(6,
 *     Elements.text("Hello, world!"),
 *     Elements.button("OK", () -> Hellow.closeTop()),
 *     Elements.link("Open docs", "https://example.com"));
 * }</pre>
 *
 * <p>All layout widgets ({@link ColumnElement}, {@link RowElement}, ...) can be nested
 * freely. Callers who need full control over a widget's constructor can still use the
 * element classes directly.
 */
public final class Elements {
    private Elements() {}

    // Theme-derived factory defaults (see {@link HellowTheme} for the single color source).
    private static final int DEFAULT_TEXT_COLOR = HellowTheme.DEFAULT.textColor();
    private static final int DEFAULT_LINK_COLOR = HellowTheme.DEFAULT.linkColor();
    private static final int DEFAULT_BORDER_COLOR = HellowTheme.DEFAULT.boxBorder();

    // -------- Leaf widgets --------

    public static Element text(String text) {
        return new TextElement(Component.literal(text), DEFAULT_TEXT_COLOR);
    }

    public static Element text(String text, int color) {
        return new TextElement(Component.literal(text), color);
    }

    public static Element text(Component text, int color) {
        return new TextElement(text, color);
    }

    /** Auto-wrapping multi-line text. */
    public static Element wrappedText(String text, int color) {
        return wrappedText(Component.literal(text), color);
    }

    /** Auto-wrapping multi-line text (Component form). */
    public static Element wrappedText(Component text, int color) {
        return new TextElement(text, color, true);
    }

    /** Single-line text, ellipsis-clamped when wider than the available space. */
    public static Element clampedText(String text, int color) {
        return new TextElement(Component.literal(text), color, StringMode.CLAMPED);
    }

    /** Single-line text, ellipsis-clamped when wider than the available space. */
    public static Element clampedText(Component text, int color) {
        return new TextElement(text, color, StringMode.CLAMPED);
    }

    public static Element button(String label, Action action) {
        return new ButtonElement(label, action);
    }

    /** Button with an initial enabled/disabled state (greyed when disabled). */
    public static Element button(String label, Action action, boolean enabled) {
        return new ButtonElement(label, action, null, null, HellowTheme.DEFAULT, enabled);
    }

    /** Themed variant; {@code theme} may be {@code null} to use {@link HellowTheme#DEFAULT}. */
    public static Element button(String label, Action action, HellowTheme theme) {
        return new ButtonElement(label, action, null, null, theme);
    }

    public static Element button(String label, Action action, Component tooltip) {
        return new ButtonElement(label, action, tooltip);
    }

    /**
     * Button that posts a {@link com.hellow.event.HellowElementPressedEvent} on the
     * NeoForge game bus when pressed (in addition to running {@code action}, which may
     * be a no-op when the caller only wants the event).
     */
    public static Element button(String label, Action action, ElementId elementId) {
        return new ButtonElement(label, action, elementId);
    }

    public static Element link(String text, String url) {
        return new LinkElement(Component.literal(text), url, DEFAULT_LINK_COLOR);
    }

    /** Themed variant (uses {@code theme.linkColor()}); {@code theme} may be {@code null}. */
    public static Element link(String text, String url, HellowTheme theme) {
        HellowTheme t = theme == null ? HellowTheme.DEFAULT : theme;
        return new LinkElement(Component.literal(text), url, t.linkColor());
    }

    public static Element link(String text, String url, Consumer<String> onOpen) {
        return new LinkElement(Component.literal(text), url, DEFAULT_LINK_COLOR, onOpen);
    }

    public static Element spacer(int height) {
        return new SpacerElement(height);
    }

    public static Element toggle(String label, boolean value, Consumer<Boolean> onChange) {
        return new ToggleElement(label, value, onChange);
    }

    /** Themed variant; {@code theme} may be {@code null}. */
    public static Element toggle(String label, boolean value, Consumer<Boolean> onChange, HellowTheme theme) {
        return new ToggleElement(label, value, onChange, null, theme);
    }

    public static Element toggle(String label, boolean value, Consumer<Boolean> onChange, ElementId elementId) {
        return new ToggleElement(label, value, onChange, elementId);
    }

    public static Element slider(String label, int min, int max, int value, Consumer<Integer> onChange) {
        return new SliderElement(label, min, max, value, onChange);
    }

    /** Themed variant; {@code theme} may be {@code null}. */
    public static Element slider(String label, int min, int max, int value, Consumer<Integer> onChange, HellowTheme theme) {
        return new SliderElement(label, min, max, value, onChange, null, theme);
    }

    public static Element slider(String label, int min, int max, int value, Consumer<Integer> onChange, ElementId elementId) {
        return new SliderElement(label, min, max, value, onChange, elementId);
    }

    public static Element textInput(String label, String value, Consumer<String> onChange) {
        return new TextInputElement(label, value, onChange);
    }

    /** Themed variant; {@code theme} may be {@code null}. */
    public static Element textInput(String label, String value, Consumer<String> onChange, HellowTheme theme) {
        return new TextInputElement(label, value, onChange, null, theme);
    }

    public static Element textInput(String label, String value, Consumer<String> onChange, ElementId elementId) {
        return new TextInputElement(label, value, onChange, elementId);
    }

    public static Element enumSelect(String label, List<String> options, int index, Consumer<String> onChange) {
        return new EnumElement(label, options, index, onChange);
    }

    /** Themed variant; {@code theme} may be {@code null}. */
    public static Element enumSelect(String label, List<String> options, int index, Consumer<String> onChange, HellowTheme theme) {
        return new EnumElement(label, options, index, onChange, null, theme);
    }

    public static Element enumSelect(String label, List<String> options, int index, Consumer<String> onChange, ElementId elementId) {
        return new EnumElement(label, options, index, onChange, elementId);
    }

    // -------- Controlled form widgets (State-backed) --------

    /** Create a controllable value holder for form read-back. */
    public static <T> State<T> state(T value) {
        return State.of(value);
    }

    /** Toggle backed by a {@link State}; every change is written back to it. */
    public static Element toggle(String label, State<Boolean> state) {
        return new ToggleElement(label, state.get(), state::set);
    }

    /** Slider backed by a {@link State}; every change is written back to it. */
    public static Element slider(String label, int min, int max, State<Integer> state) {
        return new SliderElement(label, min, max, state.get(), state::set);
    }

    /** Text input backed by a {@link State}; every change is written back to it. */
    public static Element textInput(String label, State<String> state) {
        return new TextInputElement(label, state.get(), state::set);
    }

    /** Enum backed by a {@link State<Integer>} holding the selected option index. */
    public static Element enumSelectIndex(String label, List<String> options, State<Integer> indexState) {
        int initial = options.isEmpty() ? 0 : Math.max(0, Math.min(options.size() - 1, indexState.get()));
        return new EnumElement(label, options, initial, value -> indexState.set(options.indexOf(value)));
    }

    /** Themed variant of {@link #enumSelectIndex}; {@code theme} may be {@code null}. */
    public static Element enumSelectIndex(String label, List<String> options, State<Integer> indexState, HellowTheme theme) {
        int initial = options.isEmpty() ? 0 : Math.max(0, Math.min(options.size() - 1, indexState.get()));
        return new EnumElement(label, options, initial, value -> indexState.set(options.indexOf(value)), null, theme);
    }

    /** Enum backed by a {@link State<String>} holding the selected option value. */
    public static Element enumSelect(String label, List<String> options, State<String> valueState) {
        int initial = options.isEmpty() ? 0 : Math.max(0, options.indexOf(valueState.get()));
        return new EnumElement(label, options, initial, valueState::set);
    }

    // -------- Layout containers --------

    public static Element column(int gap, Element... children) {
        return new ColumnElement(copyOf(children), gap);
    }

    /** Column with horizontal child alignment (e.g. {@code ContentAlign.CENTER_TOP}). */
    public static Element column(ContentAlign align, int gap, Element... children) {
        return new ColumnElement(copyOf(children), gap, align);
    }

    public static Element row(int gap, Element... children) {
        return new RowElement(copyOf(children), gap);
    }

    /** Row with horizontal alignment of the whole row (e.g. {@code ContentAlign.RIGHT_TOP}). */
    public static Element row(ContentAlign align, int gap, Element... children) {
        return new RowElement(copyOf(children), gap, align);
    }

    /** Wrap a child with a flex weight so a Row distributes spare width to it. */
    public static Element flex(Element child, int weight) {
        return new FlexElement(child, weight);
    }

    /** A flexible gap that absorbs all spare width in a Row (weight 1). */
    public static Element fillSpacer() {
        return new FlexElement(new SpacerElement(0), 1);
    }

    /** A flexible gap with an explicit weight. */
    public static Element fillSpacer(int weight) {
        return new FlexElement(new SpacerElement(0), weight);
    }

    /**
     * Wrap a child so {@link ColumnElement} / {@link GridElement} stretch it over the
     * container's remaining width (e.g. {@code fill(wrappedText(...))} to make wrapped
     * text consume the full column width).
     */
    public static Element fill(Element child) {
        return new FillElement(child);
    }

    public static Element grid(int columns, int gap, Element... children) {
        return new GridElement(copyOf(children), columns, gap);
    }

    /** Grid with per-column flex weights; a positive weight stretches that column. */
    public static Element grid(int columns, int gap, int[] columnWeights, Element... children) {
        return new GridElement(copyOf(children), columns, gap, columnWeights);
    }

    public static Element wrapRow(int gap, int maxWidth, Element... children) {
        return new WrapRowElement(copyOf(children), gap, maxWidth);
    }

    public static Element scroll(int viewportHeight, int gap, Element... children) {
        return new ScrollElement(copyOf(children), gap, viewportHeight);
    }

    /** Themed variant (theme colors the scrollbar); {@code theme} may be {@code null}. */
    public static Element scroll(HellowTheme theme, int viewportHeight, int gap, Element... children) {
        return new ScrollElement(copyOf(children), gap, viewportHeight, null, theme);
    }

    /**
     * Scroll view backed by an external {@link State} holding the offset, so the
     * position survives a rebuild of the element tree (pass the same state back).
     */
    public static Element scroll(State<Integer> scrollState, int viewportHeight, int gap, Element... children) {
        return new ScrollElement(copyOf(children), gap, viewportHeight, scrollState);
    }

    /** Virtualized-style fixed-row-height list: items are built on demand, viewport-culled. */
    public static Element listView(int itemCount, int itemHeight, int gap, int viewportHeight,
                                   Function<Integer, Element> itemProvider) {
        return new ListView(itemCount, itemHeight, gap, viewportHeight, itemProvider);
    }

    /**
     * Virtualized-style fixed-row-height list backed by an external {@link State}
     * holding the scroll offset (see {@link #scroll(State, int, int, Element...)}).
     */
    public static Element listView(State<Integer> scrollState, int itemCount, int itemHeight, int gap,
                                   int viewportHeight, Function<Integer, Element> itemProvider) {
        return new ListView(itemCount, itemHeight, gap, viewportHeight, itemProvider, scrollState);
    }

    public static Element box(Element content, Spacing padding) {
        return new BoxElement(content, Spacing.NONE, padding, 1, DEFAULT_BORDER_COLOR);
    }

    public static Element box(Element content, Spacing margin, Spacing padding,
                              int borderWidth, int borderColor) {
        return new BoxElement(content, margin, padding, borderWidth, borderColor);
    }

    public static Element absolute(List<AbsoluteElement.Entry> entries) {
        return new AbsoluteElement(entries);
    }

    private static List<Element> copyOf(Element[] children) {
        List<Element> list = new ArrayList<>(children.length);
        for (Element child : children) list.add(child);
        return list;
    }
}
