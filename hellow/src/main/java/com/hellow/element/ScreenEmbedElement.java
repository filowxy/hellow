package com.hellow.element;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Embeds an arbitrary third-party {@link Screen} into a hellow layout as an element:
 * renders the screen inside the element's rectangle (scissor-clipped) and forwards
 * mouse/keyboard events with the element's coordinates. This is how everyconfig-style
 * mods surface another config library's UI inside a hellow page.
 *
 * <p>The embedded screen is {@link Screen#init(Minecraft, int, int) initialized}
 * against the element's rectangle (once per size) so its widgets lay out inside the
 * rectangle, and mouse coordinates are translated from absolute screen space to
 * element-local space before being forwarded.
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

    // Last size the embedded screen was initialized with; re-init when it changes.
    private int initW = -1;
    private int initH = -1;

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

    /** Element-local mouse X inside the rectangle (embedded screens use local coords). */
    private static int localX(RenderContext ctx) {
        return ctx.mouseX() - ctx.x();
    }

    /** Element-local mouse Y inside the rectangle. */
    private static int localY(RenderContext ctx) {
        return ctx.mouseY() - ctx.y();
    }

    /** Lay the embedded screen out against the current rectangle once per size. */
    private void ensureInit(RenderContext ctx) {
        int w = ctx.width();
        int h = height(ctx.font(), w);
        if (this.initW == w && this.initH == h) return;
        this.initW = w;
        this.initH = h;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            this.embedded.init(mc, w, h);
        }
    }

    @Override
    public void render(RenderContext ctx) {
        if (ctx.g() == null) return;
        int h = height(ctx.font(), ctx.width());
        ensureInit(ctx);
        ctx.g().enableScissor(ctx.x(), ctx.y(), ctx.x() + ctx.width(), ctx.y() + h);
        try {
            this.embedded.render(ctx.g(), localX(ctx), localY(ctx), 0.0F);
        } finally {
            ctx.g().disableScissor();
        }
    }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        ensureInit(ctx);
        return this.embedded.mouseClicked(localX(ctx), localY(ctx), button);
    }

    @Override
    public boolean mouseScrolled(RenderContext ctx, double dX, double dY) {
        ensureInit(ctx);
        return this.embedded.mouseScrolled(localX(ctx), localY(ctx), dX, dY);
    }

    @Override
    public boolean mouseDragged(RenderContext ctx, int button, double dragX, double dragY) {
        ensureInit(ctx);
        return this.embedded.mouseDragged(localX(ctx), localY(ctx), button, dragX, dragY);
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
