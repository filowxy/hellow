package com.hellow;

import com.hellow.page.Page;
import com.hellow.page.PageBuilder;
import com.hellow.page.PageLayer;
import com.hellow.page.PageSpec;
import com.hellow.trigger.FireStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runnable checks for the Hellow public facade: the {@code Hellow.FireStrategy} alias
 * (the compile contract TISAM depends on) and the page builders. Kept free of Minecraft
 * runtime classes — the hellow unit-test classpath does not load MC jars.
 */
class HellowApiTest {

    @Test
    void fireStrategyAliasForwardsOnce() {
        FireStrategy s = Hellow.FireStrategy.once();
        assertTrue(s.shouldFire());
        s.markFired();
        assertFalse(s.shouldFire());
    }

    @Test
    void fireStrategyAliasForwardsEveryTime() {
        FireStrategy s = Hellow.FireStrategy.everyTime();
        assertTrue(s.shouldFire());
        s.markFired();
        assertTrue(s.shouldFire());
    }

    @Test
    void fireStrategyAliasForwardsDecaying() {
        FireStrategy s = Hellow.FireStrategy.decaying(new int[]{2});
        assertTrue(s.shouldFire());
        s.markFired();
        s.onConditionMatch();
        assertFalse(s.shouldFire());
        s.onConditionMatch();
        assertTrue(s.shouldFire());
    }

    @Test
    void pageBuilderBuildsSpec() {
        Page page = PageBuilder.of("test").title("key").layer(PageLayer.MODAL).build();
        PageSpec spec = assertInstanceOf(PageSpec.class, page);
        assertEquals("test", spec.id().value());
        assertEquals("key", spec.titleKey());
        assertEquals(PageLayer.MODAL, spec.layer());
        assertNull(spec.body());
    }
}
