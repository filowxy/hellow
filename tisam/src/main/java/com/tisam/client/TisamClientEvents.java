package com.tisam.client;

import com.tisam.TisamConstants;
import com.tisam.client.screen.DisclaimerScreen;
import com.tisam.client.screen.FirstPromptScreen;
import com.tisam.client.screen.ModListScreen;
import com.tisam.config.ConfigManager;
import com.tisam.config.ModuleEntry;
import com.tisam.config.ModulesConfig;
import com.tisam.config.PreferencesData;
import com.tisam.reminder.ReminderStrategy;
import com.tisam.scanner.ModScanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public final class TisamClientEvents {
    private TisamClientEvents() {}

    private static final AtomicBoolean LATER_CLICKED_THIS_SESSION = new AtomicBoolean(false);
    /**
     * Set the first time the auto prompt is actually queued in this session. The
     * decay check alone is not enough: paths that leave the title area without a
     * "Later" click (ESC with a must-install mod present jumps straight back to the
     * title) would otherwise re-arm the prompt — this makes "at most one automatic
     * prompt per session" unconditional (preview mode excepted).
     */
    private static final AtomicBoolean AUTO_PROMPT_QUEUED_THIS_SESSION = new AtomicBoolean(false);

    /**
     * Title screen whose first prompt is due but deferred by one tick, so the screen
     * fully initializes (including our "View Mods" button) before the prompt covers
     * it. A plain field is fine: the tick handler runs on the render thread only.
     */
    private static TitleScreen pendingPromptTitle;

    // ---- Backward compat helpers (called by screen classes) ----

    public static void markPreviewSkipped() {
        TisamConstants.LOG.info("[tisam] preview skipped by user");
    }

    public static void onPromptClosed(boolean countAsReminder) {
        TisamConstants.LOG.info("[tisam] prompt closed, countAsReminder={}", countAsReminder);
        if (countAsReminder) {
            // A session counts at most one "Later": repeated Later clicks (view the
            // mod list, come back to a re-prompted title, Later again) must not
            // inflate the decay counter — one reminder seen = one increment.
            if (isLaterClickedThisSession()) {
                TisamConstants.LOG.info("[tisam] Later already counted this session; skipping reminder count");
                return;
            }
            markLaterClickedThisSession();
            PreferencesData prefs = ConfigManager.get().getPreferences();
            prefs.reminderCount++;
            ConfigManager.get().savePreferences();
        }
    }

    public static boolean isLaterClickedThisSession() {
        return LATER_CLICKED_THIS_SESSION.get();
    }

    public static void markLaterClickedThisSession() {
        LATER_CLICKED_THIS_SESSION.set(true);
    }

    // ---- Title screen integration ----
    //
    // Both the "View Mods" button and the first prompt live on the Title screen. The
    // prompt must NOT be shown from ScreenEvent.Opening: that fires before the title
    // screen is initialized, so the button has no screen to attach to, and a nested
    // setScreen() there fights the outer one. Instead we attach the button on
    // ScreenEvent.Init.Post (after every init — a Later-return re-inits the title
    // screen and clearWidgets() would drop the button) and defer the prompt to the
    // next client tick, by which time the title screen is fully on screen.

    /**
     * Attach the "View Mods" button (if content is missing) and, when the first-prompt
     * gates pass, queue the prompt to show next tick.
     *
     * <p>Called from {@code ScreenEvent.Init.Post} for every TitleScreen init pass.
     */
    private static void syncTitleScreen(TitleScreen title, ModulesConfig modules,
                                        PreferencesData prefs, List<ModuleEntry> missing) {
        if (missing.isEmpty()) {
            // Nothing recommended-but-missing: neither button nor prompt (by design).
            return;
        }
        if (prefs.permanentlySkipped && !modules.preview) {
            // Player opted out and this is not a preview: stay quiet (by design).
            return;
        }

        addButton(title, modules, missing, prefs);

        Minecraft mc = Minecraft.getInstance();
        // Count this title-screen visit as one launch for the frequency decay. The
        // wait counter used to be hardcoded to 0, so any non-first reminder (the
        // decay intervals are all >= 1) could never fire again — the decay table in
        // the design doc silently never worked.
        prefs.launchesSinceReminder++;
        boolean shouldAuto = modules.preview
                // Preview forces every launch; otherwise the decay table decides.
                || (!isLaterClickedThisSession()
                    && ReminderStrategy.shouldRemind(prefs.reminderCount,
                            prefs.launchesSinceReminder, modules.effectiveFrequency()));
        // At most one automatic prompt per session, whether preview mode or not.
        // Without this, preview (due always true) re-queues the prompt on every
        // return to the title screen — the player clicks "Later", lands on the
        // title, and is immediately covered by the same prompt again.
        boolean due = !AUTO_PROMPT_QUEUED_THIS_SESSION.get() && shouldAuto;
        if (mc.player == null && due) {
            AUTO_PROMPT_QUEUED_THIS_SESSION.set(true);
            pendingPromptTitle = title;
            // Showing the prompt ends the current wait period; the next reminder
            // starts counting launches from this one.
            prefs.launchesSinceReminder = 0;
            TisamConstants.LOG.info("[tisam] first prompt queued for the title screen ({} missing modules)",
                    missing.size());
        }
        ConfigManager.get().savePreferences();
    }

    /** Top-left position and size of the "View Mods" button on the title screen. */
    private static final int VIEW_MODS_BUTTON_X = 8;
    private static final int VIEW_MODS_BUTTON_Y = 8;
    private static final int VIEW_MODS_BUTTON_WIDTH = 110;
    private static final int VIEW_MODS_BUTTON_HEIGHT = 20;

    /**
     * Add the "View Mods" button to the title screen. Must be invoked on every init
     * pass: {@code Screen.init()} collapses the widget list ({@code clearWidgets()}),
     * so returning from the prompt wipes the previously attached button.
     *
     * <p>Registered in the vanilla builder format (a real {@link Button} via
     * {@code Button.builder(...).bounds(...).build()}), same as Hellow's buttons.
     */
    private static void addButton(TitleScreen title, ModulesConfig modules,
                                  List<ModuleEntry> missing, PreferencesData prefs) {
        Button button = Button.builder(Component.translatable("tisam.button.view_mods"), pressed -> {
            if (Minecraft.getInstance().screen instanceof TitleScreen t) {
                Minecraft.getInstance().setScreen(new ModListScreen(t, modules, missing, prefs));
            }
        }).bounds(VIEW_MODS_BUTTON_X, VIEW_MODS_BUTTON_Y, VIEW_MODS_BUTTON_WIDTH, VIEW_MODS_BUTTON_HEIGHT).build();
        // addRenderableWidget is protected on Screen, so a plain call won't compile
        // from here; its generic type erases the parameter to GuiEventListener, so
        // reflect on that exact signature (no method scanning needed).
        try {
            Method add = Screen.class.getDeclaredMethod("addRenderableWidget", GuiEventListener.class);
            add.setAccessible(true);
            add.invoke(title, button);
        } catch (Exception e) {
            TisamConstants.LOG.error("[tisam] Failed to add the 'View Mods' button to the title screen", e);
        }
    }

    // ---- Title screen event wiring ----

    @EventBusSubscriber(modid = TisamConstants.MOD_ID, value = Dist.CLIENT)
    public static final class TitleEvents {
        private TitleEvents() {}

        @SubscribeEvent
        public static void onInit(ScreenEvent.Init.Post event) {
            if (!(event.getScreen() instanceof TitleScreen title)) return;
            ConfigManager config = ConfigManager.get();
            ModulesConfig modules = config.getModules();
            PreferencesData prefs = config.getPreferences();
            Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
            List<ModuleEntry> missing = ModScanner.findMissing(gameDir, modules.modules);
            syncTitleScreen(title, modules, prefs, missing);
        }

        @SubscribeEvent
        public static void onTick(ClientTickEvent.Post event) {
            if (pendingPromptTitle == null) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != pendingPromptTitle) {
                // The title screen was replaced before the prompt could show; drop it
                // rather than popping a prompt over an unrelated screen.
                pendingPromptTitle = null;
                return;
            }
            TitleScreen title = pendingPromptTitle;
            pendingPromptTitle = null;
            ModulesConfig modules = ConfigManager.get().getModules();
            List<ModuleEntry> missing = ModScanner.findMissing(mc.gameDirectory.toPath(), modules.modules);
            if (!missing.isEmpty()) {
                TisamConstants.LOG.info("[tisam] showing first prompt with {} missing modules", missing.size());
                mc.setScreen(new FirstPromptScreen(title, modules, missing, ConfigManager.get().getPreferences()));
            }
        }
    }
}