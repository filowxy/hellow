package com.everyconfig.embed;

import com.everyconfig.EveryConfig;
import com.everyconfig.EveryConfigConstants;
import com.hellow.element.Element;
import com.hellow.element.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Hand-written-page element backing the {@code configEmbed} JSON type.
 *
 * <p>Width/height/mouse display are delegated to a lazily-built inner element: the
 * first time this element renders (or receives a mouse event) it asks
 * {@link EveryConfig} for the chosen config library's {@link Screen} wrapper —
 * exactly once, so the page's parent screen (the hellow page actually hosting it)
 * is used as the config screen's back-target. When no library is installed the
 * inner element is the "not installed" placeholder instead (never crashes).
 *
 * <p>Selection rules, mirroring the page JSON:
 * <ul>
 *   <li>{@code preference} — ordered library-id list; the first installed adapter wins
 *       (overrides {@code library}).</li>
 *   <li>{@code library} — an exact adapter id such as {@code cloth_config}, or
 *       {@code auto}/{@code first}/{@code null} to embed the first installed library.</li>
 *   <li>{@code height} — embed height hint (default {@link EveryConfigConstants#DEFAULT_EMBED_HEIGHT}).</li>
 * </ul>
 */
public final class ConfigEmbedElement implements Element {

    private final String library;
    private final List<String> preference;
    private final int height;

    // Delegated embed element; built once on first use (see ElementRegistry tag
    // "configEmbed" registration in EveryConfig).
    private Element inner;

    public ConfigEmbedElement(String library, List<String> preference, int height) {
        this.library = isAuto(library) ? null : library;
        this.preference = preference == null || preference.isEmpty() ? null : List.copyOf(preference);
        this.height = Math.max(1, height);
    }

    private static boolean isAuto(String value) {
        return value == null || value.equalsIgnoreCase("auto") || value.equalsIgnoreCase("first");
    }

    /** Build the delegate once; the current screen is the config screen's parent. */
    private Element ensureInner() {
        if (inner != null) return inner;
        Minecraft mc = Minecraft.getInstance();
        Screen parent = mc == null ? null : mc.screen;
        if (preference != null) {
            inner = EveryConfig.embedFirstInstalledIn(preference, parent, height);
        } else if (library != null) {
            inner = EveryConfig.embed(library, parent, height);
        } else {
            inner = EveryConfig.embedFirst(parent, height);
        }
        return inner;
    }

    @Override
    public int width(Font font) {
        return ensureInner().width(font);
    }

    @Override
    public int height(Font font, int width) {
        return ensureInner().height(font, width);
    }

    @Override
    public void render(RenderContext ctx) {
        ensureInner().render(ctx);
    }

    @Override
    public boolean mouseClicked(RenderContext ctx, int button) {
        return ensureInner().mouseClicked(ctx, button);
    }

    @Override
    public boolean mouseScrolled(RenderContext ctx, double deltaX, double deltaY) {
        return ensureInner().mouseScrolled(ctx, deltaX, deltaY);
    }

    @Override
    public boolean mouseDragged(RenderContext ctx, int button, double dragX, double dragY) {
        return ensureInner().mouseDragged(ctx, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return ensureInner().keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return ensureInner().charTyped(codePoint, modifiers);
    }

    @Override
    public Component tooltipAt(RenderContext ctx) {
        return ensureInner().tooltipAt(ctx);
    }
}