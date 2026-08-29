package com.tisam.client.screen;

import com.hellow.element.ColumnElement;
import com.hellow.element.Element;
import com.hellow.element.Elements;
import com.hellow.element.RenderContext;
import com.hellow.element.TextElement;
import com.hellow.modal.HellowModal;
import com.tisam.TisamConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * v2.0 external-link safety popup. Shown before any external URL is
 * opened, so the player gets one click to verify what's about to happen.
 *
 * <p>Behaviour per 最新更新.md §4:
 * <ul>
 *   <li>URL length &le; {@link TisamConstants#EXTERNAL_LINK_URL_LIMIT} (60) →
 *       three buttons: [Copy URL] [Cancel] [Open].</li>
 *   <li>URL length &gt; 60 → safety-warning modal, no "Open" button.
 *       Long URLs are a soft signal of obfuscation; the player can still
 *       copy and inspect them manually before deciding.</li>
 * </ul>
 *
 * <p>Implementation: extends {@link HellowModal} so the dim layer, dialog
 * box, title, body region, and bottom-row button layout all come from one
 * place. The body is a hellow element column; the URL line is a CLAMPED
 * {@link TextElement} so ellipsization is hellow's job, not hand-rolled.
 */
public final class ExternalLinkConfirmScreen extends HellowModal {
    private final String url;
    private final boolean canOpen;
    private Element body;

    public ExternalLinkConfirmScreen(net.minecraft.client.gui.screens.Screen parent, String url) {
        super(Component.translatable("tisam.external_link.title"), parent);
        this.url = url == null ? "" : url;
        this.canOpen = this.url.length() <= TisamConstants.EXTERNAL_LINK_URL_LIMIT;
        // Width / height tuned so the URL line is readable on a 60-char URL
        // at GUI scale 2 (the typical player default). 420px fits comfortably.
        this.dialogWidth = 420;
        this.dialogHeight = 180;
        this.titleArea = 56;       // taller title area for the warning body
        this.buttonArea = 32;
    }

    @Override
    protected void init() {
        // (ponytail: buttons are now declared as hellow elements — HellowModal's
        //  bottom button row layouts, renders and forwards clicks for them. Hellow's
        //  role stays the *layout*; the widget is now a declarative hellow element.)
        resetBottomButtons();
        addButton(Elements.button(Component.translatable("tisam.external_link.copy").getString(),
                this::copyToClipboard));
        addButton(Elements.button(Component.translatable("tisam.external_link.cancel").getString(),
                () -> this.minecraft.setScreen(this.parent)));
        if (this.canOpen) {
            addButton(Elements.button(Component.translatable("tisam.external_link.open").getString(),
                    () -> {
                        openUrl(this.url);
                        this.minecraft.setScreen(this.parent);
                    }));
        }
        super.init();
        rebuildBody();
    }

    /** Declare the body column; the URL is CLAMPED by a hellow {@link TextElement}. */
    private void rebuildBody() {
        List<Element> rows = new ArrayList<>();
        rows.add(new TextElement(Component.translatable("tisam.external_link.body1")
                .withStyle(ChatFormatting.WHITE), 0xFFFFFFFF));
        rows.add(new TextElement(Component.translatable("tisam.external_link.body2")
                .withStyle(ChatFormatting.GRAY), 0xFFAAAAAA));
        int urlColor = this.canOpen ? 0xFF55AAFF : 0xFFFF8866;
        // fill(...) bounds the CLAMPED URL to the dialog width — without it the cell
        // is measured at its full natural width and a 200-char URL would overflow.
        rows.add(Elements.fill(TextElement.clamped(Component.literal(this.url), urlColor)));
        if (!this.canOpen) {
            rows.add(new TextElement(Component.translatable("tisam.external_link.too_long")
                    .withStyle(ChatFormatting.RED), 0xFFFF5555));
        }
        this.body = new ColumnElement(rows, 6);
    }

    private RenderContext bodyContext(int mouseX, int mouseY) {
        return new RenderContext(null, this.font, bodyLeft(), bodyTop() + 4, bodyWidth(), mouseX, mouseY);
    }

    /**
     * Draw the body region (the hellow element column). Note the URL line is
     * CLAMPED by {@link TextElement} — a 200-char URL can never wrap the dialog box.
     */
    @Override
    protected void renderBody(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (this.body == null) return;
        RenderContext ctx = new RenderContext(g, this.font, bodyLeft(), bodyTop() + 4,
                bodyWidth(), mouseX, mouseY);
        this.body.render(ctx);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.body != null
                && this.body.mouseClicked(bodyContext((int) mouseX, (int) mouseY), button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void copyToClipboard() {
        if (this.minecraft == null) return;
        // KeyboardHandler.setClipboard is the documented Minecraft 1.21.1
        // idiom for setting the system clipboard from a screen.
        this.minecraft.keyboardHandler.setClipboard(this.url);
        TisamConstants.LOG.info("[tisam] External URL copied to clipboard (len={})", this.url.length());
    }

    private void openUrl(String url) {
        if (url == null || url.isEmpty()) return;
        try {
            // Util.getPlatform().openUri(URI) is the documented NeoForge
            // / vanilla-Minecraft 1.21.1 entry point — it routes through
            // the platform's privacy/permission flow before opening.
            Util.getPlatform().openUri(URI.create(url));
        } catch (Exception e) {
            TisamConstants.LOG.error("[tisam] Failed to open external URL (len={})", url.length(), e);
        }
    }
}
