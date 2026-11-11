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

public final class PageFileLoader {

    private static final String OWNER = "hellow:pages";

    private PageFileLoader() {}

    public static Path pagesDir() {
        return FMLPaths.CONFIGDIR.get().resolve("hellow/pages");
    }

    public static int load() {
        Hellow.removeRules(OWNER);
        Path dir = pagesDir();
        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> {
                        String name = p.getFileName().toString();
                        return name.endsWith(".json") || name.endsWith(".hellow");
                    })
                    .sorted().forEach(files::add);
        } catch (IOException e) {
            HellowConstants.log().info("[hellow] no pages directory at {}", dir);
            return 0;
        }

        List<PageFileDef> ok = new ArrayList<>();
        for (Path file : files) {
            try {
                String name = file.getFileName().toString();
                PageFileDef def = name.endsWith(".hellow")
                        ? HellowDsl.parseFile(file)
                        : PageFileParser.parse(Files.readString(file, StandardCharsets.UTF_8), name);
                HellowPages.register(def.page());
                ok.add(def);
                HellowConstants.log().info("[hellow] loaded page '{}' from {}", def.page().id(), name);
            } catch (Exception e) {
                HellowDebug.capture("pages:load:" + file.getFileName(), e, PageFileLoader.class);
                HellowConstants.log().error("[hellow] failed to load page file {}", file.getFileName(), e);
            }
        }

        for (PageFileDef def : ok) {
            hookTrigger(def);
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
