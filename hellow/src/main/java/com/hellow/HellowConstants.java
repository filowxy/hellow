package com.hellow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hellow-wide constants and the SLF4J logger.
 *
 * <p>Modeled after TISAM's {@code TisamConstants}: a single home for
 * the mod id and the logger so individual classes don't each create
 * their own. Anything that needs the mod id or the logger pulls it
 * from here.
 */
public final class HellowConstants {
    private HellowConstants() {}

    /** Matches the package + jar artifact name. */
    public static final String MOD_ID = "hellow";

    /** Human-readable mod name, for log lines and (eventually) mods.toml. */
    public static final String MOD_NAME = "Hellow";

    private static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    /** Shared logger for the library. */
    public static Logger log() {
        return LOG;
    }
}
