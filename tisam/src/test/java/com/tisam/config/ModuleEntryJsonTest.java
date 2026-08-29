package com.tisam.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runnable checks for the lenient ModuleEntry JSON deserializer (ConfigManager's
 * Gson). A modpack author typo in "type"/"priority" must degrade gracefully
 * instead of throwing and quarantining the whole modules.json.
 */
class ModuleEntryJsonTest {
    private final Gson gson = ModuleEntry.lenientGson();

    private ModuleEntry entry(String json) {
        return gson.fromJson(json, ModulesConfig.class).modules.get(0);
    }

    @Test
    void typoTypeDegradesToMod() {
        assertEquals(ModuleEntry.ContentType.MOD,
                entry("{\"modules\":[{\"id\":\"a\",\"name\":\"A\",\"type\":\"datapackx\"}]}").type);
    }

    @Test
    void typoPriorityDegradesToNormal() {
        assertEquals(ModuleEntry.Priority.NORMAL,
                entry("{\"modules\":[{\"id\":\"a\",\"name\":\"A\",\"priority\":\"\u5fc5\u88c5\"}]}").priority);
    }

    @Test
    void nullTypeAndPriorityDefault() {
        ModuleEntry e = entry("{\"modules\":[{\"id\":\"a\",\"name\":\"A\",\"type\":null,\"priority\":null}]}");
        assertEquals(ModuleEntry.ContentType.MOD, e.type);
        assertEquals(ModuleEntry.Priority.NORMAL, e.priority);
    }

    @Test
    void validValuesStillParse() {
        ModuleEntry e = entry("{\"modules\":[{\"id\":\"a\",\"name\":\"A\",\"type\":\"datapack\",\"priority\":\"critical\"}]}");
        assertEquals(ModuleEntry.ContentType.DATAPACK, e.type);
        assertEquals(ModuleEntry.Priority.CRITICAL, e.priority);
    }

    @Test
    void prematureJsonErrorStillFails() {
        // A structurally broken file must still surface as a parse error (ConfigManager
        // then quarantines it) — only enum typos are forgiven, not invalid JSON.
        boolean thrown = false;
        try {
            gson.fromJson("{\"modules\":[{", ModulesConfig.class);
        } catch (com.google.gson.JsonSyntaxException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    @Test
    void fieldDefaultsPreservedWhenKeysMissing() {
        ModuleEntry e = entry("{\"modules\":[{\"id\":\"a\"}]}");
        assertEquals("", e.description);
        assertEquals("ARR", e.license);
        assertTrue(e.canIgnore);
        assertEquals(ModuleEntry.ContentType.MOD, e.type);
        assertEquals(ModuleEntry.Priority.NORMAL, e.priority);
    }
}