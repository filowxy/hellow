package com.hellow.element;

import com.hellow.action.Action;
import com.hellow.event.ElementEvents;
import com.hellow.event.ElementId;
import com.hellow.style.HellowTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Hellow button backed by a real vanilla {@link Button} widget, created in the
 * vanilla builder format ({@code Button.builder(message, onPress).bounds(...).build()}).
 * Hellow's layout only drives the widget's position/size; the look (texture, hover
 * highlight, disabled sprite, click sound) and the click handling are all vanilla's.
 */
public final class ButtonElement implements Element {
    /** Matches the vanilla button height. */
    private static final int HEIGHT = 20;
    private static final String ELEMENT_TYPE = "button";
    /**
     * Floor for the natural width: a one-character label ("×", "i") would otherwise
     * give a ~12px button whose click target is nearly unusable.
     */
    private static final int MIN_WIDTH = 24;

    private final Button vanilla;
    private final Component tooltip;
    private final ElementId elementId;

    public ButtonElement(String label, Action action) {
        this(label, action, null, null, HellowTheme.DEFAULT, true);
    }

    public ButtonElement(String label, Action action, Component tooltip) {
        this(label, action, tooltip, null, HellowTheme.DEFAULT, true);
    }

    /**
     * @param elementId when non-null, pressing the button also posts a
     *                  {@link com.hellow.event.HellowElementPressedEvent} on the
     *                  NeoForge game bus (the direct {@code action} still runs first).
     */
    public ButtonElement(String label, Action action, ElementId elementId) {
        this(label, action, null, elementId, HellowTheme.DEFAULT, true);
    }

    public ButtonElement(String label, Action action, Component tooltip, ElementId elementId) {
        this(label, action, tooltip, elementId, HellowTheme.DEFAULT, true);
    }

    public ButtonElement(String label, Action action, Component tooltip, ElementId elementId, HellowTheme theme) {
        this(label, action, tooltip, elementId, theme, true);
    }

    public ButtonElement(String label, Action action, Component tooltip, ElementId elementId,
                         HellowTheme theme, boolean enabled) {
        // Vanilla builder format; bounds are provisional — the layout recomputes them
        // on every render. The theme parameter is accepted for constructor
        // compatibility only: the vanilla look always wins (ponytail: themed,
        // self-drawn buttons were the point of the old implementation; callers that
        // need custom colors should draw their own Element instead).
        this.vanilla = Button.builder(Component.literal(label == null ? "" : label), pressed -> {
            if (action != null) action.run();
            ElementEvents.postPressed(elementId, ELEMENT_TYPE);
        }).bounds(0, 0, 0, HEIGHT).build();
        this.vanilla.active = enabled;
        this.tooltip = tooltip;
        this.elementId = elementId;
    }

    /** Disable/enable (a disabled button renders greyed and ignores clicks). */
    public void setEnabled(boolean enabled) {
        this.vanilla.active = enabled;
    }

    public boolean isEnabled() {
        return this.vanilla.active;
    }

    /** Change the label at runtime (e.g. a countdown text). */
    public void setLabel(String label) {
        this.vanilla.setMessage(Component.literal(label == null ? "" : label));
    }

    public int width(Font font) {
        return Math.max(MIN_WIDTH, font.width(this.vanilla.getMessage()) + 12);
    }

    public int height(Font font, int width) {
        return HEIGHT;
    }

    public void render(RenderContext ctx) {
        syncBounds(ctx);
        // Vanilla draws its own texture, hover highlight, disabled sprite and text.
        // Button's widget renderer does not use partialTick, so 0 is fine. Note that
        // AbstractWidget.render (final) also computes its own hovered state from the
        // mouse position + the active scissor region, so no hover bookkeeping here.
        this.vanilla.render(ctx.g(), ctx.mouseX(), ctx.mouseY(), 0.0F);
    }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        syncBounds(ctx);
        return this.vanilla.mouseClicked(ctx.mouseX(), ctx.mouseY(), button);
    }

    @Override
    public Component tooltipAt(RenderContext ctx) {
        return this.vanilla.active && this.vanilla.isMouseOver(ctx.mouseX(), ctx.mouseY()) ? this.tooltip : null;
    }

    /** Hand the layout-computed position/size to the vanilla widget. */
    private void syncBounds(RenderContext ctx) {
        this.vanilla.setPosition(ctx.x(), ctx.y());
        this.vanilla.setSize(ctx.width(), HEIGHT);
    }
}