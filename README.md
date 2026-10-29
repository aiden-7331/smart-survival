# Smart Survival

**Carry a plan, not another spreadsheet.**

Smart Survival is a lightweight, client-side Fabric companion for Minecraft Java Edition **26.3**. Pin materials for a build, prepare for a trip, see what your carried supplies can craft, and keep a few useful reminders on your HUD.

[Build checks](https://github.com/aiden-7331/smart-survival/actions) · [Download builds](https://github.com/aiden-7331/smart-survival/actions/workflows/build.yml) · [Report a problem](https://github.com/aiden-7331/smart-survival/issues)

## What it does

- **Resource goals:** search items by name or identifier, choose a target up to 1,000,000, and see carried / target / missing counts. Edit and remove multiple goals.
- **Adventure preparation:** Mining, Exploring, Building and Fighting checklists. Add or remove supplies; create, duplicate, rename and delete your own presets.
- **Inventory assistant:** a configurable near-full threshold, amber duplicate-tool and partial-stack suggestions, and favourite item types.
- **Crafting planner:** recursively expands supported recipes from your synced recipe book. Handles ingredient alternatives, recipe alternatives, output batch sizes, existing intermediates and surplus outputs. All targets share one material budget.
- **Survival dashboard:** six compact tabs with inventory, health, food and experience information. Lists are paginated and scrollable.
- **HUD editor:** independently show, hide, drag, scale and adjust the background opacity of five widgets. Positions are stored as proportions and clamped to the visible GUI.

The mod never crafts, sorts, deletes or moves items for you. It does not query containers, scan the world, send custom server packets or access hidden server state.

## Install

1. Install Java 25 and Minecraft Java Edition **26.3**.
2. Install [Fabric Loader](https://fabricmc.net/use/installer/) **0.19.5 or newer** for that version.
3. Put [Fabric API](https://modrinth.com/mod/fabric-api) **0.165.0+26.3 or newer for 26.3** and the Smart Survival release JAR into your instance's `mods` folder.
4. Start the Fabric profile and join a world. Press **K**.

Download from the latest **successful** run on the [Build and test page](https://github.com/aiden-7331/smart-survival/actions/workflows/build.yml): open the run, select **smart-survival-build** under Artifacts, unzip it and use `build/libs/smart-survival-1.0.2.jar`. GitHub requires sign-in for artifact downloads. These are installable builds; a separate distribution-platform listing is not required.

Use the normal `smart-survival-<version>.jar`; the `-sources.jar` is for developers. Install on the client only. Friends and multiplayer servers do not need Smart Survival.

Only 26.3 is targeted. Minecraft 1.21.x, 26.1 and 26.2 need separate ports; do not assume binary compatibility. No Mod Menu dependency is required.

### Upgrading from an earlier version

Version 1.0.0 declared a Loader 0.20.1 minimum. Version 1.0.1 targets Loader 0.19.5 and runs the same full client test suite against it. Replace the old Smart Survival JAR; keep only one version in your instance's `mods` folder. Your saved goals and settings remain compatible.

In the Modrinth App, select your instance and use its folder button to open the instance directory, then replace the JAR in `mods`. Keep Minecraft 26.3 and the matching Fabric API installed.

## Quick start

### Build something

Open **K → Goals → Add goal**, search for a block, select it, enter its quantity, then click **Save**. Each goal row opens editing: change the quantity and click **Save**. **Cancel** leaves it unchanged; Remove unpins it. Goals count the player's carried inventory and equipment, including offhand. They do not count items inside bundles, shulker boxes or external storage.

### Prepare for a trip

Open **Pack** and click the preset button to cycle checklists. Use **+ Item**, choose a supply and quantity, then **Save** to customize supplies and **Manage** for preset operations. Matching uses exact item identifiers: a diamond pickaxe does not silently satisfy an iron-pickaxe checklist. Change the entry to your preferred equipment.

### Plan crafting

Open **Plan → Add target**. The picker shows outputs of supported, unlocked recipes. Select an output, enter the target quantity and click **Save**. Click **Targets / Materials / Steps** to switch views. On Targets, a recipe row cycles between automatic selection and explicit routes. Materials shows additional supplies to collect after allocating inventory; **Pin** turns a missing material into a resource goal. Steps lists the chosen crafts and stations.

Recipes only become available when Minecraft sends them to your recipe book. Unlock recipes normally. Server data packs are reflected when their supported recipe displays are synced. Missing or removed recipe outputs remain explicit materials rather than being guessed.

The planner:
- Supports shaped crafting, shapeless crafting and furnace-family displays supplied in the recipe book.
- Uses actual display output counts and interchangeable ingredients.
- Reserves finished targets before expanding other targets; consumes intermediates before raw materials.
- Excludes favourites and stacks Minecraft marks unsuitable for crafting from ingredient stock.
- Counts crafting materials in the 36 main inventory slots; equipment and offhand are not crafting supply.
- Chooses routes deterministically using available stock and local material cost. It is **not a globally optimal solver**.
- Stops cycles and excessive searches. A warning explains when remaining items must be acquired directly.
- Shows cooking fuel as a separate reminder. **Fuel amount, burn time, station construction and container refilling are not included.**
- Does not simulate component-sensitive/dynamic recipes, smithing transformations, brewing or stonecutter-only recipes. Unsupported displays are counted in the dashboard. Returned containers are not credited as reusable ingredients.

### Protect favourites

In **Bag**, click the star beside a carried item, or use **Favourite item** to select any item type. The favourites view includes saved types even when you are not carrying them. Favourite types are excluded from surplus advice and crafting ingredient allocation.

Drop protection blocks manual hotbar and container drops while enabled. Unfavourite an item or disable the setting to deliberately discard it. This is not a guarantee against death, destruction, server plugins, creative-mode inventory deletion, crafting consumption, or actions performed by other mods.

### Arrange the HUD

Open **Home / Setup → Edit HUD**. Click a widget, drag it, and use **Scale ±**, **Fade ±** and **Show/hide**. Changes save when a drag ends or the editor closes. Hidden widgets remain visible in the editor. Empty widgets are hidden during play. Each widget shows up to four entries and a remaining-entry count.

The HUD follows Minecraft's hide-interface behavior and is hidden behind menus. Defaults show resource goals and inventory advice; enable the other widgets as needed.

## Controls

| Action | Default |
| --- | --- |
| Dashboard | K |
| Toggle HUD | F8 |
| Pin held item with target 64 | Unbound |
| Favourite / unfavourite held item | Unbound |

Change these under **Options → Controls → Key Binds → Smart Survival**. Nothing depends on fixed key codes after rebinding.

## Settings and privacy

Preferences, goals, recipe choices, favourite item types and presets are stored in `config/smart-survival.json`. They are shared across worlds on the same installation; there is no per-server history or tracking.

Writes use a temporary file and atomic replacement where supported. The preceding file is retained as `.bak`. Malformed files are copied to a timestamped `.corrupt-*` file before defaults can be saved. Settings from a future schema are left untouched and saving is disabled. Save failures appear in the dashboard.

Saving normalizes a separate snapshot, so an open editor keeps its live goal/preset references.

No telemetry, network service, external account or server mod is used.

## Performance

Carried resource counts are recomputed independently of the stack-change cache at most twice per second. Inventory snapshots run at most twice per second during ordinary play and immediately after local configuration edits. Recipe indexing is cached until the client's recipe collections change. Plans recalculate only when inventory, recipes or preferences change. Rendering uses cached rows; it does not traverse recipes every frame.

There are limits of 128 goals per list, 32 presets, 1,024 favourite item types, 24 recursive recipe levels and 12,000 planning operations per recalculation.

## Build and test

JDK 25 is required. Use the committed Gradle wrapper:

```sh
./gradlew build
./gradlew runClient
./gradlew runClientGameTest
```

On Windows use `gradlew.bat`. On a Linux CI machine:

```sh
LIBGL_ALWAYS_SOFTWARE=true SDL_VIDEODRIVER=x11 xvfb-run -a ./gradlew runClientGameTest
```

The GitHub Actions workflow builds the JAR, runs unit tests and starts a Minecraft client with an isolated test world. Artifacts contain build outputs, reports and available game-test screenshots. See [TESTING.md](TESTING.md) for the verification record and remaining manual checks.

Dependency pins: Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.165.0+26.3, Loom 1.19.3, Gradle 9.8.2, Java 25. Minecraft 26.3 ships unobfuscated names, so there is no Yarn or remapping dependency. Versions were checked against [Fabric's official 26.3 example](https://github.com/FabricMC/fabric-example-mod/tree/26.3) and published metadata on 28 October 2026; CI resolves and compiles the exact pins.

## Architecture

- `core/`: Minecraft-independent goal arithmetic, recipe planning, advice and settings.
- `client/`: inventory snapshots and synced-recipe adapter.
- `client/ui/`: dashboard, item picker, preset editor and HUD.
- `client/mixin/`: narrow favourite-drop guards.
- `test/`: calculation and persistence unit tests.
- `gametest/`: integration tests, excluded from the distributed mod.

Original implementation and interface; no third-party mod code, textures or branding. Gradle's standard wrapper retains its upstream licence notices.

## Contributing and support

Please include Minecraft, Fabric Loader, Fabric API and Smart Survival versions, reproduction steps, and a relevant log excerpt. For incorrect crafting results, include the target, carried materials and any data pack providing the recipe. Do not post account tokens, server addresses or private chat logs.

MIT licensed. Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.
