package com.hellow.modal;

import com.hellow.HellowConstants;
import com.hellow.element.Element;
import com.hellow.element.RenderContext;
import com.hellow.style.HellowTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Reusable modal-dialog base class.
 *
 * <p>Owns the parts every dialog window shares: the dimmed overlay, the centered
 * dialog box, the title strip, the body region and the bottom button row. Subclasses
 * only declare the body content (via {@link #renderBody}) and the buttons (via
 * {@link #addBottomButton}); the geometry comes from this class.
 *
 * <p>Geometry is tweakable before {@code init()} runs: subclasses may resize the
 * dialog with {@link #dialogWidth}/{@link #dialogHeight} and tune the title and
 * button strips with {@link #titleArea}/{@link #buttonArea}. Buttons added with
 * {@link #addBottomButton} are positioned automatically in a centered row at the
 * bottom of the dialog box.
 *
 * <p>Closing (ESC or a button that sets the parent screen) returns to the
 * {@code parent} screen supplied at construction.
 */
public class HellowModal extends Screen {
    /** Screen to return to when the dialog closes. */
    protected Screen parent;

    /** Dialog box size, in GUI-scaled pixels. */
    protected int dialogWidth = 320;
    protected int dialogHeight = 180;

    /** Height of the title strip at the top of the dialog box. */
    protected int titleArea = 28;

    /** Height of the bottom strip where {@link #addBottomButton} buttons sit. */
    protected int buttonArea = 36;

    private static final int PADDING = 8;
    private static final int BOTTOM_BUTTON_WIDTH = 100;
    private static final int BOTTOM_BUTTON_HEIGHT = 20;
    private static final int BOTTOM_BUTTON_GAP = 8;
    /**
     * Screen-space margin kept around the dialog when fitting it into the window.
     * Vertical is larger than horizontal: in a normal (non-fullscreen) window the OS
     * title bar above and the taskbar below eat into the usable height, so the dialog
     * needs a little extra top/bottom clearance to never sit under them.
     */
    private static final int WINDOW_MARGIN_X = 20;
    private static final int WINDOW_MARGIN_Y = 44;
    /**
     * Largest allowed dialog scale relative to the design size. Keeps big-GUI windows
     * from blowing the dialog up to absurd sizes while still filling the width.
     */
    private static final double MAX_UPSCALE = 1.6;

    private final List<Button> bottomButtons = new ArrayList<>();
    /** Declarative bottom-row buttons (hellow elements) — positioned by this class. */
    private final List<Element> bottomElements = new ArrayList<>();
    private final HellowTheme theme;

    /**
     * Design-time dialog size (what the subclass set in its constructor) —
     * {@link #fitToScreen()} must scale from these, never from the already-scaled
     * values, otherwise shrinking the window then enlarging it again would
     * permanently shrink the dialog.
     */
    private int designDialogWidth = -1;
    private int designDialogHeight = -1;

    public HellowModal(Component title, Screen parent) {
        this(title, parent, HellowTheme.DEFAULT);
    }

    /** @param theme dialog chrome colors; {@code null} falls back to {@link HellowTheme#DEFAULT} */
    public HellowModal(Component title, Screen parent, HellowTheme theme) {
        super(title);
        this.parent = parent;
        this.theme = theme == null ? HellowTheme.DEFAULT : theme;
    }

    /**
     * Add a declarative bottom-row button (a hellow element, e.g.
     * {@code Elements.button(...)}). Hellow positions it in the centered bottom row,
     * renders it, forwards clicks and shows its tooltip — the caller only declares
     * the button. Safe to call before {@code super.init()}, like
     * {@link #addBottomButton}.
     */
    protected void addButton(Element button) {
        // The two bottom rows (declarative elements vs vanilla widgets) are drawn at
        // the same spot — never mix them, or the rows overlap. Guard cheaply at the
        // API boundary instead of documenting it in a comment nobody reads.
        if (!this.bottomButtons.isEmpty()) {
            HellowConstants.log().warn("[hellow] {} ignored an element button: vanilla bottom button already added",
                    getClass().getSimpleName());
            return;
        }
        this.bottomElements.add(Objects.requireNonNull(button, "button"));
    }

    /**
     * Add a button to the bottom row. Safe to call before {@code super.init()} —
     * the actual widget is registered with the screen in {@link #init()}, which
     * also positions it in the centered bottom row.
     *
     * <p>(legacy path: prefer {@link #addButton(Element)} so the button is a hellow
     * element instead of a vanilla widget; this overload stays for compatibility.)
     */
    protected void addBottomButton(Button button) {
        // Mutual-exclusion guard, mirror image of {@link #addButton(Element)}.
        if (!this.bottomElements.isEmpty()) {
            HellowConstants.log().warn("[hellow] {} ignored a vanilla button: element bottom button already added",
                    getClass().getSimpleName());
            return;
        }
        this.bottomButtons.add(Objects.requireNonNull(button, "button"));
    }

    /**
     * Drop every previously-added bottom button. Subclasses re-init on resize/GUI-scale
     * changes (vanilla {@code Screen.resize} → {@code init()}); because each init pass
     * calls {@link #addButton}/{@link #addBottomButton} again, calling this at the
     * start of a subclass {@code init()} prevents the button list from growing every
     * pass ("buttons keep multiplying").
     */
    protected final void resetBottomButtons() {
        this.bottomButtons.clear();
        this.bottomElements.clear();
    }

    // -------- Geometry helpers (for subclasses rendering the body) --------

    protected final int dialogLeft() {
        return Math.max(0, (this.width - this.dialogWidth) / 2);
    }

    protected final int dialogTop() {
        return Math.max(0, (this.height - this.dialogHeight) / 2);
    }

    protected final int bodyLeft() {
        return dialogLeft() + PADDING;
    }

    protected final int bodyTop() {
        return dialogTop() + this.titleArea;
    }

    protected final int bodyWidth() {
        return this.dialogWidth - PADDING * 2;
    }

    protected final int bodyHeight() {
        return this.dialogHeight - this.titleArea - this.buttonArea;
    }

    @Override
    protected void init() {
        // Keep the whole dialog on screen: keep the design size when it fits,
        // otherwise scale it down proportionally so the dialog (including the bottom
        // button row) never exceeds the window — windowed or maximized.
        fitToScreen();
        // init() reruns on resize / GUI-scale changes (vanilla Screen.resize →
        // init). vanilla Screen.init() does NOT collapse the widget list, so without
        // this the bottom buttons (and subclass widgets) would be re-added on top of
        // the previous ones — duplicate buttons at large GUI scales.
        this.clearWidgets();
        super.init();
        int count = this.bottomButtons.size();
        if (count == 0) return;
        // Shrink the buttons when the (possibly scaled-down) dialog is too narrow for
        // the default width, so the row always stays inside the dialog box.
        int avail = this.dialogWidth - PADDING * 2;
        int buttonW = Math.min(BOTTOM_BUTTON_WIDTH,
                Math.max(1, (avail - (count - 1) * BOTTOM_BUTTON_GAP) / count));
        int total = count * buttonW + (count - 1) * BOTTOM_BUTTON_GAP;
        int x = dialogLeft() + (this.dialogWidth - total) / 2;
        int y = dialogTop() + this.dialogHeight - this.buttonArea
                + (this.buttonArea - BOTTOM_BUTTON_HEIGHT) / 2;
        for (Button button : this.bottomButtons) {
            button.setX(x);
            button.setY(y);
            button.setWidth(buttonW);
            button.setHeight(BOTTOM_BUTTON_HEIGHT);
            this.addRenderableWidget(button);
            x += buttonW + BOTTOM_BUTTON_GAP;
        }
    }

    /**
     * Proportionally fit the dialog into the window (keeping a small margin), so the
     * whole dialog stays on screen at any window size. The design size is kept when it
     * already fits; only downscaling happens (both axes by the same factor, preserving
     * the dialog's aspect ratio — the pre-migration dialogs scaled this way too).
     */
    private void fitToScreen() {
        // First fit captures the design size (the subclass-set constructor values);
        // every later fit scales from that, never from the previous scaled result.
        if (this.designDialogWidth < 0 || this.designDialogHeight < 0) {
            this.designDialogWidth = this.dialogWidth;
            this.designDialogHeight = this.dialogHeight;
        }
        int availW = Math.max(1, this.width - WINDOW_MARGIN_X);
        int availH = Math.max(1, this.height - WINDOW_MARGIN_Y);
        double scale = Math.min(availW / (double) this.designDialogWidth,
                availH / (double) this.designDialogHeight);
        // Scale UP on large windows too (up to 1.6x the design size) so the dialog does
        // not look tiny with huge empty margins on big GUI scales — only downscale is
        // clamped by the window, upscale stops at a sane ceiling.
        scale = Math.max(0.0, Math.min(scale, MAX_UPSCALE));
        this.dialogWidth = Math.max(1, (int) Math.floor(this.designDialogWidth * scale));
        this.dialogHeight = Math.max(1, (int) Math.floor(this.designDialogHeight * scale));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Dim everything behind the dialog. We own the overlay, so renderBackground
        // is a no-op (see below) and the dim is drawn here.
        g.fill(0, 0, this.width, this.height, this.theme.dimOverlay());

        int left = dialogLeft();
        int top = dialogTop();
        g.fill(left, top, left + this.dialogWidth, top + this.dialogHeight, this.theme.dialogBackground());
        g.renderOutline(left, top, this.dialogWidth, this.dialogHeight, this.theme.dialogBorder());

        // Title, vertically centered in the title strip.
        int titleY = top + (this.titleArea - this.font.lineHeight) / 2;
        g.drawString(this.font, this.title, left + PADDING, titleY, this.theme.titleColor(), true);

        // Clip the body to its region: body content (multi-line text, partially
        // visible list rows, tooltips) must never bleed into the bottom button row.
        if (bodyHeight() > 0) {
            g.enableScissor(bodyLeft(), bodyTop(), bodyLeft() + bodyWidth(), bodyTop() + bodyHeight());
            this.renderBody(g, mouseX, mouseY, partialTick);
            g.disableScissor();
        }

        renderElementButtons(g, mouseX, mouseY);

        super.render(g, mouseX, mouseY, partialTick);
    }

    // -------- Declarative (hellow element) bottom button row --------

    /**
     * Draw the centered bottom row of declarative buttons, respecting the same
     * button-area strip the vanilla {@link #addBottomButton} row uses (they never mix
     * — use one or the other).
     */
    private void renderElementButtons(GuiGraphics g, int mouseX, int mouseY) {
        if (this.bottomElements.isEmpty()) return;
        Font font = Minecraft.getInstance().font;
        int[] widths = elementButtonWidths(font);
        int total = 0;
        for (int w : widths) total += w;
        total += BOTTOM_BUTTON_GAP * (this.bottomElements.size() - 1);
        int x = dialogLeft() + (this.dialogWidth - total) / 2;
        int y = dialogTop() + this.dialogHeight - this.buttonArea
                + (this.buttonArea - BOTTOM_BUTTON_HEIGHT) / 2;
        Component tooltip = null;
        for (int i = 0; i < this.bottomElements.size(); i++) {
            RenderContext ctx = new RenderContext(g, font, x, y, widths[i], mouseX, mouseY);
            this.bottomElements.get(i).render(ctx);
            Component t = this.bottomElements.get(i).tooltipAt(ctx);
            if (t != null) tooltip = t;
            x += widths[i] + BOTTOM_BUTTON_GAP;
        }
        if (tooltip != null) {
            g.renderTooltip(font, List.of(tooltip),
                    Optional.<net.minecraft.world.inventory.tooltip.TooltipComponent>empty(), mouseX, mouseY);
        }
    }

    /**
     * Per-button render width: at least {@link #BOTTOM_BUTTON_WIDTH} (matching the
     * legacy vanilla row's fixed width), shrunk equally when the row overflows.
     */
    private int[] elementButtonWidths(Font font) {
        int n = this.bottomElements.size();
        int[] widths = new int[n];
        int totalNat = 0;
        for (int i = 0; i < n; i++) {
            widths[i] = Math.max(BOTTOM_BUTTON_WIDTH, this.bottomElements.get(i).width(font));
            totalNat += widths[i];
        }
        totalNat += BOTTOM_BUTTON_GAP * (n - 1);
        int avail = this.dialogWidth - PADDING * 2;
        if (totalNat > avail) {
            int shrink = ((totalNat - avail) + n - 1) / n;
            for (int i = 0; i < n; i++) {
                widths[i] = Math.max(8, widths[i] - shrink);
            }
        }
        return widths;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (dispatchElementButtons((int) mouseX, (int) mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Forward a click to the declarative bottom-row buttons (same layout as render). */
    private boolean dispatchElementButtons(int mouseX, int mouseY, int button) {
        if (this.bottomElements.isEmpty()) return false;
        Font font = Minecraft.getInstance().font;
        int[] widths = elementButtonWidths(font);
        int total = 0;
        for (int w : widths) total += w;
        total += BOTTOM_BUTTON_GAP * (this.bottomElements.size() - 1);
        int x = dialogLeft() + (this.dialogWidth - total) / 2;
        int y = dialogTop() + this.dialogHeight - this.buttonArea
                + (this.buttonArea - BOTTOM_BUTTON_HEIGHT) / 2;
        for (int i = 0; i < this.bottomElements.size(); i++) {
            RenderContext c = new RenderContext(null, font, x, y, widths[i], mouseX, mouseY);
            if (this.bottomElements.get(i).mouseClicked(c, button)) return true;
            x += widths[i] + BOTTOM_BUTTON_GAP;
        }
        return false;
    }

    /**
     * Hook for subclasses to draw the dialog body (between the title and the button
     * row). Bounds are available via {@link #bodyLeft()}/{@link #bodyTop()}/
     * {@link #bodyWidth()}/{@link #bodyHeight()}.
     */
    protected void renderBody(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // No-op: the dim layer is drawn in render() so it appears above the parent
        // screen's background without relying on vanilla's blur.
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC closes the dialog and returns to the parent screen.
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (this.parent != null && this.minecraft != null && this.minecraft.screen == this) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
