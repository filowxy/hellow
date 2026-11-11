package com.hellow.element;

import com.hellow.action.Action;
import com.hellow.event.ElementEvents;
import com.hellow.event.ElementId;
import com.hellow.style.HellowTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class ButtonElement implements Element {
    private static final int HEIGHT = 20;
    private static final String ELEMENT_TYPE = "button";
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
        this.vanilla = Button.builder(Component.literal(label == null ? "" : label), pressed -> {
            if (action != null) action.run();
            ElementEvents.postPressed(elementId, ELEMENT_TYPE);
        }).bounds(0, 0, 0, HEIGHT).build();
        this.vanilla.active = enabled;
        this.tooltip = tooltip;
        this.elementId = elementId;
    }

    public void setEnabled(boolean enabled) {
        this.vanilla.active = enabled;
    }

    public boolean isEnabled() {
        return this.vanilla.active;
    }

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

    private void syncBounds(RenderContext ctx) {
        this.vanilla.setPosition(ctx.x(), ctx.y());
        this.vanilla.setSize(ctx.width(), HEIGHT);
    }
}
