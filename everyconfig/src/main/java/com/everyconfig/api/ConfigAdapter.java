package com.everyconfig.api;

import net.minecraft.client.gui.screens.Screen;

/**
 * A single config-library adapter: knows whether the library mod is installed at
 * runtime and builds a {@link Screen} for it.
 *
 * <p>Soft-dependency contract: implementations may reference the library's classes
 * directly, but must never touch them unless {@link #isInstalled()} has returned
 * {@code true}. The JVM verifies/loads each method lazily, so keeping the library
 * classes inside {@link #createScreen} and calling it only after the guard keeps a
 * missing-library install from crashing.
 */
public interface ConfigAdapter {
    /** NeoForge mod id of the underlying library (e.g. {@code "cloth_config"}). */
    String libraryId();

    /** Human-readable library name for the placeholder / demo. */
    String displayName();

    /** True when the library mod is present at runtime. */
    boolean isInstalled();

    /** Build a fresh config screen parented to {@code parent}; only call when installed. */
    Screen createScreen(Screen parent);
}
