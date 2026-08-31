package com.everyconfig.embed;

import com.everyconfig.api.ConfigAdapter;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

/**
 * Cloth Config adapter. Enabled at runtime only when the {@code cloth_config}
 * mod is installed; {@link #createScreen} touches Cloth classes and is therefore
 * only invoked after {@link #isInstalled()} has returned {@code true} (the JVM
 * loads that method lazily, keeping a missing-library install safe).
 */
public final class ClothConfigAdapter implements ConfigAdapter {
    @Override
    public String libraryId() {
        return "cloth_config";
    }

    @Override
    public String displayName() {
        return "Cloth Config";
    }

    @Override
    public boolean isInstalled() {
        return ModList.get().isLoaded(libraryId());
    }

    @Override
    public Screen createScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal(displayName() + " (demo)"))
                .setSavingRunnable(() -> { /* demo config is not persisted */ });

        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
        general.addEntry(eb.startBooleanToggle(Component.literal("Enabled"), DemoConfig.enabled)
                .setDefaultValue(true)
                .setSaveConsumer(v -> DemoConfig.enabled = v)
                .build());
        general.addEntry(eb.startIntField(Component.literal("Amount"), DemoConfig.amount)
                .setDefaultValue(3).setMin(0).setMax(10)
                .setSaveConsumer(v -> DemoConfig.amount = v)
                .build());
        return builder.build();
    }
}
