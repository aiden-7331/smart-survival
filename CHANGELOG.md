# Changelog

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
