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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Hellow's public facade — the single entry point for embedding mods.
 *
 * <p>Callers use Hellow to do three things:
 * <ol>
 *   <li><b>Open windows</b> — {@link #show(Page)} / {@link #showDialog} plus the
 *       window-stack management behind {@link #closeTop()} / {@link #closeAll()}.</li>
 *   <li><b>Declare event-driven triggers</b> — "when <em>something</em> happens, show
 *       <em>this</em> UI". Three trigger sources are supported:
 *       <ul>
 *         <li>{@link #when(String, Condition, Function, FireStrategy)} — a vanilla
 *             {@link Screen} is about to open (NeoForge {@code ScreenEvent.Opening}).</li>
 *         <li>{@link #whenEvent(String, Class, Predicate, Function, FireStrategy)} —
 *             any NeoForge game-bus event of the given type fires.</li>
 *         <li>{@link #whenApi(String, ApiSource, Function, FireStrategy)} — the caller's
 *             API returns content (polled on the client tick).</li>
 *       </ul>
 *   </li>
 *   <li><b>Build pages</b> — {@link #page(PageId, String, Element)} and friends, so
 *       callers never touch {@link PageSpec} directly.</li>
 * </ol>
 *
 * <p>The design principle stays the one from the Hellow design doc: <em>the caller
 * defines the content, Hellow manages the presentation and the window lifecycle.</em>
 * Hellow never inspects or trusts the content a caller passes in.
 *
 * <p>Threading: rule registration happens on the mod-loading thread; dispatch happens
 * on the client game thread. Lists are {@link CopyOnWriteArrayList} so both are safe.
 */
public final class Hellow {
    private Hellow() {}

    /**
     * Fire-strategy alias so callers can write {@code Hellow.FireStrategy.once()}
     * instead of importing {@code com.hellow.trigger.FireStrategy}. The factories
     * forward to the base interface.
     */
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

    /**
     * A pollable data source for {@link #whenApi}. Hellow asks this supplier on the
     * client tick (throttled); when it returns a present value, the trigger fires.
     * Returning {@link Optional#empty()} simply means "not yet — keep polling".
     *
     * @param <T> the content type the caller's API produces
     */
    @FunctionalInterface
    public interface ApiSource<T> {
        Optional<T> get();
    }

    /** Default poll period (in client ticks) for {@link #whenApi} without an interval. */
    public static final int DEFAULT_POLL_INTERVAL_TICKS = 20;

    // -------- Rule storage --------

    /**
     * A "when X opens, show Y" rule. {@code factory} returns the screen to show, or
     * {@code null} to decline (e.g. the caller's own gating decided not to show).
     */
    private record ScreenRule(String owner, Condition condition,
                              Function<Screen, Screen> factory,
                              com.hellow.trigger.FireStrategy strategy) {}

    /** A "when a NeoForge event of type E fires, show Y" rule. */
    private record EventRule(String owner, Class<?> eventType,
                             Predicate<Event> test, Function<Event, Screen> factory,
                             com.hellow.trigger.FireStrategy strategy) {}

    /**
     * A "when the caller's API returns content, show Y" rule. {@code ticksSincePoll}
     * counts down the poll interval so sources that are expensive (e.g. filesystem
     * scans) are only queried every {@code pollInterval} ticks.
     */
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

    /** Event types we already hooked on the NeoForge game bus (one listener per type). */
    private static final Set<Class<?>> HOOKED_EVENT_TYPES = new CopyOnWriteArraySet<>();

    // -------- Deferred screen opening --------
    //
    // Showing a page by calling Minecraft.setScreen() *inside* ScreenEvent.Opening
    // is unsafe: Opening fires while the outer setScreen() (the one that triggered
    // it) is still in progress, so the inner setScreen() is overwritten when the
    // outer one completes — the page's Opening fires (visible in logs) but the page
    // never reaches the screen. Queue the screen and apply it on the next client
    // tick instead (same approach TISAM uses for its first prompt).

    /** Screen queued to open on the next client tick (drained by {@link #drainPendingScreen()}). */
    private static final java.util.concurrent.atomic.AtomicReference<Screen> PENDING_SCREEN =
            new java.util.concurrent.atomic.AtomicReference<>();

    /** Hooked once on the game bus to drain {@link #PENDING_SCREEN}. */
    private static volatile boolean TICK_DRAIN_HOOKED = false;

    private static void ensureTickDrainHooked() {
        if (TICK_DRAIN_HOOKED) return;
        TICK_DRAIN_HOOKED = true;
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> drainPendingScreen());
    }

    private static void drainPendingScreen() {
        Screen pending = PENDING_SCREEN.getAndSet(null);
        if (pending == null) return;
        // The trigger fired from ScreenEvent.Opening, which happens *before* the
        // outer setScreen() assigns mc.screen — so at trigger time the parent was
        // whatever screen was current before the one that's now opening (e.g. the
        // boot "Loading Minecraft" screen). By the time we drain on the next tick
        // the opening screen is fully set, so re-parent to the current screen.
        if (pending instanceof PageScreen ps) {
            ps.reparent(Minecraft.getInstance().screen);
        }
        Minecraft.getInstance().setScreen(pending);
    }

    /** Queue {@code screen} to open on the next client tick (deferred, see above). */
    private static void queueScreen(Screen screen) {
        if (screen == null) return;
        PENDING_SCREEN.set(screen);
        ensureTickDrainHooked();
    }

    // -------- Trigger registration --------

    /**
     * Register a screen-driven trigger: when a {@link Screen} matching {@code condition}
     * is about to open, ask {@code factory} for the screen to show (or {@code null} to
     * decline). The factory is called every time the condition matches until the
     * strategy decides to stop; after it returns non-null the strategy's
     * {@code markFired()} is invoked.
     *
     * @param owner     the registering mod's id (used for logs / debugging)
     * @param condition which opening screens should be considered
     * @param factory   builds the screen to show; {@code null} return = do nothing
     * @param strategy  how often to fire (e.g. {@link FireStrategy#once()})
     */
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

    /**
     * Register a NeoForge game-bus event trigger: when an event of type
     * {@code eventType} is posted and {@code test} accepts it, ask {@code factory}
     * for the screen to show. This covers "trigger the UI from any NeoForge event".
     *
     * @param owner     the registering mod's id
     * @param eventType the NeoForge event class to listen for
     * @param test      extra filter on the event (or always-true)
     * @param factory   builds the screen to show; {@code null} return = do nothing
     * @param strategy  how often to fire
     * @param <E>       the event type
     */
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
        // NeoForge's bus refuses a listener bound to the abstract base Event class, so
        // each rule hooks its own concrete event type instead of one catch-all listener.
        hookGameBus(eventType, e -> dispatchGameEvent(e));
        HellowConstants.log().info("[hellow] NeoForge-event trigger registered for '{}' on {} ({} rules total)",
                owner, eventType.getSimpleName(), EVENT_RULES.size());
    }

    /**
     * Adds one NeoForge game-bus listener for {@code eventType} the first time a rule
     * needs it; {@code dispatchGameEvent} fans the event out to every matching rule.
     */
    @SuppressWarnings("unchecked")
    private static <E extends Event> void hookGameBus(Class<E> eventType, Consumer<E> handler) {
        if (HOOKED_EVENT_TYPES.add(eventType)) {
            NeoForge.EVENT_BUS.addListener(eventType, handler);
        }
    }

    /**
     * Register an API-driven trigger: Hellow polls {@code source} on the client tick
     * and, when it returns a present value, shows the screen built by {@code factory}.
     * The "special event" the user's own API produces (a non-empty scan result, a
     * server response, a config change, ...) becomes the trigger.
     *
     * <p>(ponytail: the {@code decaying} strategy is not meaningful for polling —
     * the poller only consults {@code shouldFire()} and stops polling once a
     * strategy has fired. {@code once()} = "poll until the API has content, then show
     * exactly once"; {@code everyTime()} = "re-show whenever the API has content".)
     *
     * @param owner     the registering mod's id
     * @param source    the caller's API; polled on the client thread
     * @param factory   builds the screen to show; {@code null} return = do nothing
     * @param strategy  how often to fire
     * @param <T>       the content type the API returns
     */
    public static <T> void whenApi(String owner, ApiSource<T> source,
                                   Function<T, Screen> factory,
                                   com.hellow.trigger.FireStrategy strategy) {
        whenApi(owner, source, factory, strategy, DEFAULT_POLL_INTERVAL_TICKS);
    }

    /**
     * {@link #whenApi(String, ApiSource, Function, FireStrategy)} with an explicit
     * poll period in client ticks (default {@link #DEFAULT_POLL_INTERVAL_TICKS}).
     */
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
        // API pollers advance on every client tick; hook the tick once.
        hookGameBus(ClientTickEvent.Post.class, e -> tickPollers());
        HellowConstants.log().info("[hellow] API trigger registered for '{}' ({} rules total)",
                owner, API_RULES.size());
    }

    /** Drop every registered trigger rule. Intended for tests and mod teardown. */
    public static void clearRules() {
        SCREEN_RULES.clear();
        EVENT_RULES.clear();
        API_RULES.clear();
        // The game-bus listeners hooked per event type can't be unregistered through
        // this API; clearing the set documents that a later re-register is a no-op
        // (the old listeners still run but dispatch to empty rule lists).
        HOOKED_EVENT_TYPES.clear();
    }

    // -------- Window management --------

    /** Open a page as a window, remembering the current screen as its parent. */
    public static void show(Page page) {
        PageScheduler.show(page);
    }

    /** Open a previously-registered page by id. */
    public static void show(String pageId) {
        PageScheduler.requestShow(pageId);
    }

    /** Close the topmost hellow window, returning to the screen it was opened from. */
    public static void closeTop() {
        PageScheduler.closeTop();
    }

    /** Close every open hellow window. */
    public static void closeAll() {
        PageScheduler.closeAll();
    }

    /** Whether the given page is currently open (anywhere in the window stack). */
    public static boolean isShowing(Page page) {
        return PageScheduler.isShowing(page);
    }

    /** Open a generic dialog with an element body, parented to the current screen. */
    public static void showDialog(Component title, Element body) {
        Minecraft mc = Minecraft.getInstance();
        showDialog(title, mc.screen, body);
    }

    /** Open a generic dialog with an element body, parented to {@code parent}. */
    public static void showDialog(Component title, Screen parent, Element body) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new HellowDialog(title, parent, body));
    }

    // -------- Page builders --------

    /** Build a plain page (NORMAL layer) from an id, a title translation key and a body. */
    public static Page page(PageId id, String titleKey, Element body) {
        return page(id, titleKey, PageLayer.NORMAL, body);
    }

    /** Build a page with an explicit layer. */
    public static Page page(PageId id, String titleKey, PageLayer layer, Element body) {
        return new PageSpec(id, titleKey, layer, null, null, body);
    }

    // -------- Dispatch (called by HellowMod's bus subscribers) --------

    /**
     * Evaluate screen-driven rules for a screen that is about to open. Returns the
     * screen that was shown, or {@code null} if no rule produced one.
     *
     * <p>(ponytail: a reentrancy guard prevents a broad condition + {@code everyTime()}
     * rule from bouncing setScreen → ScreenEvent.Opening → setScreen forever. Nested
     * screen-open reactions are intentionally deferred until the current dispatch
     * unwinds. The typed alternative is to check conditions against the {@code parent}
     * screen instead of re-opening screens during dispatch.)
     */
    static Screen dispatchScreenOpen(Screen opened) {
        if (opened == null || DISPATCHING) return null;
        DISPATCHING = true;
        try {
            for (ScreenRule rule : SCREEN_RULES) {
                if (!rule.condition().test(opened)) continue;
                rule.strategy().onConditionMatch();
                if (!rule.strategy().shouldFire()) continue;
                Screen screen = rule.factory().apply(opened);
                if (screen != null) {
                    rule.strategy().markFired();
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

    /**
     * Evaluate NeoForge-event rules for a posted game event. Called by the per-type
     * game-bus listeners hooked in {@link #whenEvent}; pollers advance on
     * {@code ClientTickEvent.Post} via the listener hooked in {@link #whenApi}.
     */
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

    /** Advance each API poller; show any trigger whose source now has content. */
    private static void tickPollers() {
        for (ApiRule rule : API_RULES) {
            if (!rule.strategy.shouldFire()) continue;   // already fired / not due
            if (!rule.pollDue()) continue;               // throttled
            Optional<?> content = rule.source.get();
            if (content.isEmpty()) continue;             // API not ready yet
            Screen screen = rule.factory.apply(content.get());
            if (screen != null) {
                rule.strategy.markFired();
                queueScreen(screen);
            }
        }
    }
}
