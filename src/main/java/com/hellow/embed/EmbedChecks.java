package com.hellow.embed;

import java.util.Set;

public final class EmbedChecks {
    private EmbedChecks() {}

    public static boolean isProxyTarget(Object newScreen, Set<?> proxies) {
        return newScreen != null && proxies != null && proxies.contains(newScreen);
    }

    public static boolean resurfaceNeeded(Object host, Object current) {
        return host != null && current != host;
    }
}
