package com.tisam.client.screen;

import com.hellow.element.ButtonElement;
import com.hellow.element.Element;
import com.hellow.element.Elements;
import com.hellow.element.LinkElement;
import com.hellow.element.ListView;
import com.hellow.element.RenderContext;
import com.hellow.element.RowElement;
import com.hellow.element.ScrollElement;
import com.hellow.element.TextElement;
import com.hellow.modal.HellowModal;
import com.hellow.state.State;
import com.hellow.text.StringMode;
import com.tisam.TisamConstants;
import com.tisam.client.TisamClientEvents;
import com.tisam.config.ModuleEntry;
import com.tisam.config.ModuleEntry.ContentType;
import com.tisam.config.ModuleEntry.Priority;
import com.tisam.config.ModulesConfig;
import com.tisam.config.PreferencesData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Recommended-content list: a tabbed, scrollable card browser.
 *
 * <p>Chrome (dim layer, dialog box, title strip, bottom buttons) comes from
 * {@link HellowModal}. The card list is a hellow {@link ListView} — fixed row height,
 * rows built on demand, viewport-culled, wheel-scrollable — so a long list never
 * materializes hundreds of element objects. Each card is a {@link ModuleCardElement}
 * (a custom hellow {@link Element}) that reproduces the pre-migration card art exactly
 * and gains hover tooltips for free.
 */
public final class ModListScreen extends HellowModal {
    private static final int CARD_HEIGHT = 70;
    private static final int CARD_GAP = 6;
    private static final int TAB_ROW_HEIGHT = 20;
    private static final int GUIDE_BUTTON_HEIGHT = 20;
    private static final int BODY_HINT_MARGIN = 4;
    /** Vertical offset inside the body where the tab row and guide button live. */
    private static final int HEADER_OFFSET = BODY_HINT_MARGIN + TAB_ROW_HEIGHT + BODY_HINT_MARGIN
            + GUIDE_BUTTON_HEIGHT + BODY_HINT_MARGIN;

    private final ModulesConfig modules;
    private final List<ModuleEntry> missing;
    private final PreferencesData prefs;

    private final Map<ContentType, List<ModuleEntry>> byType = new EnumMap<>(ContentType.class);
    private ContentType activeType = ContentType.MOD;
    private List<ModuleEntry> activeList = List.of();

    private boolean showInstallGuide = false;
    private Element listView;
    /** Auto-wrapped guide panel (scrolls when the text overflows the viewport). */
    private Element guideScroll;
    private ContentType guideBuiltFor;
    private Element tabRow;
    private Element guideButton;
    /**
     * Scroll offset of the mod list, kept across {@code rebuildListView()} calls:
     * HellowModal re-inits (and rebuilds the list) whenever this screen becomes
     * current again — e.g. returning from the external-link confirm — and a fresh
     * ListView would otherwise reset the position to the top.
     */
    private final State<Integer> listScrollState = State.of(0);

    public ModListScreen(TitleScreen parent, ModulesConfig modules,
                         List<ModuleEntry> missing, PreferencesData prefs) {
        super(Component.literal(TisamConstants.MOD_NAME), parent);
        this.modules = modules;
        this.missing = missing;
        this.prefs = prefs;
        this.dialogWidth = 560;
        this.dialogHeight = 340;
        rebuildBuckets();
        this.activeType = pickDefaultTab();
        this.activeList = this.byType.getOrDefault(this.activeType, List.of());
    }

    /**
     * True when at least one still-missing entry {@code can_ignore=false}: those are
     * "must install" mods, so Later / Never Remind would otherwise skip them forever
     * (the buttons are greyed out in that case).
     */
    private boolean hasNonIgnorable() {
        for (ModuleEntry m : this.missing) {
            if (!m.canIgnore) return true;
        }
        return false;
    }

    private void rebuildBuckets() {
        this.byType.clear();
        EnumMap<ContentType, List<ModuleEntry>> tmp = new EnumMap<>(ContentType.class);
        for (ModuleEntry m : this.missing) {
            ContentType t = m.type == null ? ContentType.MOD : m.type;
            tmp.computeIfAbsent(t, k -> new ArrayList<>()).add(m);
        }
        for (Map.Entry<ContentType, List<ModuleEntry>> e : tmp.entrySet()) {
            e.getValue().sort(ModuleEntry.BY_PRIORITY);
        }
        this.byType.putAll(tmp);
    }

    private ContentType pickDefaultTab() {
        for (ContentType t : ContentType.values()) {
            List<ModuleEntry> list = this.byType.get(t);
            if (list != null && !list.isEmpty()) return t;
        }
        return ContentType.MOD;
    }

    @Override
    protected void init() {
        resetBottomButtons();
        boolean locked = hasNonIgnorable();
        addButton(Elements.button(Component.translatable("tisam.button.later").getString(),
                this::laterReminder, !locked));
        addButton(Elements.button(Component.translatable("tisam.button.never_remind").getString(),
                this::openDisclaimer, !locked));
        addButton(Elements.button(Component.translatable("tisam.modlist.exit_game").getString(),
                () -> Minecraft.getInstance().stop()));
        super.init();

        // Tab row: one hellow button per content type, disabled when that tab is empty.
        // Equal-width slots (EqualWidthRow) so the row always fills exactly the body
        // width — flex's "never shrink" rule let a tight window overflow the rightmost
        // tab past the dialog edge, so equal slots own the whole width instead.
        java.util.List<Element> tabs = new java.util.ArrayList<>();
        for (ContentType t : ContentType.values()) {
            int count = this.byType.getOrDefault(t, List.of()).size();
            String label = Component.translatable("tisam.modlist.tab." + t.name().toLowerCase()).getString()
                    + " (" + count + ")";
            tabs.add(new ButtonElement(label, () -> switchTab(t), null, null, null, count > 0));
        }
        this.tabRow = new EqualWidthRow(tabs, 4);

        // Guide toggle button below the tabs.
        this.guideButton = new ButtonElement(
                Component.translatable("tisam.modlist.guide").getString(),
                () -> this.showInstallGuide = !this.showInstallGuide);

        rebuildListView();
    }

    /** Builds a fresh ListView for the active tab (fresh instance, offset state shared). */
    private void rebuildListView() {
        int viewport = listViewportHeight();
        this.listView = new ListView(this.activeList.size(), CARD_HEIGHT,
                CARD_GAP, viewport, index -> new ModuleCardElement(this.activeList.get(index), this::openLink),
                this.listScrollState);
    }

    private int listViewportHeight() {
        return Math.max(0, bodyHeight() - HEADER_OFFSET - BODY_HINT_MARGIN);
    }

    private int listTop() {
        return bodyTop() + HEADER_OFFSET;
    }

    @Override
    public void tick() {
        super.tick();
        // Drive the inertia fling of the list and the guide panel.
        if (!this.showInstallGuide && this.listView instanceof ListView lv) {
            lv.advanceFling();
        }
        if (this.showInstallGuide && this.guideScroll instanceof ScrollElement se) {
            se.advanceFling(listContext(0, 0));
        }
    }

    @Override
    protected void renderBody(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (this.tabRow != null) {
            RenderContext tabCtx = new RenderContext(g, this.font, bodyLeft(),
                    bodyTop() + BODY_HINT_MARGIN, bodyWidth(), mouseX, mouseY);
            this.tabRow.render(tabCtx);
            RenderContext guideCtx = new RenderContext(g, this.font, bodyLeft(),
                    bodyTop() + BODY_HINT_MARGIN + TAB_ROW_HEIGHT + BODY_HINT_MARGIN,
                    bodyWidth(), mouseX, mouseY);
            this.guideButton.render(guideCtx);
        }
        if (this.showInstallGuide) {
            renderGuide(g, mouseX, mouseY);
            return;
        }
        if (this.activeList.isEmpty()) {
            g.drawString(this.font, Component.translatable("tisam.modlist.empty_for_type"),
                    bodyLeft(), listTop(), TisamConstants.TEXT_MUTED, true);
            return;
        }
        RenderContext ctx = new RenderContext(g, this.font, bodyLeft(), listTop(),
                bodyWidth(), mouseX, mouseY);
        this.listView.render(ctx);
        Component tooltip = this.listView.tooltipAt(ctx);
        if (tooltip != null) {
            g.renderTooltip(this.font, List.of(tooltip),
                    Optional.<net.minecraft.world.inventory.tooltip.TooltipComponent>empty(), mouseX, mouseY);
        }
    }

    /**
     * Guide panel: each newline-then-width-wrapped paragraph is a {@link TextElement}
     * inside a {@link ScrollElement}, so long guides wrap automatically and — when the
     * wrapped text exceeds the viewport — a scrollbar appears automatically. Rebuilt
     * only when the active tab changes.
     */
    private void renderGuide(GuiGraphics g, int mouseX, int mouseY) {
        if (this.guideBuiltFor != this.activeType || this.guideScroll == null) {
            this.guideScroll = buildGuideElement();
            this.guideBuiltFor = this.activeType;
        }
        RenderContext ctx = new RenderContext(g, this.font, bodyLeft(), listTop(),
                bodyWidth(), mouseX, mouseY);
        this.guideScroll.render(ctx);
        Component tooltip = this.guideScroll.tooltipAt(ctx);
        if (tooltip != null) {
            g.renderTooltip(this.font, List.of(tooltip),
                    Optional.<net.minecraft.world.inventory.tooltip.TooltipComponent>empty(), mouseX, mouseY);
        }
    }

    private Element buildGuideElement() {
        String guide = unescapeNewlines(guideFor(this.activeType));
        List<Element> paragraphs = new ArrayList<>();
        for (String raw : guide.split("\n")) {
            // WRAP mode soft-wraps inside the available width; height grows with lines.
            paragraphs.add(new TextElement(Component.literal(raw), TisamConstants.GUIDE_BODY_COLOR, true));
        }
        return new ScrollElement(paragraphs, 4, listViewportHeight());
    }

    /** Turns literal {@code \n} (backslash + n from a mis-escaped JSON string) into real newlines. */
    private static String unescapeNewlines(String s) {
        return s == null ? "" : s.replace("\\n", "\n");
    }

    /**
     * Per-entry override > global config > per-type default. The guide panel shows one
     * guide for the active tab, so the "per-entry override" is the first entry of the
     * active list that carries its own {@code install_guide_text}.
     */
    private String guideFor(ContentType type) {
        for (ModuleEntry entry : this.activeList) {
            if (entry.installGuideText != null && !entry.installGuideText.isEmpty()) {
                return entry.installGuideText;
            }
        }
        if (this.modules.installGuideText != null
                && !this.modules.installGuideText.equals(TisamConstants.DEFAULT_INSTALL_GUIDE_TEXT)) {
            return this.modules.installGuideText;
        }
        return TisamConstants.DEFAULT_INSTALL_GUIDE_BY_TYPE
                .getOrDefault(type, TisamConstants.DEFAULT_INSTALL_GUIDE_TEXT);
    }

    // -------- hellow element event forwarding (PageScreen-style, null-g contexts) --------

    private RenderContext listContext(int mouseX, int mouseY) {
        return new RenderContext(null, this.font, bodyLeft(), listTop(), bodyWidth(), mouseX, mouseY);
    }

    private RenderContext tabContext(int mouseX, int mouseY) {
        return new RenderContext(null, this.font, bodyLeft(), bodyTop() + BODY_HINT_MARGIN, bodyWidth(), mouseX, mouseY);
    }

    private RenderContext guideButtonContext(int mouseX, int mouseY) {
        return new RenderContext(null, this.font, bodyLeft(),
                bodyTop() + BODY_HINT_MARGIN + TAB_ROW_HEIGHT + BODY_HINT_MARGIN, bodyWidth(), mouseX, mouseY);
    }

    /** Forwards a header (tab row + guide toggle) hit; returns true if consumed. */
    private boolean headerClicked(double mouseX, double mouseY, int button) {
        if (this.tabRow != null
                && this.tabRow.mouseClicked(tabContext((int) mouseX, (int) mouseY), button)) {
            return true;
        }
        return this.guideButton != null
                && this.guideButton.mouseClicked(guideButtonContext((int) mouseX, (int) mouseY), button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (headerClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (!this.showInstallGuide && this.listView != null
                && this.listView.mouseClicked(listContext((int) mouseX, (int) mouseY), button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (this.tabRow != null
                && this.tabRow.mouseScrolled(tabContext((int) mouseX, (int) mouseY), deltaX, deltaY)) {
            return true;
        }
        if (this.guideButton != null
                && this.guideButton.mouseScrolled(guideButtonContext((int) mouseX, (int) mouseY), deltaX, deltaY)) {
            return true;
        }
        if (this.showInstallGuide) {
            if (this.guideScroll != null
                    && this.guideScroll.mouseScrolled(listContext((int) mouseX, (int) mouseY), deltaX, deltaY)) {
                return true;
            }
        } else if (this.listView != null
                && this.listView.mouseScrolled(listContext((int) mouseX, (int) mouseY), deltaX, deltaY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.tabRow != null
                && this.tabRow.mouseDragged(tabContext((int) mouseX, (int) mouseY), button, dragX, dragY)) {
            return true;
        }
        if (this.guideButton != null
                && this.guideButton.mouseDragged(guideButtonContext((int) mouseX, (int) mouseY), button, dragX, dragY)) {
            return true;
        }
        if (this.showInstallGuide) {
            if (this.guideScroll != null
                    && this.guideScroll.mouseDragged(listContext((int) mouseX, (int) mouseY), button, dragX, dragY)) {
                return true;
            }
        } else if (this.listView != null
                && this.listView.mouseDragged(listContext((int) mouseX, (int) mouseY), button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    // -------- Actions --------

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC = "Later". When a must-install (can_ignore=false) module is missing,
        // Later/Never are locked — ESC just leaves the screen (no reminder consumed,
        // the next title-screen visit will prompt again).
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

    private void switchTab(ContentType type) {
        this.activeType = type;
        this.activeList = this.byType.getOrDefault(type, List.of());
        // A new tab starts at the top — the shared scroll state must be reset, or
        // switching tabs would inherit the previous tab's position.
        this.listScrollState.set(0);
        rebuildListView();
        // The guide text depends on the active tab; force a rebuild next render.
        this.guideBuiltFor = null;
    }

    private void openLink(String url) {
        // Parent = this screen, so Cancel/Open return to the mod list with the current
        // tab and scroll position preserved (not the title screen).
        Minecraft.getInstance().setScreen(new ExternalLinkConfirmScreen(this, url));
    }

    private void laterReminder() {
        TisamClientEvents.onPromptClosed(true);
        Minecraft.getInstance().setScreen(this.parent);
    }

    private void openDisclaimer() {
        Minecraft.getInstance().setScreen(new DisclaimerScreen(this, this.parent, modules, prefs));
    }

    /**
     * Equal-width row of children: every child gets the same share of the available
     * width (last child absorbs the rounding remainder), so the row always fills
     * exactly the container and never overflows it. Children stay centered in their
     * slot — a vanilla-styled {@link ButtonElement} centers its own label text.
     */
    private static final class EqualWidthRow implements Element {
        private final List<Element> children;
        private final int gap;

        EqualWidthRow(List<Element> children, int gap) {
            this.children = children;
            this.gap = gap;
        }

        @Override
        public int width(Font font) {
            int w = 0;
            for (int i = 0; i < this.children.size(); i++) {
                w += this.children.get(i).width(font);
                if (i < this.children.size() - 1) w += this.gap;
            }
            return w;
        }

        @Override
        public int height(Font font, int width) {
            int[] slot = slotWidths(width);
            int h = 0;
            for (int i = 0; i < this.children.size(); i++) {
                h = Math.max(h, this.children.get(i).height(font, slot[i]));
            }
            return h;
        }

        @Override
        public void render(RenderContext ctx) {
            int n = this.children.size();
            if (n == 0) return;
            int[] slot = slotWidths(ctx.width());
            int x = ctx.x();
            for (int i = 0; i < n; i++) {
                this.children.get(i).render(new RenderContext(ctx.g(), ctx.font(), x, ctx.y(),
                        slot[i], ctx.mouseX(), ctx.mouseY()));
                x += slot[i] + this.gap;
            }
        }

        @Override
        public boolean mouseClicked(RenderContext ctx, int button) {
            int n = this.children.size();
            if (n == 0) return false;
            int[] slot = slotWidths(ctx.width());
            int x = ctx.x();
            for (int i = 0; i < n; i++) {
                RenderContext c = new RenderContext(ctx.g(), ctx.font(), x, ctx.y(),
                        slot[i], ctx.mouseX(), ctx.mouseY());
                if (this.children.get(i).mouseClicked(c, button)) return true;
                x += slot[i] + this.gap;
            }
            return false;
        }

        @Override
        public Component tooltipAt(RenderContext ctx) {
            int[] slot = slotWidths(ctx.width());
            int x = ctx.x();
            for (int i = 0; i < this.children.size(); i++) {
                Component tooltip = this.children.get(i).tooltipAt(new RenderContext(ctx.g(), ctx.font(),
                        x, ctx.y(), slot[i], ctx.mouseX(), ctx.mouseY()));
                if (tooltip != null) return tooltip;
                x += slot[i] + this.gap;
            }
            return null;
        }

        /** Per-child slot widths: equal shares, last child takes the rounding remainder. */
        private int[] slotWidths(int available) {
            int n = this.children.size();
            int[] slot = new int[n];
            if (n == 0) return slot;
            int gaps = this.gap * (n - 1);
            int share = Math.max(1, (available - gaps) / n);
            int total = 0;
            for (int i = 0; i < n; i++) {
                slot[i] = share;
                total += share;
            }
            // Never hand a negative width to a child (e.g. a tiny dialog where even
            // the gaps don't fit) — vanilla widgets misbehave on negative sizes.
            slot[n - 1] = Math.max(0, slot[n - 1] + available - gaps - total);
            return slot;
        }
    }

    /**
     * One card in the list — a custom hellow {@link Element}. Only the decorative
     * shell (background fill, 1px border, fixed height) is drawn here; the content
     * (name/lock, clamped description, license/priority row, clickable clamped URL)
     * is a hellow element tree, so wrapping, truncation, hover and tooltips are all
     * hellow's work.
     */
    private static final class ModuleCardElement implements Element {
        private static final int CONTENT_LEFT = 4;
        private static final int CONTENT_TOP = 5;

        private final ModuleEntry entry;
        private final java.util.function.Consumer<String> onOpenLink;
        private Element content;

        ModuleCardElement(ModuleEntry entry, java.util.function.Consumer<String> onOpenLink) {
            this.entry = entry;
            this.onOpenLink = onOpenLink;
            // Build the content tree eagerly: ListView re-creates each row element on
            // every frame (visibility culling), so a lazily-built content field would
            // only ever exist on the render instance — the mouse-click instance would
            // still have null content and swallow the click ("hover works, click dead").
            this.content = buildContent();
        }

        @Override
        public int width(Font font) {
            // Bounded by the container; the ListView hands each row its viewport width.
            return 0;
        }

        @Override
        public int height(Font font, int width) {
            return CARD_HEIGHT;
        }

        private int bgColor() {
            if (!this.entry.canIgnore) return TisamConstants.CARD_BG_LOCKED;
            return this.entry.priority == Priority.FEATURED ? TisamConstants.CARD_BG_FEATURED
                    : this.entry.priority == Priority.CRITICAL ? TisamConstants.CARD_BG_CRITICAL
                    : TisamConstants.CARD_BG_NORMAL;
        }

        private int borderColor() {
            if (!this.entry.canIgnore) return TisamConstants.CARD_BORDER_LOCKED;
            return this.entry.priority == Priority.FEATURED ? TisamConstants.CARD_BORDER_FEATURED
                    : this.entry.priority == Priority.CRITICAL ? TisamConstants.CARD_BORDER_CRITICAL
                    : TisamConstants.CARD_BORDER_NORMAL;
        }

        @Override
        public void render(RenderContext ctx) {
            int x = ctx.x();
            int y = ctx.y();
            int w = ctx.width();
            // Decorative shell.
            ctx.g().fill(x, y, x + w, y + CARD_HEIGHT, bgColor());
            ctx.g().renderOutline(x, y, w, CARD_HEIGHT, borderColor());
            this.content.render(innerCtx(ctx));
        }

        /** Declare the card content as a hellow element tree. */
        private Element buildContent() {
            boolean locked = !this.entry.canIgnore;
            java.util.List<Element> top = new java.util.ArrayList<>();
            top.add(new TextElement(Component.literal(this.entry.name), TisamConstants.CARD_NAME_COLOR));
            if (locked) {
                top.add(new TextElement(Component.literal("\uD83D\uDD12"), TisamConstants.CARD_BORDER_LOCKED));
            }
            top.add(Elements.fillSpacer());
            String typeLabel = this.entry.type != null ? this.entry.type.name() : "MOD";
            top.add(new TextElement(Component.literal(typeLabel), TisamConstants.CARD_TYPE_COLOR));

            java.util.List<Element> bottom = new java.util.ArrayList<>();
            if (this.entry.license != null && !this.entry.license.isEmpty()) {
                bottom.add(new TextElement(Component.literal(this.entry.license), TisamConstants.CARD_TYPE_COLOR));
            }
            bottom.add(Elements.fillSpacer());
            String priorityLabel = locked ? "REQUIRED"
                    : (this.entry.priority != null ? this.entry.priority.name() : "NORMAL");
            int priorityColor = locked ? TisamConstants.CARD_BORDER_LOCKED
                    : this.entry.priority == Priority.CRITICAL ? TisamConstants.CARD_BORDER_CRITICAL
                    : this.entry.priority == Priority.FEATURED ? TisamConstants.CARD_BORDER_FEATURED
                    : TisamConstants.CARD_TYPE_COLOR;
            bottom.add(new TextElement(Component.literal(priorityLabel), priorityColor));

            java.util.List<Element> rows = new java.util.ArrayList<>();
            rows.add(new RowElement(top, 6));
            // fill(...) stretches the CLAMPED cell across the column's spare width —
            // ColumnElement hands fill children the shared spare, so the ellipsis
            // truncation actually gets a bounded width (without fill the cell is
            // measured at its full natural width and never truncates).
            rows.add(Elements.fill(TextElement.clamped(Component.literal(this.entry.description),
                    TisamConstants.CARD_DESC_COLOR)));
            rows.add(new RowElement(bottom, 6));
            if (this.entry.url != null && !this.entry.url.isEmpty()) {
                rows.add(Elements.fill(new LinkElement(Component.literal(this.entry.url), this.entry.url,
                        TisamConstants.URL_ICON_COLOR, this.onOpenLink,
                        Component.literal(this.entry.url), StringMode.CLAMPED)));
            }
            return new com.hellow.element.ColumnElement(rows, 2);
        }

        private RenderContext innerCtx(RenderContext ctx) {
            return new RenderContext(ctx.g(), ctx.font(),
                    ctx.x() + CONTENT_LEFT, ctx.y() + CONTENT_TOP,
                    Math.max(1, ctx.width() - CONTENT_LEFT * 2), ctx.mouseX(), ctx.mouseY());
        }

        @Override
        public boolean mouseClicked(RenderContext ctx, int button) {
            return this.content.mouseClicked(innerCtx(ctx), button);
        }

        @Override
        public Component tooltipAt(RenderContext ctx) {
            return this.content.tooltipAt(innerCtx(ctx));
        }
    }
}