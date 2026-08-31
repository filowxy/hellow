package com.hellow.element;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Embeds an arbitrary third-party {@link Screen} into a hellow layout as an element:
 * renders the screen inside the element's rectangle and forwards mouse/keyboard
 * events with the element's coordinates. This is how everyconfig-style mods surface
 * another config library's UI inside a hellow page.
 *
 * <p><b>Scaling.</b> Config screens (Cloth Config, REI, ...) lay out against a
 * full-window size, so squeezed into a small embed rectangle their content becomes
 * unreadable. Instead the embedded screen is initialized against a fixed
 * {@link #DESIGN_W}/{@link #DESIGN_H} virtual canvas (a typical config-window
 * resolution) and the whole thing is scaled down to fit the element rectangle —
 * content stays properly laid out and fully visible. Mouse coordinates are divided
 * by the same scale before being forwarded, so the embedded screen's own scrolling
 * (list scrollbar, drag) still works.
 *
 * <p>Note (ponytail): the element's {@code height} hint sets the *element* size; the
 * embedded screen is always laid out at the fixed design resolution and scaled to
 * fit, so a taller element shows the config screen larger, never squeezed.
 */
public final class ScreenEmbedElement implements Element {

    // Fixed virtual resolution the embedded screen is laid out against — a typical
    // config-window size that keeps labels/rows readable before scaling to fit.
    private static final int DESIGN_W = 320;
    private static final int DESIGN_H = 240;

    private final Screen embedded;
    private final int hintHeight;

    // Whether the embedded screen has been initialized at the design resolution.
    private boolean initialized = false;

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

    /** Element-local mouse X inside the rectangle. */
    private static int localX(RenderContext ctx) {
        return ctx.mouseX() - ctx.x();
    }

    /** Element-local mouse Y inside the rectangle. */
    private static int localY(RenderContext ctx) {
        return ctx.mouseY() - ctx.y();
    }

    /**
     * Uniform scale that fits the fixed {@link #DESIGN_W}/{@link #DESIGN_H} canvas
     * into the element rectangle (the embedded screen keeps its aspect).
     */
    private float scale(RenderContext ctx) {
        int rectW = Math.max(1, ctx.width());
        int rectH = Math.max(1, height(ctx.font(), rectW));
        return Math.min((float) rectW / DESIGN_W, (float) rectH / DESIGN_H);
    }

    /** Lay the embedded screen out against the fixed design resolution once. */
    private void ensureInit(RenderContext ctx) {
        if (this.initialized) return;
        this.initialized = true;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            this.embedded.init(mc, DESIGN_W, DESIGN_H);
        }
    }

    /** Element-local mouse coordinates mapped into the embedded screen's design space. */
    private int designX(RenderContext ctx) {
        return (int) (localX(ctx) / scale(ctx));
    }

    private int designY(RenderContext ctx) {
        return (int) (localY(ctx) / scale(ctx));
    }

    @Override
    public void render(RenderContext ctx) {
        if (ctx.g() == null) return;
        ensureInit(ctx);
        float s = scale(ctx);
        GuiGraphics g = ctx.g();
        // Fit the virtual canvas into the element rectangle (top-left aligned).
        g.pose().pushPose();
        g.pose().translate(ctx.x(), ctx.y(), 0.0F);
        g.pose().scale(s, s, 1.0F);
        g.enableScissor(ctx.x(), ctx.y(), ctx.x() + ctx.width(), ctx.y() + height(ctx.font(), ctx.width()));
        try {
            this.embedded.render(g, designX(ctx), designY(ctx), 0.0F);
        } finally {
            g.disableScissor();
            g.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        ensureInit(ctx);
        return this.embedded.mouseClicked(designX(ctx), designY(ctx), button);
    }

    @Override
    public boolean mouseScrolled(RenderContext ctx, double dX, double dY) {
        ensureInit(ctx);
        return this.embedded.mouseScrolled(designX(ctx), designY(ctx), dX, dY);
    }

    @Override
    public boolean mouseDragged(RenderContext ctx, int button, double dragX, double dragY) {
        ensureInit(ctx);
        return this.embedded.mouseDragged(designX(ctx), designY(ctx), button, dragX, dragY);
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
