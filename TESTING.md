# Verification

The project is developed and built on GitHub. Automated checks run on GitHub Actions; no local Minecraft installation is modified.

## Automated checks

- Goal bounds and missing-resource arithmetic.
- Multi-stack inventory counts, favourite exclusions and context-aware advice.
- Recursive recipes, output batches, inventory intermediates, shared stock, reserved final targets, surplus reuse and ingredient alternatives.
- Explicit recipe routes, conversion cycles, unknown recipes and million-item targets.
- Settings round trips, corruption backups, future-schema preservation, value normalization, previous-good backups and failed writes.
- Minecraft client startup, synced recipe reading, live inventory counts and crafting against carried materials.
- Dashboard item search and goal creation, custom preset creation, actual HUD dragging and screenshots at normal and compact window sizes.
- Manual hotbar, container and cursor drop cancellation for favourites.
- Equipped/offhand inventory updates and favourite finished crafting targets.
- Separate dedicated-server connection and synced recipe planning.
- Client disconnect cleanup and settings reload.

## Verified baseline

[Run 6](https://github.com/aiden-7331/smart-survival/actions/runs/40860440324) passed on 29 October 2026: 26 unit tests (15 planner, 5 inventory, 6 settings), full JAR compilation and the Minecraft client integration scenario, including the separate server connection. Subsequent release changes must pass the same checks. Tests run with Java 25 and software-rendered Minecraft 26.3 on Ubuntu 24.04.

Visual review of run 7 confirmed legible dashboard controls and separate resource quantities at 854×480 and 640×480; it also prompted smaller responsive HUD sizing and extra compact dashboard capacity. Screenshots are generated test evidence and included in Actions artifacts. They are also emitted into the verification log for inspection. Game tests and their test-only server commands are excluded from the distributed JAR.

## Loader compatibility regression

Version 1.0.1 builds and runs client integration tests using Fabric Loader 0.19.5. The production metadata accepts that same minimum; this avoids declaring support solely by editing the dependency check. The earlier 1.0.0 baseline used Loader 0.20.1. Check the successful 1.0.1 workflow before installing its artifact.

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
