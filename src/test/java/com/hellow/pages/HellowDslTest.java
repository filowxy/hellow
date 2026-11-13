package com.hellow.pages;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hellow.pages.HellowDsl.DslNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HellowDslTest {

    private static final String SAMPLE = """
            # a demo page written in the .hellow DSL
            page demo "My Demo" layer=MODAL theme=demo_dark
              trigger screen=net.minecraft.client.gui.screens.TitleScreen strategy=once
              column gap=6
                text "Hello, world!"
                heading "Section"
                divider
                row gap=4 align=CENTER_MIDDLE
                  button "Close" onPress=closeTop
                  fillSpacer
                  link "Docs" url=https://example.com/docs
                wrappedText "Long text that wraps" color=0xFFAAAAAA
                progressBar "Progress" value=50 max=100
                scroll viewportHeight=120 gap=4
                  text "row one"
                  text "row two"
            """;

    private static DslNode sample() {
        return HellowDsl.parse(SAMPLE);
    }

    @Test
    void parsesRootPageWithIdTitleAndLayer() {
        DslNode root = sample();
        assertEquals("page", root.type());
        assertEquals(2, root.args().size());
        assertEquals("demo", root.args().get(0));
        assertEquals("My Demo", root.args().get(1));
        assertEquals("MODAL", root.attr("layer", "NORMAL"));
    }

    @Test
    void parsesTriggerAsIndentedChild() {
        DslNode root = sample();
        assertEquals(2, root.children().size());
        DslNode trigger = root.children().get(0);
        assertEquals("trigger", trigger.type());
        assertEquals("net.minecraft.client.gui.screens.TitleScreen",
                trigger.attr("screen", null));
        assertEquals("once", trigger.attr("strategy", null));
    }

    @Test
    void buildsNestedElementTree() {
        DslNode column = sample().children().get(1);
        assertEquals("column", column.type());
        assertEquals(7, column.children().size());

        DslNode row = column.children().get(3);
        assertEquals("row", row.type());
        assertEquals("CENTER_MIDDLE", row.attr("align", null));
        assertEquals(3, row.children().size());

        DslNode scroll = column.children().get(6);
        assertEquals("scroll", scroll.type());
        assertEquals("120", scroll.attr("viewportHeight", null));
        assertEquals(2, scroll.children().size());
    }

    @Test
    void keepsHashInsideQuotedStrings() {
        DslNode root = HellowDsl.parse("page p \"T\"\n  text \"a # b\"\n");
        DslNode text = root.children().get(0);
        assertEquals("a # b", text.args().get(0));
    }

    @Test
    void parsesAttachedQuotedAttrValue() {
        DslNode root = HellowDsl.parse("page p \"T\"\n  button \"Go\" tooltip=\"Press me\"\n");
        DslNode button = root.children().get(0);
        assertEquals("Go", button.args().get(0));
        assertEquals("Press me", button.attr("tooltip", null));
    }

    @Test
    void convertsToPageJson() {
        JsonObject page = HellowDsl.toPageJson(sample());
        assertEquals("demo", page.get("id").getAsString());
        assertEquals("My Demo", page.get("title").getAsString());
        assertEquals("MODAL", page.get("layer").getAsString());
        assertEquals("demo_dark", page.get("theme").getAsString());

        JsonObject trigger = page.getAsJsonObject("trigger");
        assertEquals("screen", trigger.get("type").getAsString());

        JsonObject body = page.getAsJsonObject("body");
        assertEquals("column", body.get("type").getAsString());
        JsonArray kids = body.getAsJsonArray("children");
        assertEquals(7, kids.size());
        assertEquals("Hello, world!", kids.get(0).getAsJsonObject().get("text").getAsString());
        assertEquals("divider", kids.get(2).getAsJsonObject().get("type").getAsString());
        assertEquals("row", kids.get(3).getAsJsonObject().get("type").getAsString());
        assertEquals("CENTER_MIDDLE", kids.get(3).getAsJsonObject().get("align").getAsString());
        assertEquals("closeTop", kids.get(3).getAsJsonObject().getAsJsonArray("children").get(0)
                .getAsJsonObject().get("onPress").getAsString());
    }

    @Test
    void rejectsSecondRootNode() {
        assertThrows(IllegalArgumentException.class,
                () -> HellowDsl.parse("page a \"A\"\npage b \"B\"\n"));
    }

    @Test
    void rejectsRootWithoutBody() {
        assertThrows(IllegalArgumentException.class,
                () -> HellowDsl.parse("page a \"A\"\n"));
    }

    @Test
    void rejectsUnterminatedString() {
        assertThrows(IllegalArgumentException.class,
                () -> HellowDsl.parse("page a \"A\n  text \"x\"\n"));
    }

    @Test
    void ignoresBlankLinesAndComments() {
        DslNode root = HellowDsl.parse("# leading comment\n\npage a \"A\"\n\n  # inner\n  text \"x\"\n\n");
        assertEquals(1, root.children().size());
        DslNode text = root.children().get(0);
        assertEquals("text", text.type());
        assertTrue(text.args().contains("x"));
    }
}
