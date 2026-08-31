package com.everyconfig;

import com.everyconfig.api.ConfigAdapter;
import com.everyconfig.api.ConfigAdapterRegistry;
import com.everyconfig.embed.ConfigEmbedElement;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hellow.element.Element;
import com.hellow.element.ElementRegistry;
import com.hellow.element.ElementSpec;
import com.hellow.element.Elements;
import com.hellow.element.ScreenEmbedElement;
import com.hellow.layout.Spacing;
import com.hellow.pages.PageFileLoader;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * EveryConfig — embeds third-party config-library screens into hellow pages.
 *
 * <p>The public surface is the static {@code embed} family: callers hand back an
 * {@link Element} (a {@link ScreenEmbedElement} wrapping the library's config
 * screen, or a "not installed" placeholder when the library mod is missing) that
 * slots into any hellow layout tree. Which library is used is decided by the
 * adapters registered in {@link ConfigAdapterRegistry} (soft dependencies).
 *
 * <p>Client-only and command-free: the config screens are surfaced through hellow's
 * hand-written page system. This mod registers a {@code configEmbed} element factory
 * into {@link ElementRegistry} (the page JSON type {@code {"type": "configEmbed"}}),
 * so a file under {@code config/hellow/pages/} can embed any installed config
 * library's screen as a normal layout element. There is no /command to open it —
 * pages and their triggers declare when to show.
 */
@Mod(value = EveryConfigConstants.MOD_ID, dist = Dist.CLIENT)
public final class EveryConfig {
    private static final Logger LOG = LoggerFactory.getLogger(EveryConfigConstants.MOD_ID);

    // Placeholder colors (grey, less prominent than live content).
    private static final int PH_TITLE_COLOR = 0xFFAAAAAA;
    private static final int PH_HINT_COLOR = 0xFF777777;

    public EveryConfig(IEventBus modBus, ModContainer modContainer) {
        ElementRegistry.register("configEmbed", EveryConfig::configEmbed);
        LOG.info("[{}] EveryConfig initialized on client bus; 'configEmbed' element registered.",
                EveryConfigConstants.MOD_ID);
        // hellow parses config/hellow/pages during its own mod construction; because
        // this mod loads after hellow (dependency order), pages referencing
        // configEmbed could not parse yet. Re-run the loader now that the factory is
        // registered so those pages load and their triggers hook (the loader hooks
        // triggers per page id, see PageFileLoader).
        PageFileLoader.load();
    }

    // -------- Custom hand-written-page element: configEmbed --------

    /**
     * Factory for the page-JSON element type {@code configEmbed}. Supported rules:
     * <ul>
     *   <li>{@code library} — exact adapter id ({@code cloth_config},
     *       {@code yet_another_config_lib_v3}) or {@code auto}/{@code first} to embed
     *       the first installed library.</li>
     *   <li>{@code libraries} — array of ids in preference order; the first installed
     *       is embedded (overrides {@code library}).</li>
     *   <li>{@code height} — embed height hint; default
     *       {@link EveryConfigConstants#DEFAULT_EMBED_HEIGHT}.</li>
     * </ul>
     */
    private static Element configEmbed(ElementSpec spec) {
        if (!(spec.args() instanceof JsonObject o)) {
            throw new IllegalArgumentException("configEmbed expects a JSON object as its page-file declaration");
        }
        String library = o.has("library") && !o.get("library").isJsonNull()
                ? o.get("library").getAsString() : null;
        List<String> preference = null;
        if (o.has("libraries") && !o.get("libraries").isJsonNull()) {
            preference = new ArrayList<>();
            for (JsonElement e : o.getAsJsonArray("libraries")) preference.add(e.getAsString());
        }
        int height = o.has("height") && !o.get("height").isJsonNull()
                ? o.get("height").getAsInt() : EveryConfigConstants.DEFAULT_EMBED_HEIGHT;
        return new ConfigEmbedElement(library, preference, height);
    }

    // -------- Public facade --------

    /** Embed a specific library's config screen; placeholder when absent/not installed. */
    public static Element embed(String libraryId, Screen parent) {
        return embed(libraryId, parent, EveryConfigConstants.DEFAULT_EMBED_HEIGHT);
    }

    /** {@link #embed(String, Screen)} with an explicit height hint. */
    public static Element embed(String libraryId, Screen parent, int height) {
        ConfigAdapter adapter = ConfigAdapterRegistry.get(libraryId).orElse(null);
        if (adapter == null) return placeholder(libraryId, height);
        if (!adapter.isInstalled()) return placeholder(adapter.displayName(), height);
        return new ScreenEmbedElement(adapter.createScreen(parent), height);
    }

    /** Embed the first installed library's config screen (registration order). */
    public static Element embedFirst(Screen parent) {
        return embedFirst(parent, EveryConfigConstants.DEFAULT_EMBED_HEIGHT);
    }

    /** {@link #embedFirst(Screen)} with an explicit height hint. */
    public static Element embedFirst(Screen parent, int height) {
        return ConfigAdapterRegistry.firstInstalled()
                .map(a -> (Element) new ScreenEmbedElement(a.createScreen(parent), height))
                .orElseGet(() -> placeholder("No config library", height));
    }

    /**
     * Embed the first installed library among {@code libraryIds} (preference order);
     * placeholder when none of them is installed.
     */
    public static Element embedFirstInstalledIn(List<String> libraryIds, Screen parent, int height) {
        return ConfigAdapterRegistry.firstInstalledIn(libraryIds)
                .map(a -> (Element) new ScreenEmbedElement(a.createScreen(parent), height))
                .orElseGet(() -> placeholder(String.join(", ", libraryIds), height));
    }

    /** Placeholder shown when a config library is not installed (never crashes). */
    static Element placeholder(String what, int height) {
        return Elements.box(
                Elements.column(4,
                        Elements.text(what + " is not installed.", PH_TITLE_COLOR),
                        Elements.text("Install the library mod to embed its config screen.", PH_HINT_COLOR)),
                Spacing.all(8));
    }
}