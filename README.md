# Hellow — Declarative UI Library & Window Manager

A client-only UI toolkit for **NeoForge 1.21.1** mods. Describe what your UI should look like and let Hellow handle drawing, input, scrolling, and window flow — no manual widget math, no per-pixel layout code.

> The caller defines content; Hellow owns presentation and the window lifecycle. Hellow never inspects or trusts the content it is given.

---

## What Hellow does for you

### 1. Declarative UI without boilerplate

Compose screens from a small element tree instead of hand-placing coordinates. Hellow renders, hit-tests, and forwards input for the whole tree automatically.

```java
// One dialog, five lines: column -> text + button
Hellow.showDialog(
    Component.literal("Welcome"),
    Elements.column(6,
        Elements.text("Hello, world!"),
        Elements.wrappedText("Long paragraphs wrap on their own.", 0xEEEEEE),
        Elements.button("Got it", Hellow::closeTop)));
```

### 2. Windows & page flow

- **Window stack**: open pages as windows; ESC or `closeTop()` returns to the exact screen you came from.
- **Three layers**: normal pages, stacked modals, and top-level floats that stay above everything.
- **Dialogs & modals out of the box**: dim overlay, centered box, title strip, body region, self-layouting bottom button row. Dialog size auto-fits the window and never runs off screen.

```java
// Register a reusable page, then open it by id anywhere
HellowPages.register(PageBuilder.of("changelog")
    .title("my_mod.page.changelog")
    .layer(PageLayer.MODAL)
    .body(Elements.column(6, Elements.wrappedText(changelogText, 0xDDDDDD)))
    .build());

Hellow.show("changelog");   // open it
Hellow.closeTop();          // ESC-equivalent close
Hellow.closeAll();          // unwind every hellow window
```

Custom modals extend `HellowModal`: declare the body in `renderBody()` and the buttons in `init()`.

```java
class ConfirmScreen extends HellowModal {
    ConfirmScreen() { super(Component.literal("Confirm"), parentScreen); }

    @Override protected void init() {
        resetBottomButtons();                       // always call — buttons would duplicate otherwise
        addButton(Elements.button("Confirm", this::confirm));
        addButton(Elements.button("Cancel", this::cancel));
        super.init();
    }

    @Override protected void renderBody(GuiGraphics g, int mx, int my, float pt) {
        g.drawString(this.font, "Are you sure?", bodyLeft(), bodyTop(), 0xFFFFFF, true);
    }
}
```

### 3. Event-driven automation

Decide *when* UI appears in one place — no polling logic scattered through your mod:

- when a specific **screen opens**,
- when a **NeoForge game event** fires,
- or when **your own API reports content** (polled on the client tick).

Control frequency with simple strategies: **once**, **every time**, or a **decay schedule** (e.g. every launch → every 3rd → every 7th).

```java
// When the title screen opens, show a screen (only once)
Hellow.when("my_mod", s -> s instanceof TitleScreen,
    s -> new MyScreen(s), Hellow.FireStrategy.once());

// When a custom game event fires and the test passes, pop a screen every time
Hellow.whenEvent("my_mod", MyGameEvent.class,
    e -> e.value() > 0, e -> new NoticeScreen(e.value()),
    Hellow.FireStrategy.everyTime());

// Poll your own API until it has content (throttled to every 20 ticks), show&shut
Hellow.whenApi("my_mod", () -> myApi.result(),
    r -> new ResultScreen(r), Hellow.FireStrategy.once());
```

### 4. Form controls with state

Toggle switches, sliders, text inputs, dropdown/enum selectors. Back them with a `State` value so the UI is fully controlled.

```java
State<Boolean> fastTravel = State.of(true);
State<Integer> renderDist = State.of(8);
State<String> name = State.of("Steve");

Elements.column(6,
    Elements.toggle("Fast travel", fastTravel),
    Elements.slider("Render distance", 2, 32, renderDist),
    Elements.textInput("Player name", name),
    Elements.enumSelect("Quality", List.of("Low", "Med", "High"),
        State.of("Med")));
```

Declarative events: give a control an id and receive `pressed` / `value-changed` on the NeoForge bus — UI and logic stay decoupled.

```java
Elements.button("Save", this::save, ElementId.of("my_mod", "save_btn"));

@SubscribeEvent
public void onPressed(HellowElementPressedEvent e) {
    if (e.elementId().equals("my_mod", "save_btn")) { save(); }
}
```

### 5. Text that just wraps

Three text modes: plain single-line, **auto-wrapping** paragraphs, and **ellipsis-clamped** single lines for URLs / filenames / descriptions.

```java
Elements.text("plain");                     // single line, no wrap
Elements.wrappedText("Wraps at the container width.", 0xDDDDDD);
Elements.clampedText("https://very.long.url/...", 0x55AAFF);   // ellipsis …
// Clamped text should sit in a fill()/flex slot that stretches to the container width,
// so it actually gets a bounded budget:
Elements.fill(Elements.clampedText(description, 0xAAAAAA));
```

### 6. Layout engine

- **Column / Row / flex** — distribute spare width by weight.
- **Equal-width rows** for tabs and toolbars.
- **Grids** with per-column weights; **auto-wrapping rows** for tags/chips.
- **Scrollable regions** and **virtualized lists**: only visible rows are built, so thousands of entries stay cheap.
- Absolute positioning when you need pixel control.

```java
Elements.row(6,
    Elements.flex(Elements.button("Fill left", a), 2),   // #1 takes 2/3 of spare
    Elements.button("Right", b),                          // #2 keeps natural width
    Elements.fillSpacer());                               // pure flexible gap

Elements.grid(3, 6, new int[]{1, 1, 2}, cell1, cell2, cell3);  // 1:1:2 columns

// Virtualized list: rows are built on demand, viewport-culled
Elements.listView(itemCount, ITEM_H, 4, viewportH, index -> cardFor(index));

// Scrollable region whose position survives rebuilds (pass the same State back)
Elements.scroll(scrollState, viewportH, 6, paragraph1, paragraph2);
```

### 7. Smooth, familiar scrolling

Wheel scrolling with **inertia**, a **draggable scrollbar**, and strict viewport clipping — clicks never land in scrolled-away space, content never bleeds out.

### 8. Vanilla-consistent look and feel

Buttons are real **vanilla buttons** (texture, hover, disabled state, click sound). Hover tooltips, keyboard input into text fields, and focus-guarded controls come free.

```java
Elements.button("Install", this::install, Tooltip.create(Component.literal("Opens the official page")));
Elements.button("Later", this::later, false);   // disabled — greyed out, clicks ignored
```

### 9. Theming & translations

One `HellowTheme` object controls all colors — swap it to restyle everything. Per-mod translation sources with fallback chains resolve UI text at runtime.

```java
I18n.registerSource(new TranslationSource() {
    @Override public String modId() { return "my_mod"; }
    @Override public String get(String key, String language) { return translations.get(key); }
    @Override public Set<String> supportedLanguages() { return translations.keySet(); }
});
Component title = I18n.translatable("my_mod", "my_mod.page.title", I18n.DEFAULT_LANGUAGE);
// Packaged lang files (assets/<modId>/lang/*.json) are picked up via vanilla's loader too.
```

---

## Design principles

1. **Declarative first** — you say *what*, Hellow does *how*.
2. **Presentation vs. logic separation** — markup stays clean; behavior returns via events.
3. **Cheap by default** — virtualized rows, culled rendering, pure layout math you can unit-test.
4. **Native feel** — vanilla buttons and familiar scroll/input behavior everywhere.

## Requirements

- Minecraft **1.21.1**
- **NeoForge 21.1.x**
- Java **21+**
- Client side only

## Getting started

Include Hellow as a dependency of your NeoForge mod. Build an element tree, hand it to a dialog or register it as a page, and wire up when it should appear. As a library it ships with no UI of its own — TISAM is the reference implementation showing a full recommendation screen (virtualized list, tabs, cards, install guides, external-link confirmations) built on this library.