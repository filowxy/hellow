package com.hellow.pages;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HellowDsl {

    private HellowDsl() {}

    public record DslNode(String type, List<String> args, Map<String, String> attrs, List<DslNode> children) {

        public String attr(String key, String dflt) {
            return this.attrs.getOrDefault(key, dflt);
        }

        public boolean hasAttr(String key) {
            return this.attrs.containsKey(key);
        }
    }

    private record Line(int indent, String body, int number) {}

    private record Frame(DslNode node, int indent) {}

    private record Tok(String key, String value, boolean attr) {}

    private static final Map<String, String> PRIMARY_FIELD = Map.ofEntries(
            Map.entry("text", "text"),
            Map.entry("wrappedText", "text"),
            Map.entry("clampedText", "text"),
            Map.entry("heading", "text"),
            Map.entry("button", "text"),
            Map.entry("link", "text"),
            Map.entry("toggle", "text"),
            Map.entry("slider", "text"),
            Map.entry("textInput", "label"),
            Map.entry("enumSelect", "text"),
            Map.entry("progressBar", "text"),
            Map.entry("expandableList", "header")
    );

    public static DslNode parse(String source) {
        List<Line> lines = new ArrayList<>();
        int number = 0;
        for (String raw : source.split("\n")) {
            number++;
            String content = expandTabs(stripComment(raw.replace("\r", "")));
            if (content.isBlank()) continue;
            int indent = 0;
            while (indent < content.length() && content.charAt(indent) == ' ') indent++;
            String body = content.substring(indent).strip();
            if (!body.isEmpty()) lines.add(new Line(indent, body, number));
        }
        if (lines.isEmpty()) throw new IllegalArgumentException("empty .hellow document");

        Deque<Frame> stack = new ArrayDeque<>();
        DslNode root = null;
        for (Line line : lines) {
            DslNode node = parseLine(line.body(), line.number());
            while (!stack.isEmpty() && line.indent() <= stack.peek().indent()) stack.pop();
            if (stack.isEmpty()) {
                if (root != null) {
                    throw new IllegalArgumentException("line " + line.number()
                            + ": unexpected second root node '" + node.type()
                            + "' (a trigger line must be indented under its page)");
                }
                root = node;
            } else {
                stack.peek().node().children().add(node);
            }
            stack.push(new Frame(node, line.indent()));
        }
        return root;
    }

    public static JsonObject toJson(DslNode node) {
        JsonObject o = new JsonObject();
        o.addProperty("type", node.type());
        String primary = PRIMARY_FIELD.get(node.type());
        if (primary != null && !node.args().isEmpty()) {
            o.addProperty(primary, node.args().get(0));
        } else if (!node.args().isEmpty()) {
            JsonArray arr = new JsonArray();
            for (String arg : node.args()) arr.add(arg);
            o.add("args", arr);
        }
        for (Map.Entry<String, String> e : node.attrs().entrySet()) {
            o.addProperty(e.getKey(), e.getValue());
        }
        if (!node.children().isEmpty()) {
            JsonArray arr = new JsonArray();
            for (DslNode child : node.children()) arr.add(toJson(child));
            o.add("children", arr);
        }
        return o;
    }

    public static JsonObject toPageJson(DslNode root) {
        if (!"page".equals(root.type())) {
            throw new IllegalArgumentException("root node must be 'page', got '" + root.type() + "'");
        }
        List<String> args = root.args();
        if (args.isEmpty()) throw new IllegalArgumentException("page node needs an id");
        JsonObject page = new JsonObject();
        page.addProperty("id", args.get(0));
        page.addProperty("title", args.size() > 1 ? args.get(1) : args.get(0));
        page.addProperty("layer", root.attr("layer", "NORMAL"));
        for (Map.Entry<String, String> e : root.attrs().entrySet()) {
            if (!e.getKey().equals("layer")) page.addProperty(e.getKey(), e.getValue());
        }
        DslNode body = null;
        DslNode trigger = null;
        for (DslNode child : root.children()) {
            if ("trigger".equals(child.type())) {
                if (trigger != null) throw new IllegalArgumentException("page accepts at most one trigger");
                trigger = child;
            } else if (body == null) {
                body = child;
            } else {
                throw new IllegalArgumentException("page accepts exactly one body element");
            }
        }
        if (body == null) throw new IllegalArgumentException("page needs a body element");
        page.add("body", toJson(body));
        if (trigger != null) page.add("trigger", triggerJson(trigger));
        return page;
    }

    public static PageFileDef parse(String text, String source) {
        return PageFileParser.parse(toPageJson(parse(text)).toString(), source);
    }

    public static PageFileDef parseFile(Path file) throws IOException {
        return parse(Files.readString(file, StandardCharsets.UTF_8), file.getFileName().toString());
    }

    private static JsonObject triggerJson(DslNode t) {
        JsonObject o = new JsonObject();
        o.addProperty("type", t.hasAttr("event") ? "event" : "screen");
        for (Map.Entry<String, String> e : t.attrs().entrySet()) {
            o.addProperty(e.getKey(), e.getValue());
        }
        return o;
    }

    private static DslNode parseLine(String body, int number) {
        List<Tok> tokens = tokenize(body, number);
        List<String> args = new ArrayList<>();
        Map<String, String> attrs = new LinkedHashMap<>();
        String type = null;
        for (Tok tok : tokens) {
            if (tok.attr()) {
                attrs.put(tok.key(), tok.value());
            } else if (type == null) {
                type = tok.value();
            } else {
                args.add(tok.value());
            }
        }
        if (type == null) {
            throw new IllegalArgumentException("line " + number + ": missing element type");
        }
        return new DslNode(type, args, attrs, new ArrayList<>());
    }

    private static List<Tok> tokenize(String body, int number) {
        List<Tok> out = new ArrayList<>();
        int i = 0;
        int n = body.length();
        while (i < n) {
            while (i < n && body.charAt(i) == ' ') i++;
            if (i >= n) break;
            if (body.charAt(i) == '"') {
                out.add(new Tok(null, readQuoted(body, i, number), false));
                i = advancePastQuoted(body, i);
            } else {
                int start = i;
                while (i < n && body.charAt(i) != ' ' && body.charAt(i) != '"') i++;
                String head = body.substring(start, i);
                if (i < n && body.charAt(i) == '"' && head.endsWith("=")) {
                    String key = head.substring(0, head.length() - 1);
                    out.add(new Tok(key, readQuoted(body, i, number), true));
                    i = advancePastQuoted(body, i);
                } else {
                    int eq = head.indexOf('=');
                    if (eq > 0) {
                        out.add(new Tok(head.substring(0, eq), head.substring(eq + 1), true));
                    } else {
                        out.add(new Tok(null, head, false));
                    }
                }
            }
        }
        return out;
    }

    private static String readQuoted(String body, int start, int number) {
        int i = start + 1;
        int n = body.length();
        StringBuilder sb = new StringBuilder();
        while (i < n && body.charAt(i) != '"') {
            if (body.charAt(i) == '\\' && i + 1 < n) {
                char next = body.charAt(i + 1);
                if (next == '"' || next == '\\') {
                    sb.append(next);
                    i += 2;
                    continue;
                }
            }
            sb.append(body.charAt(i));
            i++;
        }
        if (i >= n) throw new IllegalArgumentException("line " + number + ": unterminated quoted string");
        return sb.toString();
    }

    private static int advancePastQuoted(String body, int start) {
        int i = start + 1;
        int n = body.length();
        while (i < n && body.charAt(i) != '"') {
            if (body.charAt(i) == '\\' && i + 1 < n) i += 2;
            else i++;
        }
        return i + 1;
    }

    private static String stripComment(String line) {
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) quoted = !quoted;
            else if (c == '#' && !quoted) return line.substring(0, i);
        }
        return line;
    }

    private static String expandTabs(String line) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\t') sb.append("  ");
            else if (c == ' ') sb.append(c);
            else {
                sb.append(line.substring(i));
                break;
            }
        }
        return sb.toString();
    }
}
