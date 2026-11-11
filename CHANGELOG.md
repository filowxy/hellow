# Changelog

All notable changes to Hellow are documented in this file.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versioning follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `ElementLayout` and `StyledElement`: per-child weight, fill, min/max width and alignment.
- `Justify` / `JustifyMath` with center, end, space-between, space-around, space-evenly and equal modes.
- `Elements.styled`, `weighted`, `minWidth`, `maxWidth`, `widthBounds`, `aligned`, `row(Justify, ...)`.
- Element decorations: `Backdrop`, `SolidBorder`, `GradientRing`, `Shadow`, `Glow`, `CornerAccents`, `Sparkles`.
- `DecoratedElement` and `Elements.decorate(...)`.
- `DecorationPresets` (`outline`, `panel`, `card`, `glow`, `chip`, `focus`, `shadow`, `framed`).
- `WindowSpec` and `PageBuilder.window(...)` for page size, anchor, padding, title bar and content width.
- File pages: per-element `style` and `decorations`, page-level `window`, row `justify`.
- `LayoutPlaygroundScreen` in the debug report.

### Changed

- Containers resolve child layout through `ElementLayouts` instead of wrapper types.
- Columns and rows honor per-child alignment.
- `FlexElement` and `FillElement` remain supported; `PageSpec` keeps its 6- and 7-argument constructors.

### Fixed

- Wrap rows measured against their own `maxWidth` and could overflow a narrower parent.
- Column children that legitimately measured zero width were re-measured on every pass.

## [1.0.0]

- Initial release.
