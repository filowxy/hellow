package com.hellow.element;

import com.hellow.action.Action;
import com.hellow.event.ElementId;
import com.hellow.layout.ContentAlign;
import com.hellow.layout.ElementLayout;
import com.hellow.layout.Justify;
import com.hellow.layout.Spacing;
import com.hellow.state.State;
import com.hellow.style.HellowColor;
import com.hellow.style.HellowTheme;
import com.hellow.style.HellowThemes;
import com.hellow.text.StringMode;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Elements {
    private Elements() {}

    private static final HellowColor DEFAULT_TEXT_COLOR = () -> HellowThemes.defaultTheme().textColor().argb();
    private static final HellowColor DEFAULT_LINK_COLOR = () -> HellowThemes.defaultTheme().linkColor().argb();
    private static final HellowColor DEFAULT_BORDER_COLOR = () -> HellowThemes.defaultTheme().boxBorder().argb();

    public static Element text(String text) {
        return new TextElement(Component.literal(text), DEFAULT_TEXT_COLOR);
    }

    public static Element text(String text, int color) {
        return new TextElement(Component.literal(text), color);
    }

    public static Element text(Component text, int color) {
        return new TextElement(text, color);
    }

    public static Element heading(String text) {
        return new TextElement(Component.literal(text).withStyle(ChatFormatting.BOLD), DEFAULT_TEXT_COLOR);
    }

    public static Element heading(String text, int color) {
        return new TextElement(Component.literal(text).withStyle(ChatFormatting.BOLD), color);
    }

    public static Element divider() {
        return new FillElement(new DividerElement());
    }

    public static Element divider(int thickness, int color) {
        return new FillElement(new DividerElement(thickness, color));
    }

    public static Element progressBar(String label, int value, int max) {
        return new ProgressBarElement(label, value, max);
    }

    public static Element progressBar(String label, int value, int max, HellowTheme theme) {
        return new ProgressBarElement(label, value, max, theme);
    }

    public static Element withTooltip(Element child, Component tooltip) {
        return new TooltipElement(child, tooltip);
    }

    public static Element withTooltip(Element child, String tooltip) {
        return new TooltipElement(child, Component.literal(tooltip));
    }

    public static Element clickable(Element child, Action action) {
        return new ClickableElement(child, action);
    }

    public static Element clickable(Element child, Runnable action) {
        return new ClickableElement(child, action::run);
    }

    public static Element wrappedText(String text, int color) {
        return wrappedText(Component.literal(text), color);
    }

    public static Element wrappedText(Component text, int color) {
        return new TextElement(text, color, true);
    }

    public static Element clampedText(String text, int color) {
        return new TextElement(Component.literal(text), color, StringMode.CLAMPED);
    }

    public static Element clampedText(Component text, int color) {
        return new TextElement(text, color, StringMode.CLAMPED);
    }

    public static Element button(String label, Action action) {
        return new ButtonElement(label, action);
    }

    public static Element button(String label, Action action, boolean enabled) {
        return new ButtonElement(label, action, null, null, HellowTheme.DEFAULT, enabled);
    }

    public static Element button(String label, Action action, HellowTheme theme) {
        return new ButtonElement(label, action, null, null, theme);
    }

    public static Element button(String label, Action action, Component tooltip) {
        return new ButtonElement(label, action, tooltip);
    }

    public static Element button(String label, Action action, ElementId elementId) {
        return new ButtonElement(label, action, elementId);
    }

    public static Element link(String text, String url) {
        return new LinkElement(Component.literal(text), url, DEFAULT_LINK_COLOR);
    }

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

    public static Element toggle(String label, boolean value, Consumer<Boolean> onChange, HellowTheme theme) {
        return new ToggleElement(label, value, onChange, null, theme);
    }

    public static Element toggle(String label, boolean value, Consumer<Boolean> onChange, ElementId elementId) {
        return new ToggleElement(label, value, onChange, elementId);
    }

    public static Element slider(String label, int min, int max, int value, Consumer<Integer> onChange) {
        return new SliderElement(label, min, max, value, onChange);
    }

    public static Element slider(String label, int min, int max, int value, Consumer<Integer> onChange, HellowTheme theme) {
        return new SliderElement(label, min, max, value, onChange, null, theme);
    }

    public static Element slider(String label, int min, int max, int value, Consumer<Integer> onChange, ElementId elementId) {
        return new SliderElement(label, min, max, value, onChange, elementId);
    }

    public static Element textInput(String label, String value, Consumer<String> onChange) {
        return new TextInputElement(label, value, onChange);
    }

    public static Element textInput(String label, String value, Consumer<String> onChange, HellowTheme theme) {
        return new TextInputElement(label, value, onChange, null, theme);
    }

    public static Element textInput(String label, String value, Consumer<String> onChange, ElementId elementId) {
        return new TextInputElement(label, value, onChange, elementId);
    }

    public static Element enumSelect(String label, List<String> options, int index, Consumer<String> onChange) {
        return new EnumElement(label, options, index, onChange);
    }

    public static Element enumSelect(String label, List<String> options, int index, Consumer<String> onChange, HellowTheme theme) {
        return new EnumElement(label, options, index, onChange, null, theme);
    }

    public static Element enumSelect(String label, List<String> options, int index, Consumer<String> onChange, ElementId elementId) {
        return new EnumElement(label, options, index, onChange, elementId);
    }

    public static <T> State<T> state(T value) {
        return State.of(value);
    }

    public static Element toggle(String label, State<Boolean> state) {
        return new ToggleElement(label, state.get(), state::set);
    }

    public static Element slider(String label, int min, int max, State<Integer> state) {
        return new SliderElement(label, min, max, state.get(), state::set);
    }

    public static Element textInput(String label, State<String> state) {
        return new TextInputElement(label, state.get(), state::set);
    }

    public static Element enumSelectIndex(String label, List<String> options, State<Integer> indexState) {
        int initial = options.isEmpty() ? 0 : Math.max(0, Math.min(options.size() - 1, indexState.get()));
        return new EnumElement(label, options, initial, value -> indexState.set(options.indexOf(value)));
    }

    public static Element enumSelectIndex(String label, List<String> options, State<Integer> indexState, HellowTheme theme) {
        int initial = options.isEmpty() ? 0 : Math.max(0, Math.min(options.size() - 1, indexState.get()));
        return new EnumElement(label, options, initial, value -> indexState.set(options.indexOf(value)), null, theme);
    }

    public static Element enumSelect(String label, List<String> options, State<String> valueState) {
        int initial = options.isEmpty() ? 0 : Math.max(0, options.indexOf(valueState.get()));
        return new EnumElement(label, options, initial, valueState::set);
    }

    public static Element column(int gap, Element... children) {
        return new ColumnElement(copyOf(children), gap);
    }

    public static Element column(ContentAlign align, int gap, Element... children) {
        return new ColumnElement(copyOf(children), gap, align);
    }

    public static Element row(int gap, Element... children) {
        return new RowElement(copyOf(children), gap);
    }

    public static Element row(ContentAlign align, int gap, Element... children) {
        return new RowElement(copyOf(children), gap, align);
    }

    /** Row with an explicit main-axis distribution ({@link Justify#EQUAL} for tabs). */
    public static Element row(Justify justify, int gap, Element... children) {
        return new RowElement(copyOf(children), gap, ContentAlign.LEFT_TOP, justify);
    }

    public static Element row(ContentAlign align, Justify justify, int gap, Element... children) {
        return new RowElement(copyOf(children), gap, align, justify);
    }

    // -------- Per-child layout intent (see ElementLayout) --------

    /** Wrap a child with a full layout description. */
    public static Element styled(Element child, ElementLayout layout) {
        return new StyledElement(child, layout);
    }

    /** Share of the row's spare width, e.g. {@code weighted(text, 2)}. */
    public static Element weighted(Element child, int weight) {
        return new StyledElement(child, ElementLayout.weighted(weight));
    }

    /** Lower width bound for a child. */
    public static Element minWidth(Element child, int minWidth) {
        return new StyledElement(child, ElementLayout.NATURAL.withWidthBounds(minWidth, ElementLayout.UNSET));
    }

    /** Upper width bound for a child — the clean way to make text wrap or clamp. */
    public static Element maxWidth(Element child, int maxWidth) {
        return new StyledElement(child, ElementLayout.NATURAL.withWidthBounds(ElementLayout.UNSET, maxWidth));
    }

    /** Both width bounds at once. */
    public static Element widthBounds(Element child, int minWidth, int maxWidth) {
        return new StyledElement(child, ElementLayout.NATURAL.withWidthBounds(minWidth, maxWidth));
    }

    /** Alignment of this child inside the slot its parent hands it. */
    public static Element aligned(Element child, ContentAlign align) {
        return new StyledElement(child, ElementLayout.NATURAL.withAlign(align));
    }

    // -------- Decorations (see com.hellow.decor) --------

    /** Wrap a child with optional visual layers; layout and input are untouched. */
    public static Element decorate(Element child, com.hellow.decor.Decoration... decorations) {
        return new com.hellow.decor.DecoratedElement(child, java.util.List.of(decorations));
    }

    /** List-taking form, e.g. {@code decorate(child, DecorationPresets.card(theme))}. */
    public static Element decorate(Element child, List<com.hellow.decor.Decoration> decorations) {
        return new com.hellow.decor.DecoratedElement(child, decorations);
    }

    public static Element flex(Element child, int weight) {
        return new FlexElement(child, weight);
    }

    public static Element fillSpacer() {
        return new FlexElement(new SpacerElement(0), 1);
    }

    public static Element fillSpacer(int weight) {
        return new FlexElement(new SpacerElement(0), weight);
    }

    public static Element fill(Element child) {
        return new FillElement(child);
    }

    public static Element grid(int columns, int gap, Element... children) {
        return new GridElement(copyOf(children), columns, gap);
    }

    public static Element grid(int columns, int gap, int[] columnWeights, Element... children) {
        return new GridElement(copyOf(children), columns, gap, columnWeights);
    }

    public static Element wrapRow(int gap, int maxWidth, Element... children) {
        return new WrapRowElement(copyOf(children), gap, maxWidth);
    }

    /** Wrap row whose lines align horizontally inside the available width. */
    public static Element wrapRow(ContentAlign align, int gap, int maxWidth, Element... children) {
        return new WrapRowElement(copyOf(children), gap, maxWidth, align);
    }

    public static Element scroll(int viewportHeight, int gap, Element... children) {
        return new ScrollElement(copyOf(children), gap, viewportHeight);
    }

    public static Element scroll(HellowTheme theme, int viewportHeight, int gap, Element... children) {
        return new ScrollElement(copyOf(children), gap, viewportHeight, null, theme);
    }

    public static Element scroll(State<Integer> scrollState, int viewportHeight, int gap, Element... children) {
        return new ScrollElement(copyOf(children), gap, viewportHeight, scrollState);
    }

    public static Element listView(int itemCount, int itemHeight, int gap, int viewportHeight,
                                   Function<Integer, Element> itemProvider) {
        return new ListView(itemCount, itemHeight, gap, viewportHeight, itemProvider);
    }

    public static Element listView(State<Integer> scrollState, int itemCount, int itemHeight, int gap,
                                   int viewportHeight, Function<Integer, Element> itemProvider) {
        return new ListView(itemCount, itemHeight, gap, viewportHeight, itemProvider, scrollState);
    }

    public static Element box(Element content, Spacing padding) {
        return new BoxElement(content, Spacing.NONE, padding, 1, DEFAULT_BORDER_COLOR.argb());
    }

    public static Element box(Element content, Spacing margin, Spacing padding,
                              int borderWidth, int borderColor) {
        return new BoxElement(content, margin, padding, borderWidth, borderColor);
    }

    public static Element padding(Spacing padding, Element... children) {
        return new BoxElement(column(0, children), Spacing.NONE, padding, 0, 0);
    }

    public static Element margin(Spacing margin, Element... children) {
        return new BoxElement(column(0, children), margin, Spacing.NONE, 0, 0);
    }

    public static Element list(int itemHeight, int gap, int viewportHeight, Element... items) {
        return new ListView(itemHeight, gap, viewportHeight, List.of(items));
    }

    public static Element list(State<Integer> scrollState, int itemHeight, int gap, int viewportHeight,
                               Element... items) {
        return new ListView(itemHeight, gap, viewportHeight, List.of(items), scrollState);
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
