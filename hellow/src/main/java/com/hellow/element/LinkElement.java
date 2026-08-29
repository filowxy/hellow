package com.hellow.element;

import com.hellow.text.StringMode;
import com.hellow.text.TextRenderer;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.util.function.Consumer;

public final class LinkElement implements Element {
    private static final int COLOR_HOVER_BRIGHTEN = 0x00202020;

    private final Component text;
    private final String url;
    private final int color;
    private final Consumer<String> onOpen;
    private final Component tooltip;
    /** {@code null} = draw the whole text; CLAMPED = ellipsis-truncate to fit width. */
    private final StringMode mode;

    public LinkElement(Component text, String url, int color) {
        this(text, url, color, LinkElement::openUrlDefault, null, null);
    }

    /**
     * @param onOpen invoked when the link is clicked; receives the raw url. A
     *               {@code null} handler keeps the element inert (caller-owned click).
     */
    public LinkElement(Component text, String url, int color, Consumer<String> onOpen) {
        this(text, url, color, onOpen, null, null);
    }

    public LinkElement(Component text, String url, int color, Consumer<String> onOpen, Component tooltip) {
        this(text, url, color, onOpen, tooltip, null);
    }

    /** Full constructor; {@code mode} may be {@code null} (draw whole text) or CLAMPED. */
    public LinkElement(Component text, String url, int color, Consumer<String> onOpen,
                       Component tooltip, StringMode mode) {
        this.text = text;
        this.url = url;
        this.color = color;
        this.onOpen = onOpen == null ? ignored -> {} : onOpen;
        this.tooltip = tooltip;
        this.mode = mode;
    }

    public int width(Font font) {
        return font.width(this.text);
    }

    public int height(Font font, int width) {
        return font.lineHeight;
    }

    public void render(RenderContext ctx) {
        int c = this.color;
        if (hit(ctx)) c += COLOR_HOVER_BRIGHTEN;
        if (this.mode == StringMode.CLAMPED) {
            TextRenderer.Layout layout = TextRenderer.layout(ctx.font(), this.text, Math.max(1, ctx.width()), StringMode.CLAMPED);
            if (!layout.isEmpty()) {
                ctx.g().drawString(ctx.font(), layout.lines().get(0), ctx.x(), ctx.y(), c, true);
            }
        } else {
            ctx.g().drawString(ctx.font(), this.text, ctx.x(), ctx.y(), c, true);
        }
    }

    public String url() { return this.url; }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        if (button != 0) return false;
        if (!hit(ctx)) return false;
        this.onOpen.accept(this.url);
        return true;
    }

    @Override
    public Component tooltipAt(RenderContext ctx) {
        return hit(ctx) ? this.tooltip : null;
    }

    private boolean hit(RenderContext ctx) {
        return ctx.mouseX() >= ctx.x() && ctx.mouseX() <= ctx.x() + renderedWidth(ctx)
                && ctx.mouseY() >= ctx.y() && ctx.mouseY() <= ctx.y() + this.height(ctx.font(), ctx.width());
    }

    /**
     * Width of the actually-drawn text. CLAMPED truncates to the layout budget, so
     * the hit test must use the truncated width — otherwise a long URL that is
     * visually ellipsized still claims the whole natural width as clickable surface
     * (and cast a hover state over text that isn't there).
     */
    private int renderedWidth(RenderContext ctx) {
        if (this.mode == StringMode.CLAMPED) {
            TextRenderer.Layout layout = TextRenderer.layout(ctx.font(), this.text,
                    Math.max(1, ctx.width()), StringMode.CLAMPED);
            return layout.isEmpty() ? 0 : ctx.font().width(layout.lines().get(0));
        }
        return ctx.font().width(this.text);
    }

    /** Default click behaviour: open the URL via the platform (NeoForge privacy flow). */
    private static void openUrlDefault(String url) {
        if (url == null || url.isEmpty()) return;
        try {
            Util.getPlatform().openUri(URI.create(url));
        } catch (Exception e) {
            // Never crash the client over a bad link; log and continue.
            com.hellow.HellowConstants.log().warn("[hellow] failed to open link '{}'", url);
        }
    }
}
