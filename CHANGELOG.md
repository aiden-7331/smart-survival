# Changelog

## 1.0.2 — Quantity editing and resource refresh

- Add explicit Save and Cancel controls to item/quantity editors across goals, crafting plans and preparation checklists.
- Keep the selected item and typed quantity through window resizing.
- Normalize a detached settings snapshot when saving, preserving references used by open editors.
- Recount carried resources on every inventory sample, independently of cached stack comparisons.
- Add real mouse/keyboard tests for editing and saving quantities, cancellation and invalid quantities, plus Acacia Boat pickup, hotbar, removal and creative-mode HUD checks.
- Retain Minecraft 26.3 and Fabric Loader 0.19.5 compatibility.


## 1.0.1 — Loader compatibility

- Lower the declared and build-time Fabric Loader minimum from 0.20.1 to 0.19.5.
- Run compilation, unit tests and Minecraft client integration tests against Loader 0.19.5 in CI.
- Fix the startup dependency rejection for Minecraft 26.3 instances using Loader 0.19.5, including Modrinth App installations.
- Preserve the existing settings format and mod behavior.


## 1.0.0 — 2026-10-29

- Add multiple searchable resource goals and live carried-resource counts.
- Add reusable mining, exploring, building and fighting checklists with custom presets.
- Add amber inventory advice, near-full reminders and favourite item types.
- Block manual hotbar, slot and cursor drops of favourite item types when protection is enabled.
- Add recursive planning from synced crafting and furnace recipe displays.
- Respect batch output quantities, ingredient alternatives, carried intermediates and shared stock.
- Add recipe route selection, missing-material pinning and crafting-step lists.
- Add a six-tab survival dashboard and five independently configurable HUD widgets.
- Add draggable positions, scale, background opacity and configurable keybindings.
- Add safe settings persistence, corruption backups and future-schema protection.
- Keep large goal counts readable independently of item names.
- Include 26 calculation/persistence tests and Minecraft integration checks for singleplayer, separate server connections, dashboard editing and favourite protection.

Compatibility: Minecraft Java 26.3, Java 25, Fabric Loader 0.20.1+, Fabric API 0.165.0+26.3+ for Minecraft 26.3.

Known boundaries: unlocked synced recipes only; no hidden inventory access; no dynamic/component-sensitive transformations, smithing, brewing or stonecutter-only planning; cooking fuel quantities are not calculated. See README for details.
