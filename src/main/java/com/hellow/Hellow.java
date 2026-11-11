package com.hellow;

import com.hellow.element.Element;
import com.hellow.page.Page;
import com.hellow.page.PageId;
import com.hellow.page.PageLayer;
import com.hellow.page.PageScheduler;
import com.hellow.page.PageScreen;
import com.hellow.page.PageSpec;
import com.hellow.trigger.Condition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class Hellow {
    private Hellow() {}

    public interface FireStrategy extends com.hellow.trigger.FireStrategy {
        static com.hellow.trigger.FireStrategy once() {
            return com.hellow.trigger.FireStrategy.once();
        }

        static com.hellow.trigger.FireStrategy everyTime() {
            return com.hellow.trigger.FireStrategy.everyTime();
        }

        static com.hellow.trigger.FireStrategy decaying(int[] intervals) {
            return com.hellow.trigger.FireStrategy.decaying(intervals);
        }
    }

    @FunctionalInterface
    public interface ApiSource<T> {
        Optional<T> get();
    }

    public static final int DEFAULT_POLL_INTERVAL_TICKS = 20;

    private record ScreenRule(String owner, Condition condition,
                              Function<Screen, Screen> factory,
                              com.hellow.trigger.FireStrategy strategy) {}

    private record EventRule(String owner, Class<?> eventType,
                             Predicate<Event> test, Function<Event, Screen> factory,
                             com.hellow.trigger.FireStrategy strategy) {}

    private static final class ApiRule {
        final String owner;
        final Supplier<Optional<?>> source;
        final Function<Object, Screen> factory;
        final com.hellow.trigger.FireStrategy strategy;
        final int pollInterval;
        int ticksSincePoll;

        ApiRule(String owner, Supplier<Optional<?>> source, Function<Object, Screen> factory,
                com.hellow.trigger.FireStrategy strategy, int pollInterval) {
            this.owner = owner;
            this.source = source;
            this.factory = factory;
            this.strategy = strategy;
            this.pollInterval = Math.max(1, pollInterval);
        }

        boolean pollDue() {
            this.ticksSincePoll++;
            if (this.ticksSincePoll < this.pollInterval) return false;
            this.ticksSincePoll = 0;
            return true;
        }
    }

    private static final List<ScreenRule> SCREEN_RULES = new CopyOnWriteArrayList<>();
    private static final List<EventRule> EVENT_RULES = new CopyOnWriteArrayList<>();
    private static final List<ApiRule> API_RULES = new CopyOnWriteArrayList<>();

    private static final Map<ScreenRule, Screen> LAST_TRIGGERED_SCREEN =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static final Set<Class<?>> HOOKED_EVENT_TYPES = new CopyOnWriteArraySet<>();

    private static final java.util.concurrent.atomic.AtomicReference<Screen> PENDING_SCREEN =
            new java.util.concurrent.atomic.AtomicReference<>();

    private static volatile boolean TICK_DRAIN_HOOKED = false;

    private static void ensureTickDrainHooked() {
        if (TICK_DRAIN_HOOKED) return;
        TICK_DRAIN_HOOKED = true;
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> drainPendingScreen());
    }

    private static void drainPendingScreen() {
        Screen pending = PENDING_SCREEN.getAndSet(null);
        if (pending == null) return;
        if (pending instanceof PageScreen ps) {
            ps.reparent(Minecraft.getInstance().screen);
        }
        Minecraft.getInstance().setScreen(pending);
    }

    private static void queueScreen(Screen screen) {
        if (screen == null) return;
        PENDING_SCREEN.set(screen);
        ensureTickDrainHooked();
    }

    public static void when(String owner, Condition condition,
                            Function<Screen, Screen> factory,
                            com.hellow.trigger.FireStrategy strategy) {
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(factory, "factory");
        SCREEN_RULES.add(new ScreenRule(owner, condition, factory,
                strategy == null ? FireStrategy.everyTime() : strategy));
        HellowConstants.log().info("[hellow] screen trigger registered for '{}' ({} rules total)",
                owner, SCREEN_RULES.size());
    }

    @SuppressWarnings("unchecked")
    public static <E extends Event> void whenEvent(String owner, Class<E> eventType,
                                                   Predicate<E> test, Function<E, Screen> factory,
                                                   com.hellow.trigger.FireStrategy strategy) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(factory, "factory");
        Predicate<E> testOrTrue = test == null ? e -> true : test;
        EVENT_RULES.add(new EventRule(owner, eventType,
                e -> testOrTrue.test((E) e),
                e -> factory.apply((E) e),
                strategy == null ? FireStrategy.everyTime() : strategy));
        hookGameBus(eventType, e -> dispatchGameEvent(e));
        HellowConstants.log().info("[hellow] NeoForge-event trigger registered for '{}' on {} ({} rules total)",
                owner, eventType.getSimpleName(), EVENT_RULES.size());
    }

    @SuppressWarnings("unchecked")
    private static <E extends Event> void hookGameBus(Class<E> eventType, Consumer<E> handler) {
        if (HOOKED_EVENT_TYPES.add(eventType)) {
            NeoForge.EVENT_BUS.addListener(eventType, handler);
        }
    }

    public static <T> void whenApi(String owner, ApiSource<T> source,
                                   Function<T, Screen> factory,
                                   com.hellow.trigger.FireStrategy strategy) {
        whenApi(owner, source, factory, strategy, DEFAULT_POLL_INTERVAL_TICKS);
    }

    @SuppressWarnings("unchecked")
    public static <T> void whenApi(String owner, ApiSource<T> source,
                                   Function<T, Screen> factory,
                                   com.hellow.trigger.FireStrategy strategy,
                                   int pollIntervalTicks) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(factory, "factory");
        API_RULES.add(new ApiRule(owner,
                () -> (Optional<?>) source.get(),
                content -> factory.apply((T) content),
                strategy == null ? FireStrategy.everyTime() : strategy,
                pollIntervalTicks));
        hookGameBus(ClientTickEvent.Post.class, e -> tickPollers());
        HellowConstants.log().info("[hellow] API trigger registered for '{}' ({} rules total)",
                owner, API_RULES.size());
    }

    public static void clearRules() {
        SCREEN_RULES.clear();
        EVENT_RULES.clear();
        API_RULES.clear();
        LAST_TRIGGERED_SCREEN.clear();
    }

    public static void removeRules(String owner) {
        SCREEN_RULES.removeIf(r -> r.owner().equals(owner));
        EVENT_RULES.removeIf(r -> r.owner().equals(owner));
        API_RULES.removeIf(r -> r.owner.equals(owner));
    }

    public static void show(Page page) {
        PageScheduler.show(page);
    }

    public static void show(String pageId) {
        PageScheduler.requestShow(pageId);
    }

    public static void closeTop() {
        PageScheduler.closeTop();
    }

    public static void closeAll() {
        PageScheduler.closeAll();
    }

    public static boolean isShowing(Page page) {
        return PageScheduler.isShowing(page);
    }

    public static void showDialog(Component title, Element body) {
        Minecraft mc = Minecraft.getInstance();
        showDialog(title, mc.screen, body);
    }

    public static void showDialog(Component title, Screen parent, Element body) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new HellowDialog(title, parent, body));
    }

    public static Page page(PageId id, String titleKey, Element body) {
        return page(id, titleKey, PageLayer.NORMAL, body);
    }

    public static Page page(PageId id, String titleKey, PageLayer layer, Element body) {
        return new PageSpec(id, titleKey, layer, null, null, body);
    }

    static Screen dispatchScreenOpen(Screen opened) {
        if (opened == null || DISPATCHING) return null;
        DISPATCHING = true;
        try {
            for (ScreenRule rule : SCREEN_RULES) {
                if (!rule.condition().test(opened)) continue;
                if (LAST_TRIGGERED_SCREEN.get(rule) == opened) continue;
                rule.strategy().onConditionMatch();
                if (!rule.strategy().shouldFire()) continue;
                Screen screen = rule.factory().apply(opened);
                if (screen != null) {
                    rule.strategy().markFired();
                    LAST_TRIGGERED_SCREEN.put(rule, opened);
                    queueScreen(screen);
                    return screen;
                }
            }
        } finally {
            DISPATCHING = false;
        }
        return null;
    }

    private static volatile boolean DISPATCHING = false;

    static void dispatchGameEvent(Event event) {
        if (event == null) return;
        for (EventRule rule : EVENT_RULES) {
            if (!rule.eventType().isInstance(event)) continue;
            if (!rule.test().test(event)) continue;
            rule.strategy().onConditionMatch();
            if (!rule.strategy().shouldFire()) continue;
            Screen screen = rule.factory().apply(event);
            if (screen != null) {
                rule.strategy().markFired();
                queueScreen(screen);
            }
        }
    }

    private static void tickPollers() {
        for (ApiRule rule : API_RULES) {
            if (!rule.strategy.shouldFire()) continue;
            if (!rule.pollDue()) continue;
            Optional<?> content = rule.source.get();
            if (content.isEmpty()) continue;
            Screen screen = rule.factory.apply(content.get());
            if (screen != null) {
                rule.strategy.markFired();
                queueScreen(screen);
            }
        }
    }
}
