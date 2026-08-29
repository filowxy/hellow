# TISAM — There Is Still A Mod

A compliance-friendly **optional mod recommender** for NeoForge 1.21.1 modpack authors.
TISAM scans the `mods` folder on startup, compares it against a configured list of "optional mods",
and reminds the player via a simple in-game UI to download the missing ones through **official channels only**.

> TISAM **never** bundles any third-party mod files and **never** auto-downloads anything.
> All links point to mod authors' official release pages (Modrinth, CurseForge, etc.).

---

## Features

- Startup scan of `mods/` directory; matches by **jar filename** against `config/tisam/modules.json`
- Three-step in-game flow: concise first prompt → detailed mod list → collapsible install guide
- Reminder frequency decay: every launch → every 3 launches → every 7 launches → every 15 launches
- Permanent-skip with **5-second confirmation cooldown** and a configurable disclaimer
- **Priority tags** for recommended mods (`featured` / `critical` / `normal` / `optional`) with sort + visual badges
- **Custom title-screen integration** via public Java API (`com.tisam.api.TisamApi`)
- **`/tisam ...` client command** for FancyMenu buttons (`/tisam show`, `/tisam preview`, `/tisam reset`)
- Preview mode (`"preview": true`) forces the first prompt on every launch for modpack-author previews
- All user data stored locally in `config/tisam/preferences.json`; no network calls

---

## Quick Start (Players)

1. Drop `tisam-1.0.0.jar` into your `.minecraft/mods` folder.
2. Launch the game once — TISAM will auto-generate `config/tisam/modules.json` and `config/tisam/preferences.json` under your `.minecraft/config/tisam/` directory.
3. Edit `modules.json` to list the optional mods you want to recommend (see [Configuration](#configuration) below).
4. Restart the game; the first prompt will appear on the title screen.

---

## Quick Start (Modpack Authors)

Include the TISAM jar in your modpack, then ship a pre-filled `config/tisam/modules.json` with your
recommended optional mods. See `项目设计.md` for the full specification of the configuration schema.

To preview the prompt at any time, set `"preview": true` in `modules.json`.

---

## Configuration

### `config/tisam/modules.json`

| Field | Type | Description |
|-------|------|-------------|
| `modules[]` | array | Recommended mods |
| `modules[].id` | string | **Filename (without `.jar`)** to detect installation |
| `modules[].name` | string | Display name |
| `modules[].description` | string | One-line description |
| `modules[].license` | string | License tag (e.g. `ARR`, `MIT`, `LGPL`) |
| `modules[].url` | string | Official download page URL |
| `modules[].can_ignore` | bool | Whether the player can permanently skip this mod |
| `modules[].priority` | string | One of `featured` / `critical` / `normal` / `optional` (see [Priority Tags](#priority-tags)) |
| `reminder_frequency[]` | int array | Decay intervals between reminders |
| `install_guide_text` | string | Custom install instructions shown in the guide panel |
| `disclaimer_text` | string | Text shown in the confirmation disclaimer (see [Permanent Skip](#permanent-skip)) |
| `preview` | bool | If `true`, force the first prompt on every launch |

### `config/tisam/preferences.json`

| Field | Type | Description |
|-------|------|-------------|
| `permanently_skipped` | bool | Player has confirmed the permanent-skip disclaimer |
| `reminder_count` | int | Total reminders shown so far; drives the decay strategy |
| `last_reminder_date` | string | ISO date of the most recent reminder (informational) |

---

## Permanent Skip

When the player clicks "Permanently Skip" on the first prompt, TISAM shows a **disclaimer dialog** before
committing. The confirmation button is **disabled for 5 seconds** to prevent mis-clicks. The disclaimer
text is read from `modules.json`'s `disclaimer_text` field. To re-enable reminders later, delete
`preferences.json` or set `permanently_skipped` to `false`.

---

## Priority Tags

Each entry in `modules[]` can be tagged with a `priority` value to control its position in the
list and its visual treatment:

| Value | Sort Order | Visual |
|-------|-----------|--------|
| `featured` | 1st | ★ gold border, gold title, "Editor's Pick" label |
| `critical` | 2nd | ⚠ red border, warm-red title, "Strongly Recommended" label |
| `normal` (default) | 3rd | plain card, default look |
| `optional` | last | ↓ dim border, dim title, "Optional" label |

If any of the missing mods is tagged `critical` or `featured`, the first-prompt body changes from
"optional mods" to "recommended mods" — a small UX nudge from the modpack author.

Example entry:

```json
{
  "id": "sodium",
  "name": "Sodium",
  "description": "Modern rendering engine, dramatically better FPS.",
  "license": "LGPL-3.0",
  "url": "https://modrinth.com/mod/sodium",
  "can_ignore": true,
  "priority": "critical"
}
```

The list is sorted FEATURED → CRITICAL → NORMAL → OPTIONAL. Within the same priority, JSON
order is preserved (stable sort).

---

## Compliance Statement

TISAM is licensed under **Apache-2.0**. It does not bundle any third-party mod files and does not
initiate any downloads. Every external link points to the mod author's official release page.

Modpack authors shipping TISAM should add the following statement to their modpack description:

> This modpack uses TISAM (There Is Still A Mod), an in-game recommender for optional mods.
> All recommended mods are downloaded from official channels at the player's discretion.
> This modpack does not contain any of the recommended mods' files.

---

## Build

```bash
# Build the mod jar
gradlew.bat build        # Windows
./gradlew build          # Linux / macOS

# Launch a dev client (auto-generates run/client/)
gradlew.bat runClient    # Windows
./gradlew runClient      # Linux / macOS
```

The compiled jar will be at `build/libs/There-Is-Still-A-Mod-1.0.0.jar`.

---

## Requirements

- Java 21+
- NeoForge 21.1.x for Minecraft 1.21.1
- Git (only for development)

---

## Project Layout

```
.
├── src/main/
│   ├── java/com/tisam/        # Mod code
│   ├── resources/             # Runtime resources (mixins.json, pack.mcmeta, etc.)
│   └── templates/META-INF/    # neoforge.mods.toml template (filled by Gradle)
├── build.gradle.kts           # Build configuration
├── settings.gradle.kts
├── gradle.properties
└── LICENSE.txt                # Apache-2.0
```

---

## License

Apache-2.0 — see [LICENSE.txt](LICENSE.txt).
