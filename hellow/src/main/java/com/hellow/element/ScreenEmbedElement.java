package com.hellow.element;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Embeds an arbitrary third-party {@link Screen} into a hellow layout as an element:
 * renders the screen inside the element's rectangle (scissor-clipped) and forwards
 * mouse/keyboard events with the element's coordinates. This is how everyconfig-style
 * mods surface another config library's UI inside a hellow page.
 *
 * <p>Note (ponytail): embedded screens lay out their widgets against their own window
 * size, so content may overflow the rectangle — the scissor clips it. The element's
 * {@code height} is a stylistic hint; wrap it in a {@code box(...)} / {@code scroll(...)}
 * for borders or scrolling. Background dim: if the embedded screen fills its own
 * background it will show inside the rectangle.
 */
public final class ScreenEmbedElement implements Element {

    private final Screen embedded;
    private final int hintHeight;

    public ScreenEmbedElement(Screen embedded) {
        this(embedded, 120);
    }

    /** @param height stylist height for the embedded region */
    public ScreenEmbedElement(Screen embedded, int height) {
        this.embedded = embedded;
        this.hintHeight = Math.max(1, height);
    }

    public Screen embedded() {
        return this.embedded;
    }

    public int width(Font font) {
        return 200; // best-effort — containers stretch via flex/fill where needed
    }

    public int height(Font font, int width) {
        return this.hintHeight;
    }

    @Override
    public void render(RenderContext ctx) {
        if (ctx.g() == null) return;
        int h = height(ctx.font(), ctx.width());
        ctx.g().enableScissor(ctx.x(), ctx.y(), ctx.x() + ctx.width(), ctx.y() + h);
        try {
            this.embedded.render(ctx.g(), ctx.mouseX(), ctx.mouseY(), 0.0F);
        } finally {
            ctx.g().disableScissor();
        }
    }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        return this.embedded.mouseClicked(ctx.mouseX(), ctx.mouseY(), button);
    }

    @Override
    public boolean mouseScrolled(RenderContext ctx, double dX, double dY) {
        return this.embedded.mouseScrolled(ctx.mouseX(), ctx.mouseY(), dX, dY);
    }

    @Override
    public boolean mouseDragged(RenderContext ctx, int button, double dragX, double dragY) {
        return this.embedded.mouseDragged(ctx.mouseX(), ctx.mouseY(), button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.embedded.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.embedded.charTyped(codePoint, modifiers);
    }

    @Override
    public Component tooltipAt(RenderContext ctx) {
        return null; // the embedded screen owns its tooltips
    }
}