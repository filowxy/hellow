package com.tisam.client.screen;

import com.hellow.element.ColumnElement;
import com.hellow.element.Element;
import com.hellow.element.Elements;
import com.hellow.element.RenderContext;
import com.hellow.element.TextElement;
import com.hellow.modal.HellowModal;
import com.tisam.TisamConstants;
import com.tisam.client.TisamClientEvents;
import com.tisam.config.ModuleEntry;
import com.tisam.config.ModuleEntry.Priority;
import com.tisam.config.ModulesConfig;
import com.tisam.config.PreferencesData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * First "there is still a mod" prompt shown at the title screen.
 *
 * <p>Chrome (dim layer, dialog box, title strip, bottom button row) comes from
 * {@link HellowModal}. The body is a hellow element column — TISAM only declares the
 * lines and their colors; hellow does the text layout.
 */
public final class FirstPromptScreen extends HellowModal {

    private final TitleScreen parent;
    private final ModulesConfig modules;
    private final List<ModuleEntry> missing;
    private final PreferencesData prefs;
    private Element body;

    public FirstPromptScreen(TitleScreen parent, ModulesConfig modules,
                             List<ModuleEntry> missing, PreferencesData prefs) {
        super(titleWithEscHint(), parent);
        this.parent = parent;
        this.modules = modules;
        this.missing = missing;
        this.prefs = prefs;
        this.dialogWidth = 380;
        this.dialogHeight = 210;
    }

    /**
     * The title strip shows the mod name plus a gray "ESC = Later" hint on the right.
     * Putting the hint inside the title (instead of the body) keeps it in the title
     * bar — the pre-migration look — and away from the body text it used to overlap.
     */
    private static Component titleWithEscHint() {
        return Component.literal(TisamConstants.MOD_NAME)
                .append(Component.literal("  "))
                .append(Component.literal("ESC = Later").withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected void init() {
        resetBottomButtons();
        // can_ignore=false entries are "must install": disallow Later / Never Remind.
        boolean locked = false;
        for (ModuleEntry m : this.missing) {
            if (!m.canIgnore) {
                locked = true;
                break;
            }
        }
        addButton(Elements.button(Component.translatable("tisam.button.view_mods").getString(),
                this::openModList));
        addButton(Elements.button(Component.translatable("tisam.button.later").getString(),
                this::laterReminder, !locked));
        addButton(Elements.button(Component.translatable("tisam.button.never_remind").getString(),
                this::openDisclaimer, !locked));
        super.init();
        rebuildBody();
    }

    /** Declare the body as a hellow element column — hellow lays the lines out. */
    private void rebuildBody() {
        List<Element> rows = new ArrayList<>();
        String bodyKey = hasCritical() ? "tisam.first_prompt.recommended_body"
                                       : "tisam.first_prompt.body_line1";
        rows.add(new TextElement(Component.translatable(bodyKey, missing.size()),
                TisamConstants.CARD_NAME_COLOR));
        rows.add(new TextElement(Component.translatable("tisam.first_prompt.body_line2"),
                TisamConstants.TEXT_MUTED));
        int hintColor = hasCritical() ? TisamConstants.PROMPT_HINT_HIGHLIGHT
                                      : TisamConstants.PROMPT_HINT_NORMAL;
        rows.add(new TextElement(Component.literal("▶ " + missing.size() + " mods found"), hintColor));
        if (hasNonIgnorable()) {
            rows.add(new TextElement(Component.translatable("tisam.first_prompt.must_install"),
                    TisamConstants.PROMPT_HINT_HIGHLIGHT));
        }
        this.body = new ColumnElement(rows, 4);
    }

    private RenderContext bodyContext(int mouseX, int mouseY) {
        return new RenderContext(null, this.font, bodyLeft(), bodyTop() + 4, bodyWidth(), mouseX, mouseY);
    }

    @Override
    protected void renderBody(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (this.body == null) return;
        RenderContext ctx = new RenderContext(g, this.font, bodyLeft(), bodyTop() + 4,
                bodyWidth(), mouseX, mouseY);
        this.body.render(ctx);
        Component tooltip = this.body.tooltipAt(ctx);
        if (tooltip != null) {
            g.renderTooltip(this.font, List.of(tooltip),
                    java.util.Optional.<net.minecraft.world.inventory.tooltip.TooltipComponent>empty(), mouseX, mouseY);
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
        // ESC = "Later" (counts as one reminder, same as the Later button). When a
        // must-install (can_ignore=false) entry is missing, Later/Never are locked —
        // ESC just dismisses the prompt (no reminder consumed, next title visit
        // prompts again) so the player is never trapped.
        if (keyCode == 256) {
            if (hasNonIgnorable()) {
                Minecraft.getInstance().setScreen(this.parent);
            } else {
                laterReminder();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean hasNonIgnorable() {
        for (ModuleEntry m : this.missing) {
            if (!m.canIgnore) return true;
        }
        return false;
    }

    private void openModList() {
        Minecraft.getInstance().setScreen(new ModListScreen(parent, modules, missing, prefs));
    }

    private void laterReminder() {
        TisamClientEvents.onPromptClosed(true);
        Minecraft.getInstance().setScreen(this.parent);
    }

    private void openDisclaimer() {
        Minecraft.getInstance().setScreen(new DisclaimerScreen(this, parent, modules, prefs));
    }

    private boolean hasCritical() {
        for (ModuleEntry m : missing) {
            if (m.priority == Priority.CRITICAL || m.priority == Priority.FEATURED) return true;
        }
        return false;
    }
}