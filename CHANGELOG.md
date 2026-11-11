# Changelog

All notable changes to Hellow are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **Layout as data.** A child can now declare its layout intent instead of
  needing a wrapper class per capability:
  - `ElementLayout` (weight, fill, min/max width, alignment) with a
    Minecraft-free `clampWidth` so the arithmetic is unit-testable.
  - `StyledElement` and `Elements.styled/weighted/minWidth/maxWidth/widthBounds/aligned`.
- **Row distribution modes.** `Justify` and `JustifyMath` add
  `START`, `CENTER`, `END`, `SPACE_BETWEEN`, `SPACE_AROUND`, `SPACE_EVENLY`
  and `EQUAL`. `Elements.row(Justify, gap, ...)` builds a row that fills its
  container exactly — the pattern tab bars and toolbars need.
- **Per-element decorations**, wrappable around any element including containers:
  - `Backdrop` (flat or vertical gradient), `SolidBorder`, `GradientRing`,
    `Shadow`, `Glow`, `CornerAccents`, `Sparkles`.
  - `DecoratedElement` and `Elements.decorate(...)`.
  - `DecorationPresets` (`outline`, `panel`, `card`, `glow`, `chip`, `focus`,
    `shadow`, `framed`) derived from the active `HellowTheme`, so a custom theme
    restyles decorations for free.
  - `Sparkles` is a generic port of the TISAM card sparks: analytic motion
    (no per-frame integration), comet tails, and state keyed by
    `Decoration.stateKey()` so virtualized list rebuilds do not reset it.
- **Configurable window chrome.** `WindowSpec` (size, anchor, padding, title bar
  height, content max width) and `PageBuilder.window(...)`. `WindowsSpec.DEFAULT`
  reproduces the previous full-screen layout exactly.
- **File-driven pages** now understand:
  - a per-element `style` block (weight, fill, min/max width, align),
  - a per-element `decorations` array (objects with `kind`, or preset names),
  - a page-level `window` block,
  - `justify` on rows.
- **Layout Playground** — an in-game tuner reachable from the debug report that
  previews gap / justify / align / decoration presets live and writes the
  equivalent JSON to the log.

### Changed

- Containers resolve layout through `ElementLayouts.of(child)`; they no longer
  inspect concrete wrapper types.
- Per-child alignment is honored by columns and rows.
- `Elements.wrapRow(ContentAlign, gap, maxWidth, ...)` aligns each wrapped line.

### Fixed

- **Wrap rows overflowed narrow parents.** `WrapRowElement` measured and wrapped
  against its own `maxWidth` even when the parent handed it less space. It now
  uses the smaller of the two; `maxWidth <= 0` means "no cap".
- A column child that legitimately measured zero width was silently re-measured
  on every layout pass.

### Compatibility

- `FlexElement` and `FillElement` are unchanged and keep working; the layout
  resolver maps them onto the equivalent `ElementLayout`.
- `PageSpec` gained an eighth component but keeps its 6- and 7-argument
  constructors, so existing call sites compile unchanged.
- `HellowTheme` is untouched: presets read its existing slots rather than adding
  a 27th component, which would have broken positional construction.

## [1.0.0] — Initial release

- Declarative element tree with column/row/grid/flex/fill/wrap/scroll/list
  layout.
- Window stack with normal, modal and top layers; dialogs and modals.
- Event-driven page triggers (screen open, game event, API poll) with
  once/every-time/decay strategies.
- Form controls backed by `State`, declarative element events.
- Text modes: single line, auto-wrap, ellipsis clamp, collapsible.
- Vanilla-consistent widgets, inertia scrolling, theming and translations.
- Debug report with error capture, log export, page reload.
- File-driven pages under `config/hellow/pages`.
- Embedding of third-party screens as elements.
