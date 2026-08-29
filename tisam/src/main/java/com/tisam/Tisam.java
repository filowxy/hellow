package com.tisam;

import com.tisam.client.TisamClientEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = TisamConstants.MOD_ID, dist = Dist.CLIENT)
public class Tisam {
    public Tisam(IEventBus modBus, ModContainer modContainer) {
        TisamConstants.LOG.info("[{}] TISAM initialized on client bus.", TisamConstants.MOD_ID);
        // Title-screen button + first prompt are driven by client events
        // (ScreenEvent.Init.Post → button, ClientTick → deferred prompt) registered in
        // TisamClientEvents.TitleEvents — no setup needed here.
    }
}
