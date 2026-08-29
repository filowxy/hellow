package com.tisam.client.screen;

import com.hellow.element.ButtonElement;
import com.hellow.element.ColumnElement;
import com.hellow.element.Element;
import com.hellow.element.Elements;
import com.hellow.element.RenderContext;
import com.hellow.element.ScrollElement;
import com.hellow.modal.HellowModal;
import com.hellow.style.HellowTheme;
import com.tisam.TisamConstants;
import com.tisam.client.TisamClientEvents;
import com.tisam.config.ConfigManager;
import com.tisam.config.ModulesConfig;
import com.tisam.config.PreferencesData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Disclaimer modal shown before permanent-skip takes effect.
 *
 * <p>Per the requirement: the "Confirm" button is disabled for
 * {@link TisamConstants#DISCLAIMER_COOLDOWN_SECONDS} seconds to prevent mis-clicks; the
 * button shows the remaining time, e.g. "Confirm (3s)".
 *
 * <p>Chrome comes from {@link HellowModal} with a {@link #WARN_THEME} that only swaps
 * the dialog border/title to the orange warning color TISAM always used; everything
 * else matches {@link HellowTheme#DEFAULT}.
 *
 * <p>Self-contained: this screen commits the permanent skip itself (no delegation to a
 * possibly-uninitialized FirstPromptScreen — see the pre-migration crash note). "Back"
 * returns to {@code backTarget}; "Confirm" jumps to {@code confirmTarget} (TitleScreen).
 */
public final class DisclaimerScreen extends HellowModal {

    /** Orange warning accent — identical to the pre-migration hand-drawn dialog. */
    private static final HellowTheme WARN_THEME = new HellowTheme(
            HellowTheme.DEFAULT.dimOverlay(),
            HellowTheme.DEFAULT.panelBackground(),
            HellowTheme.DEFAULT.panelBorder(),
            TisamConstants.DISCLAIMER_WARN_COLOR,       // title
            HellowTheme.DEFAULT.dialogBackground(),
            TisamConstants.DISCLAIMER_WARN_COLOR,       // dialog border
            HellowTheme.DEFAULT.buttonBackground(),
            HellowTheme.DEFAULT.buttonBackgroundHover(),
            HellowTheme.DEFAULT.buttonBorder(),
            HellowTheme.DEFAULT.buttonBorderHover(),
            HellowTheme.DEFAULT.buttonText(),
            HellowTheme.DEFAULT.boxBorder(),
            HellowTheme.DEFAULT.textColor(),
            HellowTheme.DEFAULT.linkColor(),
            HellowTheme.DEFAULT.expandHeaderText(),
            HellowTheme.DEFAULT.expandHeaderTextHover(),
            HellowTheme.DEFAULT.scrollbarTrack(),
            HellowTheme.DEFAULT.scrollbarThumb(),
            HellowTheme.DEFAULT.formLabel(),
            HellowTheme.DEFAULT.formFieldBackground(),
            HellowTheme.DEFAULT.formOutline(),
            HellowTheme.DEFAULT.formOutlineFocused(),
            HellowTheme.DEFAULT.formValue(),
            HellowTheme.DEFAULT.toggleKnobOn(),
            HellowTheme.DEFAULT.sliderTrack(),
            HellowTheme.DEFAULT.sliderKnob());

    /** Screen to return to on "Confirm" — typically the TitleScreen. */
    private final net.minecraft.client.gui.screens.Screen confirmTarget;
    private final ModulesConfig modules;
    private final PreferencesData prefs;

    private final long openedAtMillis;
    private com.hellow.element.ButtonElement confirmButton;
    private Element body;

    public DisclaimerScreen(net.minecraft.client.gui.screens.Screen backTarget,
                            net.minecraft.client.gui.screens.Screen confirmTarget,
                            ModulesConfig modules, PreferencesData prefs) {
        super(Component.translatable("tisam.disclaimer.title"), backTarget, WARN_THEME);
        this.confirmTarget = confirmTarget;
        this.modules = modules;
        this.prefs = prefs;
        this.openedAtMillis = System.currentTimeMillis();
        this.dialogWidth = 360;
        this.dialogHeight = 200;
    }

    @Override
    protected void init() {
        resetBottomButtons();
        addButton(Elements.button(Component.translatable("tisam.disclaimer.back").getString(),
                () -> this.minecraft.setScreen(this.parent)));

        // Confirm → commit the permanent skip and jump to the title screen.
        // All prefs mutation happens here so we don't depend on a possibly-uninit'd
        // parent screen (ponytail: this used to delegate to parent.commitPermanentSkip()
        // and crashed because that parent's minecraft field was null).
        // The button starts disabled; renderBody unlocks it after the cooldown.
        this.confirmButton = new ButtonElement(
                Component.translatable("tisam.disclaimer.confirm_cooldown",
                        TisamConstants.DISCLAIMER_COOLDOWN_SECONDS).getString(),
                this::commitPermanentSkip, null, null, WARN_THEME, false);
        addButton(this.confirmButton);
        super.init();
        rebuildBody();
    }

    /** Disclaimer body: a scrollable fill-wrapped text — long author text can be read. */
    private void rebuildBody() {
        String disclaimer = modules.disclaimerText == null ? "" : modules.disclaimerText.replace("\\n", "\n");
        this.body = new ScrollElement(List.of(
                Elements.fill(Elements.wrappedText(Component.literal(disclaimer), TisamConstants.DISCLAIMER_TEXT_COLOR))),
                0, Math.max(1, bodyHeight()));
    }

    private RenderContext bodyContext(int mouseX, int mouseY) {
        return new RenderContext(null, this.font, bodyLeft(), bodyTop() + 4, bodyWidth(), mouseX, mouseY);
    }

    @Override
    protected void renderBody(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (this.body != null) {
            RenderContext ctx = new RenderContext(g, this.font, bodyLeft(), bodyTop() + 4,
                    bodyWidth(), mouseX, mouseY);
            this.body.render(ctx);
        }

        // Cooldown countdown — update the confirm button every frame until it unlocks.
        if (this.confirmButton != null) {
            long elapsedSec = (System.currentTimeMillis() - this.openedAtMillis) / 1000;
            long remaining = Math.max(0, TisamConstants.DISCLAIMER_COOLDOWN_SECONDS - elapsedSec);
            if (remaining > 0) {
                this.confirmButton.setEnabled(false);
                this.confirmButton.setLabel(Component.translatable(
                        "tisam.disclaimer.confirm_cooldown", remaining).getString());
            } else if (!this.confirmButton.isEnabled()) {
                this.confirmButton.setEnabled(true);
                this.confirmButton.setLabel(Component.translatable("tisam.disclaimer.confirm").getString());
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.body != null
                && this.body.mouseClicked(bodyContext((int) mouseX, (int) mouseY), button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (this.body != null
                && this.body.mouseScrolled(bodyContext((int) mouseX, (int) mouseY), deltaX, deltaY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.body != null
                && this.body.mouseDragged(bodyContext((int) mouseX, (int) mouseY), button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC = "Back" (safer default than skipping the confirmation). Handled here
        // (not in onClose) because onClose also fires whenever setScreen() replaces us
        // — ponytail: putting it in onClose used to undo the Confirm's setScreen.
        if (keyCode == 256) {
            if (this.minecraft != null && this.parent != null) {
                this.minecraft.setScreen(this.parent);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Inline implementation of the "Confirm" action. Doing it here (instead of
     * delegating to a parent screen) means we never have to worry about whether the
     * parent screen was actually shown / initialized.
     */
    private void commitPermanentSkip() {
        // In preview mode, the confirmation is in-memory only — we don't write
        // permanentlySkipped to disk so authors still see the prompt each launch.
        if (modules.preview) {
            TisamClientEvents.markPreviewSkipped();
        } else {
            this.prefs.permanentlySkipped = true;
            this.prefs.lastReminderDate = LocalDate.now().toString();
            ConfigManager.get().savePreferences();
            TisamConstants.LOG.info("[tisam] Player confirmed permanent skip");
        }
        TisamClientEvents.onPromptClosed(false);
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.confirmTarget);
        }
    }
}