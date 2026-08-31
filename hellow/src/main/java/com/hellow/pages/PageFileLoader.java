package com.hellow.pages;

import com.hellow.Hellow;
import com.hellow.HellowConstants;
import com.hellow.HellowPages;
import com.hellow.debug.HellowDebug;
import com.hellow.page.PageScreen;
import com.hellow.trigger.FireStrategy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Loads {@code config/hellow/pages/*.json} files into registered pages plus their
 * triggers, mirroring what a Java caller would build with
 * {@code HellowPages.register} + {@code Hellow.when*}. One bad file is reported to
 * the debug facility and skipped — it never wipes the pages that parsed fine.
 *
 * <p>Triggers are hooked once per page id: the first time a file under a given id
 * parses successfully its trigger is registered; re-loading the same id replaces the
 * page body without adding duplicate triggers. This also lets a mod that registers
 * custom element factories AFTER hellow (e.g. everyconfig's {@code configEmbed})
 * re-run {@link #load()} so its pages parse and their triggers hook correctly.
 */
public final class PageFileLoader {

    private static final String OWNER = "hellow:pages";

    private PageFileLoader() {}

    /** Page ids whose trigger has already been hooked into Hellow. */
    private static final java.util.Set<String> HOOKED_IDS =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static Path pagesDir() {
        return FMLPaths.CONFIGDIR.get().resolve("hellow/pages");
    }

    /** Scan the pages directory and register every valid file. Returns load count. */
    public static int load() {
        Path dir = pagesDir();
        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted().forEach(files::add);
        } catch (IOException e) {
            // Directory missing is expected on first launch.
            HellowConstants.log().info("[hellow] no pages directory at {}", dir);
            return 0;
        }

        List<PageFileDef> ok = new ArrayList<>();
        for (Path file : files) {
            try {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                PageFileDef def = PageFileParser.parse(json, file.getFileName().toString());
                HellowPages.register(def.page());
                ok.add(def);
                HellowConstants.log().info("[hellow] loaded page '{}' from {}", def.page().id(), file.getFileName());
            } catch (Exception e) {
                HellowDebug.capture("pages:load:" + file.getFileName(), e, PageFileLoader.class);
                HellowConstants.log().error("[hellow] failed to load page file {}", file.getFileName(), e);
            }
        }

        for (PageFileDef def : ok) {
            if (HOOKED_IDS.add(def.page().id().value())) hookTrigger(def);
        }
        return ok.size();
    }

    private static void hookTrigger(PageFileDef def) {
        if (!def.hasTrigger()) return;
        FireStrategy strategy = strategy(def.strategy());
        if (def.triggerScreen() != null) {
            String target = def.triggerScreen();
            Hellow.when(OWNER, s -> s != null && s.getClass().getName().equals(target),
                    s -> pageScreen(def), strategy);
        } else if (def.triggerEvent() != null) {
            try {
                Class<?> type = Class.forName(def.triggerEvent());
                Hellow.whenEvent(OWNER, type.asSubclass(Event.class),
                        e -> true, e -> pageScreen(def), strategy);
            } catch (ClassNotFoundException e) {
                HellowDebug.capture("pages:trigger:" + def.triggerEvent(), e, PageFileLoader.class);
                HellowConstants.log().warn("[hellow] page '{}' references unknown event class '{}'",
                        def.page().id(), def.triggerEvent());
            }
        }
    }

    private static Screen pageScreen(PageFileDef def) {
        return new PageScreen(def.page(), Minecraft.getInstance().screen);
    }

    private static FireStrategy strategy(String s) {
        if (s != null && s.equalsIgnoreCase("everyTime")) return FireStrategy.everyTime();
        return FireStrategy.once();
    }
}