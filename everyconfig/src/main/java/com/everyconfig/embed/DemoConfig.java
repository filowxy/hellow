package com.everyconfig.embed;

/**
 * Tiny demo config backing the sample screens the built-in adapters build.
 * Each library's adapter reads/writes these fields so the embedded screen has
 * real, live widgets instead of an empty form.
 *
 * <p>(ponytail: this is a demo holder, not a persisted config. Real integrations
 * replace the adapter's {@code createScreen} with the owning mod's own options.)
 */
public final class DemoConfig {
    private DemoConfig() {}

    public static boolean enabled = true;
    public static int amount = 3;
}
