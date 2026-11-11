package com.hellow.embed;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbedChecksTest {

    private static Set<Object> proxySet(Object... proxies) {
        Set<Object> set = Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        Collections.addAll(set, proxies);
        return set;
    }

    @Test
    void proxyTargetRecognisedByIdentity() {
        Object proxy = new Object();
        assertTrue(EmbedChecks.isProxyTarget(proxy, proxySet(proxy)));
    }

    @Test
    void equalButDistinctObjectIsNotAProxyTarget() {
        String proxy = new String("screen");
        String lookalike = new String("screen");
        assertFalse(EmbedChecks.isProxyTarget(lookalike, proxySet(proxy)));
    }

    @Test
    void nullTargetIsNeverAProxy() {
        assertFalse(EmbedChecks.isProxyTarget(null, proxySet(new Object())));
    }

    @Test
    void emptyProxySetNeverMatches() {
        assertFalse(EmbedChecks.isProxyTarget(new Object(), Set.of()));
    }

    @Test
    void nullProxySetNeverMatches() {
        assertFalse(EmbedChecks.isProxyTarget(new Object(), null));
    }

    @Test
    void noResurfaceWhileHostIsCurrent() {
        Object host = new Object();
        assertFalse(EmbedChecks.resurfaceNeeded(host, host));
    }

    @Test
    void resurfaceWhenHostWasReplaced() {
        Object host = new Object();
        assertTrue(EmbedChecks.resurfaceNeeded(host, new Object()));
    }

    @Test
    void noResurfaceWithoutAHost() {
        assertFalse(EmbedChecks.resurfaceNeeded(null, new Object()));
        assertFalse(EmbedChecks.resurfaceNeeded(null, null));
    }
}
