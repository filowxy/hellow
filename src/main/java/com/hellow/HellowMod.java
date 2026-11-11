package com.hellow;

import com.hellow.debug.DebugScreen;
import com.hellow.page.HellowRegistry;
import com.hellow.page.Page;
import com.hellow.page.PageScheduler;
import com.hellow.page.PageScreen;
import com.hellow.page.PageSpec;
import com.hellow.pages.PageFileLoader;
import com.hellow.trigger.FireStrategy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
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

        @SubscribeEvent
        public static void onScreenOpen(ScreenEvent.Opening event) {
            Screen opened = event.getScreen();
            if (event.isCanceled() || opened instanceof com.hellow.embed.EmbedProxyScreen) return;
            if (Minecraft.getInstance().screen instanceof PageScreen) return;
            HellowConstants.log().debug("[hellow] ScreenEvent.Opening fired for {}",
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

        private static boolean autoDebugOpened;

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            if (autoDebugOpened || !HellowConfig.get().debug()) return;
            Minecraft mc = Minecraft.getInstance();
            Screen current = mc.screen;
            if (current == null) return;
            if (current instanceof PageScreen || current instanceof DebugScreen) return;
            autoDebugOpened = true;
            HellowConstants.log().info("[hellow] debug=true: opening the debug report over {}",
                    current.getClass().getSimpleName());
            mc.setScreen(new DebugScreen(current));
        }
    }
}
