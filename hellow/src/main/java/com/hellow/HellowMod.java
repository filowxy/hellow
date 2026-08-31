package com.hellow;

import com.hellow.debug.HellowDebug;
import com.hellow.page.HellowRegistry;
import com.hellow.page.Page;
import com.hellow.page.PageScheduler;
import com.hellow.page.PageSpec;
import com.hellow.pages.PageFileLoader;
import com.hellow.trigger.FireStrategy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@Mod(HellowConstants.MOD_ID)
public final class HellowMod {
    public HellowMod() {
        HellowConstants.log().info("[hellow] declarative page framework initialized");
        PageFileLoader.load();
    }

    @EventBusSubscriber(modid = HellowConstants.MOD_ID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {}

        /**
         * Screen is about to open: let explicit caller rules ({@link Hellow#when})
         * decide first, then fall back to legacy auto-open pages registered with a
         * {@link PageSpec#condition()}.
         */
        @SubscribeEvent
        public static void onScreenOpen(ScreenEvent.Opening event) {
            Screen opened = event.getScreen();
            HellowConstants.log().info("[hellow] ScreenEvent.Opening fired for {}",
                    opened == null ? "null" : opened.getClass().getName());
            if (opened == null) return;
            if (Hellow.dispatchScreenOpen(opened) != null) return;
            Page match = findMatch(opened);
            if (match != null) PageScheduler.show(match, opened);
        }

        private static Page findMatch(Screen screen) {
            for (Page page : HellowRegistry.all()) {
                if (!(page instanceof PageSpec spec) || spec.condition() == null) continue;
                FireStrategy strategy = spec.fireStrategy();
                if (strategy == null) strategy = FireStrategy.everyTime();
                if (!spec.condition().test(screen)) continue;
                strategy.onConditionMatch();
                if (strategy.shouldFire()) {
                    strategy.markFired();
                    return page;
                }
            }
            return null;
        }

        /** {@code /hellow debug} / {@code /hellow pages reload}. */
        @SubscribeEvent
        public static void onRegisterCommands(RegisterClientCommandsEvent event) {
            event.getDispatcher().register(Commands.literal("hellow")
                    .then(Commands.literal("debug").executes(ctx -> {
                        HellowDebug.openDebug();
                        return 1;
                    }))
                    .then(Commands.literal("pages")
                            .then(Commands.literal("reload").executes(ctx -> {
                                int n = PageFileLoader.load();
                                HellowConstants.log().info("[hellow] pages reloaded: {}", n);
                                return 1;
                            }))));
        }
    }
}
