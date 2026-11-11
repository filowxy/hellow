package com.hellow.pages;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hellow.Hellow;
import com.hellow.HellowConstants;
import com.hellow.action.Action;
import com.hellow.decor.Backdrop;
import com.hellow.decor.CornerAccents;
import com.hellow.decor.Decoration;
import com.hellow.decor.DecorationPresets;
import com.hellow.decor.DecoratedElement;
import com.hellow.decor.Glow;
import com.hellow.decor.GradientRing;
import com.hellow.decor.Shadow;
import com.hellow.decor.SolidBorder;
import com.hellow.decor.Sparkles;
import com.hellow.element.AbsoluteElement;
import com.hellow.element.BoxElement;
import com.hellow.element.ButtonElement;
import com.hellow.element.ClickableElement;
import com.hellow.element.ColumnElement;
import com.hellow.element.DividerElement;
import com.hellow.element.Element;
import com.hellow.element.ElementRegistry;
import com.hellow.element.Elements;
import com.hellow.element.EnumElement;
import com.hellow.element.ExpandableListElement;
import com.hellow.element.FillElement;
import com.hellow.element.FlexElement;
import com.hellow.element.GridElement;
import com.hellow.element.LinkElement;
import com.hellow.element.ListView;
import com.hellow.element.ProgressBarElement;
import com.hellow.element.RowElement;
import com.hellow.element.ScrollElement;
import com.hellow.element.SliderElement;
import com.hellow.element.SpacerElement;
import com.hellow.element.StyledElement;
import com.hellow.element.TextElement;
import com.hellow.element.TextInputElement;
import com.hellow.element.ToggleElement;
import com.hellow.element.TooltipElement;
import com.hellow.element.WrapRowElement;
import com.hellow.event.ElementId;
import com.hellow.layout.ContentAlign;
import com.hellow.layout.ElementLayout;
import com.hellow.layout.Justify;
import com.hellow.layout.Spacing;
import com.hellow.page.PageId;
import com.hellow.page.PageLayer;
import com.hellow.page.PageSpec;
import com.hellow.page.WindowSpec;
import com.hellow.style.HellowColor;
import com.hellow.style.HellowTheme;
import com.hellow.style.HellowThemes;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.NeoForge;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PageFileParser {

    private PageFileParser() {}

    public static PageFileDef parse(String json, String source) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        String id = requireString(root, "id", source);
        String title = optString(root, "title", id);
        PageLayer layer = parseLayer(optString(root, "layer", "NORMAL"));
        // Resolved before the body so decoration presets can derive their colors
        // from the same theme the page renders with.
        HellowTheme theme = root.has("theme") ? HellowThemes.resolve(optString(root, "theme", null)) : null;
        WindowSpec window = window(root);

        JsonObject body = requireObject(root, "body", source);
        Element tree = buildElement(body, source, theme);

        JsonObject trigger = root.has("trigger") ? root.getAsJsonObject("trigger") : null;
        String triggerScreen = null;
        String triggerEvent = null;
        String strategy = "once";
        if (trigger != null) {
            String type = requireString(trigger, "type", source + ".trigger");
            strategy = optString(trigger, "strategy", "once");
            if (type.equals("screen")) {
                triggerScreen = requireString(trigger, "screen", source + ".trigger");
            } else if (type.equals("event")) {
                triggerEvent = requireString(trigger, "event", source + ".trigger");
            } else {
                throw new IllegalArgumentException(source + ": unknown trigger type '" + type
                        + "' (expected 'screen' or 'event')");
            }
        }

        PageSpec page = new PageSpec(PageId.of(id), optString(root, "title", id), layer, null, null, tree, theme, window);
        return new PageFileDef(page, triggerScreen, triggerEvent, strategy);
    }

    // -------- Elements --------

    private static Element buildElement(JsonObject o, String where, HellowTheme theme) {
        Element base = buildElementBase(o, where, theme);
        base = applyStyle(base, o);
        return applyDecorations(base, o, where, theme);
    }

    private static Element buildElementBase(JsonObject o, String where, HellowTheme theme) {
        String type = requireString(o, "type", where);
        return switch (type) {
            case "column" -> column(o, where, theme);
            case "row" -> row(o, where, theme);
            case "grid" -> grid(o, where, theme);
            case "scroll" -> scroll(o, where, theme);
            case "flex" -> flex(o, where, theme);
            case "fill" -> new FillElement(buildElement(requireObject(o, "child", where), where, theme));
            case "fillSpacer" -> Elements.fillSpacer(optInt(o, "weight", 1));
            case "spacer" -> new SpacerElement(optInt(o, "height", 0));
            case "text" -> new TextElement(Component.literal(optString(o, "text", "")), optColor(o));
            case "wrappedText" -> new TextElement(Component.literal(optString(o, "text", "")), optColor(o), true);
            case "clampedText" -> TextElement.clamped(Component.literal(optString(o, "text", "")), optColor(o));
            case "heading" -> Elements.heading(optString(o, "text", ""), optColor(o));
            case "button" -> button(o, where);
            case "link" -> link(o, where);
            case "toggle" -> toggle(o, where);
            case "slider" -> slider(o, where);
            case "textInput" -> new TextInputElement(optString(o, "label", ""), optString(o, "value", ""),
                    v -> emitValue(where, v));
            case "enumSelect" -> enumSelect(o, where);
            case "box" -> box(o, where, theme);
            case "wrapRow" -> new WrapRowElement(children(o, where, theme), optInt(o, "gap", 4),
                    optInt(o, "maxWidth", 200), parseAlign(optString(o, "align", "LEFT_TOP")));
            case "list" -> new ListView(optInt(o, "itemHeight", 20), optInt(o, "gap", 4),
                    optInt(o, "viewportHeight", 100), children(o, where, theme));
            case "expandableList" -> expandableList(o, where, theme);
            case "divider" -> new FillElement(new DividerElement(optInt(o, "thickness", 1),
                    optColor(o, "color", 0xFFFFFFFF)));
            case "progressBar" -> new ProgressBarElement(optString(o, "text", ""),
                    optInt(o, "value", 0), optInt(o, "max", 100));
            case "withTooltip" -> new TooltipElement(
                    buildElement(requireObject(o, "child", where), where, theme),
                    Component.literal(optString(o, "tooltip", "")));
            case "clickable" -> new ClickableElement(
                    buildElement(requireObject(o, "child", where), where, theme), parseAction(o, where));
            case "padding" -> Elements.padding(Spacing.all(optInt(o, "padding", 4)),
                    buildElement(requireObject(o, "child", where), where, theme));
            case "margin" -> Elements.margin(Spacing.all(optInt(o, "margin", 4)),
                    buildElement(requireObject(o, "child", where), where, theme));
            case "absolute" -> absolute(o, where, theme);
            default -> customElement(type, o, where);
        };
    }

    private static Element customElement(String type, JsonObject o, String where) {
        if (!ElementRegistry.contains(type)) {
            throw new IllegalArgumentException(where + ": unknown element type '" + type
                    + "' (not a built-in type and no custom factory registered for it)");
        }
        return ElementRegistry.create(type, o);
    }

    private static Element column(JsonObject o, String where, HellowTheme theme) {
        String align = optString(o, "align", null);
        return align == null
                ? new ColumnElement(children(o, where, theme), optInt(o, "gap", 4))
                : new ColumnElement(children(o, where, theme), optInt(o, "gap", 4), parseAlign(align));
    }

    private static Element row(JsonObject o, String where, HellowTheme theme) {
        String align = optString(o, "align", null);
        String justify = optString(o, "justify", null);
        if (justify != null) {
            return new RowElement(children(o, where, theme), optInt(o, "gap", 4),
                    parseAlign(align), parseJustify(justify));
        }
        return align == null
                ? new RowElement(children(o, where, theme), optInt(o, "gap", 4))
                : new RowElement(children(o, where, theme), optInt(o, "gap", 4), parseAlign(align));
    }

    private static ContentAlign parseAlign(String s) {
        return parseAlign(s, ContentAlign.LEFT_TOP);
    }

    private static ContentAlign parseAlign(String s, ContentAlign fallback) {
        if (s == null) return fallback;
        try {
            return ContentAlign.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private static Justify parseJustify(String s) {
        try {
            return Justify.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Justify.START;
        }
    }

    private static Element grid(JsonObject o, String where, HellowTheme theme) {
        List<Element> children = children(o, where, theme);
        int columns = Math.max(1, optInt(o, "columns", 1));
        JsonArray weights = o.has("columnWeights") ? o.getAsJsonArray("columnWeights") : null;
        int[] colW = null;
        if (weights != null) {
            colW = new int[columns];
            for (int i = 0; i < columns && i < weights.size(); i++) colW[i] = weights.get(i).getAsInt();
        }
        return new GridElement(children, columns, optInt(o, "gap", 4), colW);
    }

    private static Element scroll(JsonObject o, String where, HellowTheme theme) {
        return new ScrollElement(children(o, where, theme), optInt(o, "gap", 4), optInt(o, "viewportHeight", 120));
    }

    private static Element flex(JsonObject o, String where, HellowTheme theme) {
        Element child = buildElement(requireObject(o, "child", where), where, theme);
        return new FlexElement(child, Math.max(0, optInt(o, "weight", 1)));
    }

    private static Element button(JsonObject o, String where) {
        String label = optString(o, "text", "");
        Action action = parseAction(o, where);
        String tooltip = optString(o, "tooltip", null);
        boolean enabled = optBool(o, "enabled", true);
        return new ButtonElement(label, action,
                tooltip == null ? null : Component.literal(tooltip),
                null, null, enabled);
    }

    private static Element link(JsonObject o, String where) {
        return new LinkElement(Component.literal(optString(o, "text", "")), optString(o, "url", ""),
                0xFF55AAFF, url -> openExternal(url));
    }

    private static Element toggle(JsonObject o, String where) {
        String id = optString(o, "elementId", null);
        return new ToggleElement(optString(o, "text", ""), optBool(o, "value", false),
                v -> emitValue(where, String.valueOf(v)), id == null ? null : ElementId.of(id));
    }

    private static Element slider(JsonObject o, String where) {
        String id = optString(o, "elementId", null);
        return new SliderElement(optString(o, "text", ""), optInt(o, "min", 0), optInt(o, "max", 100),
                optInt(o, "value", 50), v -> emitValue(where, String.valueOf(v)),
                id == null ? null : ElementId.of(id));
    }

    private static Element enumSelect(JsonObject o, String where) {
        List<String> options = new ArrayList<>();
        if (o.has("options")) {
            for (JsonElement e : o.getAsJsonArray("options")) options.add(e.getAsString());
        }
        return new EnumElement(optString(o, "text", ""), options, optInt(o, "index", 0),
                v -> emitValue(where, v));
    }

    private static Element box(JsonObject o, String where, HellowTheme theme) {
        Element content = buildElement(requireObject(o, "child", where), where, theme);
        Spacing padding = Spacing.all(optInt(o, "padding", 4));
        Spacing margin = Spacing.all(optInt(o, "margin", 0));
        return new BoxElement(content, margin, padding, optInt(o, "borderWidth", 1), optColor(o, "borderColor", 0xFF888888));
    }

    private static Element expandableList(JsonObject o, String where, HellowTheme theme) {
        List<String> items = new ArrayList<>();
        if (o.has("items")) {
            for (JsonElement e : o.getAsJsonArray("items")) items.add(e.getAsString());
        }
        return new ExpandableListElement(optString(o, "header", "Expandable list"), items,
                Elements::text, optInt(o, "gap", 2));
    }

    private static Element absolute(JsonObject o, String where, HellowTheme theme) {
        List<AbsoluteElement.Entry> entries = new ArrayList<>();
        if (o.has("entries")) {
            int i = 0;
            for (JsonElement e : o.getAsJsonArray("entries")) {
                JsonObject entry = e.getAsJsonObject();
                Element child = buildElement(requireObject(entry, "child", where + ".entries[" + i + "]"), where, theme);
                entries.add(new AbsoluteElement.Entry(child, optInt(entry, "x", 0), optInt(entry, "y", 0)));
                i++;
            }
        }
        return new AbsoluteElement(entries);
    }

    private static List<Element> children(JsonObject o, String where, HellowTheme theme) {
        List<Element> out = new ArrayList<>();
        if (!o.has("children")) return out;
        int i = 0;
        for (JsonElement e : o.getAsJsonArray("children")) {
            out.add(buildElement(e.getAsJsonObject(), where + ".children[" + i + "]", theme));
            i++;
        }
        return out;
    }

    // -------- Per-element style --------

    /**
     * Applies the optional {@code style} block — the JSON face of
     * {@link ElementLayout}. A missing or empty block leaves the element as-is,
     * so wrapping only happens when the author asked for it.
     */
    private static Element applyStyle(Element base, JsonObject o) {
        if (!o.has("style") || !o.get("style").isJsonObject()) return base;
        JsonObject style = o.getAsJsonObject("style");
        int weight = optInt(style, "weight", 0);
        boolean fill = optBool(style, "fill", false);
        int minWidth = optInt(style, "minWidth", ElementLayout.UNSET);
        int maxWidth = optInt(style, "maxWidth", ElementLayout.UNSET);
        String align = optString(style, "align", null);
        if (weight == 0 && !fill && minWidth == ElementLayout.UNSET
                && maxWidth == ElementLayout.UNSET && align == null) {
            return base;
        }
        ContentAlign parsed = align == null ? null : parseAlign(align, null);
        return new StyledElement(base, new ElementLayout(weight, fill, minWidth, maxWidth, parsed));
    }

    // -------- Decorations --------

    private static Element applyDecorations(Element base, JsonObject o, String where, HellowTheme theme) {
        if (!o.has("decorations") || !o.get("decorations").isJsonArray()) return base;
        List<Decoration> decorations = new ArrayList<>();
        int i = 0;
        for (JsonElement e : o.getAsJsonArray("decorations")) {
            String at = where + ".decorations[" + i + "]";
            if (e.isJsonObject()) {
                addDecoration(e.getAsJsonObject(), decorations, at, theme);
            } else if (e.isJsonPrimitive()) {
                addPreset(e.getAsString(), decorations, at, theme);
            }
            i++;
        }
        return decorations.isEmpty() ? base : new DecoratedElement(base, decorations);
    }

    private static void addDecoration(JsonObject d, List<Decoration> out, String where, HellowTheme theme) {
        String kind = optString(d, "kind", null);
        if (kind == null) {
            HellowConstants.log().warn("[hellow] {}: decoration without 'kind' skipped", where);
            return;
        }
        switch (kind) {
            case "backdrop" -> out.add(new Backdrop(
                    HellowColor.of(optColor(d)),
                    HellowColor.of(optColor(d, "color2", optColor(d)))));
            case "border" -> out.add(new SolidBorder(optInt(d, "thickness", 1), HellowColor.of(optColor(d))));
            case "gradientRing" -> out.add(new GradientRing(
                    optInt(d, "thickness", 1),
                    HellowColor.of(optColor(d, "start", 0xFFFFFFFF)),
                    HellowColor.of(optColor(d, "end", 0xFF888888)),
                    HellowColor.of(optColor(d, "inner", 0xFF1E1E1E))));
            case "glow" -> out.add(new Glow(optInt(d, "radius", 3), HellowColor.of(optColor(d))));
            case "shadow" -> out.add(new Shadow(
                    optInt(d, "offsetX", 2), optInt(d, "offsetY", 2), optInt(d, "spread", 3),
                    HellowColor.of(optColor(d, "color", 0x66000000))));
            case "corners" -> out.add(new CornerAccents(
                    optInt(d, "length", 8), optInt(d, "thickness", 1), HellowColor.of(optColor(d))));
            case "sparkles" -> out.add(new Sparkles(
                    optInt(d, "count", 8), optInt(d, "size", 1), (float) optDouble(d, "speed", 1.0),
                    HellowColor.of(optColor(d)), optString(d, "id", null)));
            case "preset" -> addPreset(optString(d, "name", ""), out, where, theme);
            default -> HellowConstants.log().warn("[hellow] {}: unknown decoration kind '{}' skipped", where, kind);
        }
    }

    private static void addPreset(String name, List<Decoration> out, String where, HellowTheme theme) {
        switch (name) {
            case "outline" -> out.addAll(DecorationPresets.outline(theme));
            case "panel" -> out.addAll(DecorationPresets.panel(theme));
            case "card" -> out.addAll(DecorationPresets.card(theme));
            case "glow" -> out.addAll(DecorationPresets.glow(theme));
            case "chip" -> out.addAll(DecorationPresets.chip(theme));
            case "focus" -> out.addAll(DecorationPresets.focus(theme));
            case "shadow" -> out.addAll(DecorationPresets.shadow(theme));
            case "framed" -> out.addAll(DecorationPresets.framed(theme));
            default -> HellowConstants.log().warn("[hellow] {}: unknown decoration preset '{}' skipped", where, name);
        }
    }

    // -------- Window --------

    private static WindowSpec window(JsonObject root) {
        if (!root.has("window") || !root.get("window").isJsonObject()) return WindowSpec.DEFAULT;
        JsonObject w = root.getAsJsonObject("window");
        return new WindowSpec(
                optInt(w, "width", WindowSpec.AUTO),
                optInt(w, "height", WindowSpec.AUTO),
                parseAlign(optString(w, "anchor", "CENTER_MIDDLE"), ContentAlign.CENTER_MIDDLE),
                optInt(w, "padding", 10),
                optInt(w, "titleBarHeight", 20),
                optInt(w, "contentMaxWidth", WindowSpec.AUTO));
    }

    // -------- Actions --------

    private static Action parseAction(JsonObject o, String where) {
        String onPress = optString(o, "onPress", null);
        if (onPress == null) return null;
        if (onPress.equals("closeTop") || onPress.equals("close")) {
            return Hellow::closeTop;
        }
        if (onPress.startsWith("openPage:")) {
            String target = onPress.substring("openPage:".length());
            return () -> Hellow.show(target);
        }
        if (onPress.startsWith("url:")) {
            String url = onPress.substring("url:".length());
            return () -> openExternal(url);
        }
        String event = onPress.startsWith("emit:") ? onPress.substring("emit:".length()) : onPress;
        return () -> NeoForge.EVENT_BUS.post(new PageButtonActionEvent(event));
    }

    private static void emitValue(String where, String value) {
        NeoForge.EVENT_BUS.post(new PageButtonActionEvent("file:" + where + "=" + value));
    }

    static void openExternal(String url) {
        if (url == null || url.isEmpty()) return;
        try {
            Util.getPlatform().openUri(URI.create(url));
        } catch (Exception e) {
            HellowConstants.log().warn("[hellow] failed to open file-page link '{}'", url);
        }
    }

    private static PageLayer parseLayer(String s) {
        return switch (s.toUpperCase(Locale.ROOT)) {
            case "MODAL" -> PageLayer.MODAL;
            case "TOP" -> PageLayer.TOP;
            default -> PageLayer.NORMAL;
        };
    }

    private static int optColor(JsonObject o) {
        return optColor(o, "color", 0xFFFFFFFF);
    }

    private static int optColor(JsonObject o, String key, int dflt) {
        return o.has(key) ? o.get(key).getAsInt() : dflt;
    }

    private static String requireString(JsonObject o, String key, String where) {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            throw new IllegalArgumentException(where + ": missing required field '" + key + "'");
        }
        return o.get(key).getAsString();
    }

    private static JsonObject requireObject(JsonObject o, String key, String where) {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            throw new IllegalArgumentException(where + ": missing required object '" + key + "'");
        }
        return o.get(key).getAsJsonObject();
    }

    private static String optString(JsonObject o, String key, String dflt) {
        if (!o.has(key) || o.get(key).isJsonNull()) return dflt;
        return o.get(key).getAsString();
    }

    private static int optInt(JsonObject o, String key, int dflt) {
        if (!o.has(key) || o.get(key).isJsonNull()) return dflt;
        return o.get(key).getAsInt();
    }

    private static double optDouble(JsonObject o, String key, double dflt) {
        if (!o.has(key) || o.get(key).isJsonNull()) return dflt;
        return o.get(key).getAsDouble();
    }

    private static boolean optBool(JsonObject o, String key, boolean dflt) {
        if (!o.has(key) || o.get(key).isJsonNull()) return dflt;
        return o.get(key).getAsBoolean();
    }
}
