package com.hellow.pages;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hellow.Hellow;
import com.hellow.HellowConstants;
import com.hellow.action.Action;
import com.hellow.element.BoxElement;
import com.hellow.element.ButtonElement;
import com.hellow.element.ColumnElement;
import com.hellow.element.Element;
import com.hellow.element.Elements;
import com.hellow.element.EnumElement;
import com.hellow.element.FillElement;
import com.hellow.element.FlexElement;
import com.hellow.element.GridElement;
import com.hellow.element.LinkElement;
import com.hellow.element.RowElement;
import com.hellow.element.ScrollElement;
import com.hellow.element.SliderElement;
import com.hellow.element.SpacerElement;
import com.hellow.element.TextElement;
import com.hellow.element.TextInputElement;
import com.hellow.element.ToggleElement;
import com.hellow.event.ElementId;
import com.hellow.layout.Spacing;
import com.hellow.page.PageId;
import com.hellow.page.PageLayer;
import com.hellow.page.PageSpec;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.NeoForge;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a {@code config/hellow/pages/*.json} file into a {@link PageFileDef}.
 * The JSON uses the same vocabulary as the {@code Elements} factory and Hellow's
 * trigger API, so a file describes exactly what a Java caller would build:
 *
 * <pre>{@code
 * {
 *   "id": "demo",
 *   "title": "hellow.demo.title",
 *   "layer": "MODAL",
 *   "body": {
 *     "type": "column", "gap": 6,
 *     "children": [
 *       {"type": "text", "text": "Hello from a file!"},
 *       {"type": "button", "text": "Close", "onPress": "closeTop"}
 *     ]
 *   },
 *   "trigger": { "type": "screen", "screen": "net.minecraft.client.gui.screens.TitleScreen", "strategy": "once" }
 * }
 * }</pre>
 *
 * <p>Unknown element types, missing fields and malformed trees fail loudly with an
 * {@link IllegalArgumentException} carrying the file name — the loader records such
 * failures in the debug report instead of swallowing them.
 */
public final class PageFileParser {

    private PageFileParser() {}

    /**
     * Parse a JSON string into a page definition. {@code source} is only used in
     * error messages.
     */
    public static PageFileDef parse(String json, String source) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        String id = requireString(root, "id", source);
        String title = optString(root, "title", id);
        PageLayer layer = parseLayer(optString(root, "layer", "NORMAL"));

        JsonObject body = requireObject(root, "body", source);
        Element tree = buildElement(body, source);

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

        // PageSpec carries no dialog geometry; width/height fields are accepted in
        // the file for forwards compatibility and currently ignored (ponytail:
        // custom page size needs a PageScreen change — not done yet).
        PageSpec page = new PageSpec(PageId.of(id), optString(root, "title", id), layer, null, null, tree);
        return new PageFileDef(page, triggerScreen, triggerEvent, strategy);
    }

    // -------- Element tree --------

    private static Element buildElement(JsonObject o, String where) {
        String type = requireString(o, "type", where);
        return switch (type) {
            case "column" -> column(o, where);
            case "row" -> row(o, where);
            case "grid" -> grid(o, where);
            case "scroll" -> scroll(o, where);
            case "flex" -> flex(o, where);
            case "fill" -> new FillElement(buildElement(requireObject(o, "child", where), where));
            case "fillSpacer" -> Elements.fillSpacer(optInt(o, "weight", 1));
            case "spacer" -> new SpacerElement(optInt(o, "height", 0));
            case "text" -> new TextElement(Component.literal(optString(o, "text", "")), optColor(o));
            case "wrappedText" -> new TextElement(Component.literal(optString(o, "text", "")), optColor(o), true);
            case "clampedText" -> TextElement.clamped(Component.literal(optString(o, "text", "")), optColor(o));
            case "button" -> button(o, where);
            case "link" -> link(o, where);
            case "toggle" -> toggle(o, where);
            case "slider" -> slider(o, where);
            case "textInput" -> new TextInputElement(optString(o, "label", ""), optString(o, "value", ""),
                    v -> emitValue(where, v));
            case "enumSelect" -> enumSelect(o, where);
            case "box" -> box(o, where);
            default -> throw new IllegalArgumentException(
                    where + ": unknown element type '" + type + "'");
        };
    }

    private static Element column(JsonObject o, String where) {
        return new ColumnElement(children(o, where), optInt(o, "gap", 4));
    }

    private static Element row(JsonObject o, String where) {
        return new RowElement(children(o, where), optInt(o, "gap", 4));
    }

    private static Element grid(JsonObject o, String where) {
        List<Element> children = children(o, where);
        int columns = Math.max(1, optInt(o, "columns", 1));
        JsonArray weights = o.has("columnWeights") ? o.getAsJsonArray("columnWeights") : null;
        int[] colW = null;
        if (weights != null) {
            colW = new int[columns];
            for (int i = 0; i < columns && i < weights.size(); i++) colW[i] = weights.get(i).getAsInt();
        }
        return new GridElement(children, columns, optInt(o, "gap", 4), colW);
    }

    private static Element scroll(JsonObject o, String where) {
        return new ScrollElement(children(o, where), optInt(o, "gap", 4), optInt(o, "viewportHeight", 120));
    }

    private static Element flex(JsonObject o, String where) {
        Element child = buildElement(requireObject(o, "child", where), where);
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

    private static Element box(JsonObject o, String where) {
        Element content = buildElement(requireObject(o, "child", where), where);
        Spacing padding = Spacing.all(optInt(o, "padding", 4));
        Spacing margin = Spacing.all(optInt(o, "margin", 0));
        return new BoxElement(content, margin, padding, optInt(o, "borderWidth", 1), optColor(o, "borderColor", 0xFF888888));
    }

    private static List<Element> children(JsonObject o, String where) {
        List<Element> out = new ArrayList<>();
        if (!o.has("children")) return out;
        int i = 0;
        for (JsonElement e : o.getAsJsonArray("children")) {
            out.add(buildElement(e.getAsJsonObject(), where + ".children[" + i + "]"));
            i++;
        }
        return out;
    }

    // -------- Actions & events --------

    /**
     * Map a button's "onPress" onto a Hellow action. Supported values:
     * {@code closeTop}, {@code close}, {@code openPage:<id>}, {@code url:<url>},
     * {@code emit:<name>} or a bare name (emitted as-is).
     */
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

    /** Open a URL via the platform privacy flow (same as {@link LinkElement}). */
    static void openExternal(String url) {
        if (url == null || url.isEmpty()) return;
        try {
            Util.getPlatform().openUri(URI.create(url));
        } catch (Exception e) {
            HellowConstants.log().warn("[hellow] failed to open file-page link '{}'", url);
        }
    }

    // -------- Layer / helpers --------

    private static PageLayer parseLayer(String s) {
        return switch (s.toUpperCase(java.util.Locale.ROOT)) {
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

    private static boolean optBool(JsonObject o, String key, boolean dflt) {
        if (!o.has(key) || o.get(key).isJsonNull()) return dflt;
        return o.get(key).getAsBoolean();
    }
}