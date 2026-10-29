# Verification

The project is developed and built on GitHub. Automated checks run on GitHub Actions; no local Minecraft installation is modified.

## Automated checks

- Goal bounds and missing-resource arithmetic.
- Multi-stack inventory counts, favourite exclusions and context-aware advice.
- Recursive recipes, output batches, inventory intermediates, shared stock, reserved final targets, surplus reuse and ingredient alternatives.
- Explicit recipe routes, conversion cycles, unknown recipes and million-item targets.
- Settings round trips, corruption backups, future-schema preservation, value normalization, previous-good backups and failed writes.
- Minecraft client startup, synced recipe reading, live inventory counts and crafting against carried materials.
- Dashboard/HUD screenshots at normal and compact window sizes.
- Client disconnect cleanup and settings reload.

## Release gate

Compilation, unit tests and client integration tests must succeed for the release commit. Passing compilation alone is not treated as an in-game verification.

The latest workflow run and its artifacts are the authoritative evidence:
https://github.com/aiden-7331/smart-survival/actions

## Manual compatibility checks

Before marking a version as broadly compatible on a distribution platform, test on a real survival server with its rules and plugins, and alongside the player's usual HUD/inventory mods. Automated tests cannot establish compatibility with every modpack.

Suggested manual checks:
1. Rebind K and F8, change GUI scale, resize the window, and reopen every dashboard tab.
2. Test recipes supplied/removed by a server data pack and restricted recipe unlocking.
3. Check favourite drops from hotbar, container slots and a carried cursor stack.
4. Verify F1, death/respawn, reconnect and switching between different servers.
5. Confirm large build goals, partial stacks, offhand/equipment counts and invalid configuration recovery.

No FPS or memory-use benchmark claim is made.
