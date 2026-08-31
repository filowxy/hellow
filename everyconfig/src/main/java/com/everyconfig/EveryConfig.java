package com.everyconfig;

import com.everyconfig.api.ConfigAdapter;
import com.everyconfig.api.ConfigAdapterRegistry;
import com.hellow.Hellow;
import com.hellow.element.Element;
import com.hellow.element.Elements;
import com.hellow.element.ScreenEmbedElement;
import com.hellow.layout.Spacing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EveryConfig — embeds third-party config-library screens into hellow pages.
 *
 * <p>The public surface is the static {@code embed} family: callers hand back an
 * {@link Element} (a {@link ScreenEmbedElement} wrapping the library's config
 * screen, or a "not installed" placeholder when the library mod is missing) that
 * slots into any hellow layout tree. Which library is used is decided by the
 * adapters registered in {@link ConfigAdapterRegistry} (soft dependencies).
 *
 * <p>{@code /everyconfig demo} opens a hellow dialog embedding the first installed
 * library's config screen for quick in-game verification.
 */
@Mod(value = EveryConfigConstants.MOD_ID, dist = Dist.CLIENT)
public final class EveryConfig {
    private static final Logger LOG = LoggerFactory.getLogger(EveryConfigConstants.MOD_ID);

    // Placeholder colors (grey, less prominent than live content).
    private static final int PH_TITLE_COLOR = 0xFFAAAAAA;
    private static final int PH_HINT_COLOR = 0xFF777777;

    public EveryConfig(IEventBus modBus, ModContainer modContainer) {
        LOG.info("[{}] EveryConfig initialized on client bus.", EveryConfigConstants.MOD_ID);
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

    /** Placeholder shown when a config library is not installed (never crashes). */
    static Element placeholder(String what, int height) {
        return Elements.box(
                Elements.column(4,
                        Elements.text(what + " is not installed.", PH_TITLE_COLOR),
                        Elements.text("Install the library mod to embed its config screen.", PH_HINT_COLOR)),
                Spacing.all(8));
    }

    @EventBusSubscriber(modid = EveryConfigConstants.MOD_ID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {}

        /** {@code /everyconfig demo} — open a dialog embedding the first installed config screen. */
        @SubscribeEvent
        public static void onRegisterCommands(RegisterClientCommandsEvent event) {
            event.getDispatcher().register(Commands.literal("everyconfig")
                    .then(Commands.literal("demo").executes(ctx -> {
                        Minecraft mc = Minecraft.getInstance();
                        Hellow.showDialog(Component.literal("EveryConfig demo"),
                                mc.screen, embedFirst(mc.screen));
                        return 1;
                    })));
        }
    }
}
