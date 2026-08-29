# TISAM — There Is Still A Mod

> *A gentle nudge, not a hard shove.* — TISAM helps modpack authors remind players about optional mods **through official download channels only**.

---

## ✨ Mod Introduction (简介)

**TISAM** is a lightweight, compliance-friendly mod recommender for **NeoForge 1.21.1** modpacks. On every launch, it scans the player's `mods` folder, compares the installed jars against a configured list of *optional* mods, and — when something is missing — opens a small, non-intrusive dialog on the title screen pointing the player to the **official** download page.

TISAM links out to mod authors' original release pages on Modrinth, CurseForge, or GitHub. The player can browse the missing list, view per-mod install instructions, or permanently dismiss the prompt with a 5-second confirmation cooldown.

Built for **modpack authors** who want a discoverability layer that respects both **licensing** and **player agency**.

---

## 📖 Mod Details (详情)

### 🎯 What It Does

On every game launch, TISAM:

1. **Scans** `.minecraft/mods/` and matches jar filenames against your `modules.json` id list.
2. **Computes the missing set** — the recommended mods the player hasn't installed yet.
3. **Decides whether to show the prompt** using a three-layer gate:
   - Is every recommended mod already installed? → silent.
   - Has the player confirmed "Never Remind Again"? → silent.
   - Is the reminder decay timer due? → silent.
   - Otherwise → show a small first-prompt card on the title screen.
4. **Renders the prompt UI** with three buttons:
   - **View Mods** — opens a scrollable, sortable list with badges.
   - **Later Reminder** — closes; increments the decay counter.
   - **Never Remind Again** — opens a disclaimer (5s cooldown) before committing.

### 🛠️ Features

- 🔍 **Filename-based detection** — no manifest parsing, no class-loading; lightweight and version-tolerant (`sodium-0.5.0.jar`, `Sodium-0.6.0-mc1.21.1.jar`, etc. all match `id: "sodium"`).
- 📋 **Three-step in-game flow** — concise prompt → scrollable list → collapsible install guide with custom instructions.
- ⏱️ **Decay-based reminder frequency** — `reminder_frequency: [1, 1, 3, 3, 3, 7, 7, 7, 7, 15]` (every launch → every 3 launches → every 7 → every 15) configurable per modpack.
- 🏷️ **Priority tags** — `featured` ★, `critical` ⚠, `normal`, `optional` ↓ with sort order, color borders, and labels.
- 🛡️ **5-second confirmation cooldown** on the "Never Remind Again" disclaimer — prevents mis-clicks.
- 🔌 **Public API** — `com.tisam.api.TisamApi` for other mods to integrate with TISAM.
- ⌨️ **Client command** — `/tisam show | preview | reset` for FancyMenu buttons and debugging.
- 🧪 **Preview mode** — `"preview": true` forces the prompt on every launch for modpack-author previews, with **in-memory-only** click handling (writes nothing to disk while the modpack author is testing).
- 💾 **Local-only data** — everything lives in `config/tisam/preferences.json`; zero network calls.

### 📦 Configuration

`config/tisam/modules.json`:

```json
{
  "modules": [
    {
      "id": "sodium",
      "name": "Sodium",
      "description": "Modern rendering engine, dramatically better FPS.",
      "license": "LGPL-3.0",
      "url": "https://modrinth.com/mod/sodium",
      "can_ignore": true,
      "priority": "critical"
    }
  ],
  "reminder_frequency": [1, 1, 3, 3, 3, 7, 7, 7, 7, 15],
  "install_guide_text": "...",
  "disclaimer_text": "...",
  "preview": false
}
```

`config/tisam/preferences.json` (auto-managed by TISAM):

```json
{
  "permanently_skipped": false,
  "reminder_count": 0,
  "last_reminder_date": ""
}
```

### 🚀 Quick Start

**Players:**
1. Drop `There-Is-Still-A-Mod-1.0.0.jar` into `.minecraft/mods/`.
2. Launch once — TISAM auto-generates the config files under `.minecraft/config/tisam/`.
3. Restart. The prompt will appear if your modpack author configured any recommended mods.

**Modpack Authors:**
1. Include the TISAM jar in your modpack.
2. Ship a pre-filled `config/tisam/modules.json` listing your recommended optional mods.
3. Set `"preview": true` while authoring, then flip it to `false` for release.
4. (Optional) Add the [compliance statement](#-compliance-statement) to your modpack description.

### 🔐 Compliance Statement

TISAM is licensed under **Apache-2.0**. It ships only its own bytecode; all recommended-mod files stay with their original authors. Every external link in the prompt UI points to the mod author's official release page.

Modpack authors shipping TISAM should add this to their modpack description:

> This modpack uses **TISAM (There Is Still A Mod)**, an in-game recommender for optional mods. All recommended mods are downloaded from official channels at the player's discretion. This modpack does not contain any of the recommended mods' files.

### 📋 Requirements

- ☕ **Java 21+**
- 🟢 **NeoForge 21.1.x** for Minecraft **1.21.1**
- 🔧 Git (development only)

### 🏗️ Build

```bash
gradlew.bat build        # Windows
./gradlew build          # Linux / macOS
```

Output: `build/libs/There-Is-Still-A-Mod-1.0.0.jar`

### 📄 License

Apache-2.0 — see [LICENSE.txt](LICENSE.txt).
