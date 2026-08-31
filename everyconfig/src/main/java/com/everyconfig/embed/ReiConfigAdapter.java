package com.everyconfig.embed;

import com.everyconfig.api.ConfigAdapter;
import me.shedaniel.rei.api.client.config.ConfigManager;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModList;

/**
 * Adapter for a REAL mod whose config screen is built with Cloth Config:
 * Roughly Enough Items (REI). Unlike {@link ClothConfigAdapter} (which builds a
 * demo screen), this returns REI's actual in-game config screen
 * ({@code ConfigManager#getConfigScreen}) so a hellow page can embed the mod's
 * real settings UI.
 *
 * <p>Enabled at runtime only when the {@code roughlyenoughitems} mod is installed;
 * {@link #createScreen} touches REI classes and is therefore only invoked after
 * {@link #isInstalled()} has returned {@code true} (the JVM loads that method
 * lazily, keeping a missing-library install safe).
 */
public final class ReiConfigAdapter implements ConfigAdapter {
    @Override
    public String libraryId() {
        return "roughlyenoughitems";
    }

    @Override
    public String displayName() {
        return "Roughly Enough Items (REI)";
    }

    @Override
    public boolean isInstalled() {
        return ModList.get().isLoaded(libraryId());
    }

    @Override
    public Screen createScreen(Screen parent) {
        return ConfigManager.getInstance().getConfigScreen(parent);
    }
}
