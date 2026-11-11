package com.hellow.embed;

import com.hellow.HellowConstants;
import com.hellow.debug.HellowDebug;
import com.hellow.page.Page;
import com.hellow.page.PageScheduler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public final class EmbedNavigation {

    public interface Sink {
        void onReturnRequested();

        default boolean allowExit() { return false; }

        Screen host();

        Page page();

        Screen hostParent();

        Screen embeddedScreen();
    }

    private static final Set<EmbedProxyScreen> PROXIES =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private static final Map<Screen, Sink> EMBEDDED =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private EmbedNavigation() {}

    public static void trackEmbedded(Screen embedded, Sink owner) {
        if (embedded != null && owner != null) EMBEDDED.put(embedded, owner);
    }

    public static void untrackEmbedded(Screen embedded) {
        if (embedded != null) EMBEDDED.remove(embedded);
    }

    public static Screen createProxy(Sink owner) {
        if (owner == null) throw new IllegalArgumentException("proxy owner required");
        EmbedProxyScreen proxy = new EmbedProxyScreen(owner);
        PROXIES.add(proxy);
        return proxy;
    }

    public static void release(Screen proxy) {
        if (proxy != null) PROXIES.remove(proxy);
    }

    private static Sink embeddedOwnerOf(Screen target) {
        return target == null ? null : EMBEDDED.get(target);
    }

    @EventBusSubscriber(modid = HellowConstants.MOD_ID, value = Dist.CLIENT)
    static final class Events {
        private Events() {}

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onOpening(ScreenEvent.Opening event) {
            Screen target = event.getNewScreen();
            if (!EmbedChecks.isProxyTarget(target, PROXIES)) {
                Sink promoted = embeddedOwnerOf(target);
                if (promoted != null) {
                    event.setCanceled(true);
                    if (EmbedChecks.resurfaceNeeded(promoted.host(), Minecraft.getInstance().screen)) {
                        resurface(promoted);
                    }
                }
                return;
            }
            Sink owner = ((EmbedProxyScreen) target).owner();
            if (owner.allowExit()) return;
            event.setCanceled(true);
            try {
                owner.onReturnRequested();
            } catch (Throwable t) {
                HellowDebug.capture("embed.onReturnRequested", t, owner);
            }
            if (EmbedChecks.resurfaceNeeded(owner.host(), Minecraft.getInstance().screen)) {
                resurface(owner);
            }
        }
    }

    static void resurface(Sink owner) {
        if (owner == null) return;
        Page page = owner.page();
        if (page == null) return;
        try {
            PageScheduler.show(page, owner.hostParent());
        } catch (Throwable t) {
            HellowDebug.capture("embed.resurface", t, owner);
        }
    }
}
