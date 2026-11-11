package com.hellow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HellowConstants {
    private HellowConstants() {}

    public static final String MOD_ID = "hellow";

    public static final String MOD_NAME = "Hellow";

    private static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    public static Logger log() {
        return LOG;
    }
}
