# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Dev Log

`DEVLOG.md` at the repo root tracks decisions, problems found, and breakthroughs as the
mod is built — kept for a later devlog/video writeup. Entries are grouped under `## Day N —
YYYY-MM-DD` headings, one per calendar day of work (check the existing highest Day N before
adding a new heading — don't renumber past days). Append to the current day's entry (or
start a new Day heading on a new work day) whenever a session produces a non-obvious
decision, a real problem/gotcha, or a verified breakthrough — CLAUDE.md documents current
state/conventions, DEVLOG.md documents the story of how it got there.

## Project

Sky Automata is a Minecraft Fabric mod targeting Minecraft 1.21.11, built with Fabric Loom. It is currently at the
scaffold stage — the code is still the unmodified Fabric example-mod template (mod id `sky-automata`, base package
`pt.codered.sky.automata`, maven group `pt.codered.sky.automata`), not yet holding real mod features.

## Commands

```bash
./gradlew build          # compile, run checks, and produce the mod jar (build/libs/)
./gradlew runClient      # launch a dev Minecraft client with the mod loaded
./gradlew runServer      # launch a dev Minecraft server with the mod loaded
./gradlew genSources     # decompile/attach Minecraft sources for IDE navigation
./gradlew clean          # remove build output
```

There is no test suite yet — `./gradlew build` is the only CI-checked step (see `.github/workflows/build.yml`,
which runs on JDK 25 via `microsoft` distribution and uploads `build/libs/` as an artifact).

Mod, loader, loom, and Fabric API versions are pinned in `gradle.properties`, not in `build.gradle.kts` — bump
versions there when updating dependencies.

## Core Ecosystem Conventions

- **Language:** all mod logic is Java — no Kotlin/Scala/other JVM languages for mod code.
- **Mappings:** the project standard is **Mojang Mappings + Parchment** (Parchment supplies parameter names/javadoc
  on top of Mojang's official mappings). Note: `build.gradle.kts` currently only applies
  `loom.officialMojangMappings()` with no Parchment layer configured — add the Parchment dependency/mappings
  block when wiring this up, don't assume it's already in place. Never switch to Yarn or MCP mappings.
- **Java toolchain:** target is Java 21 (matches `options.release = 21` and `sourceCompatibility`/
  `targetCompatibility` in `build.gradle.kts`). Prefer expressing this via a Gradle `java { toolchain { ... } }`
  block over relying on whatever JDK happens to be on the local/CI `PATH` — the build doesn't yet declare an
  explicit toolchain, only source/target compatibility plus `options.release`.

## Baritone Integration

- **Baritone is client-only.** It's a client-side automation/pathing bot; all Baritone API imports, calls,
  wrappers, and bot logic must live in `src/client/java` (or client-only entrypoints) and never in `src/main/java`.
  This follows the same split-source-set sidedness rule as the rest of the client/server separation below —
  leaking a Baritone import into common code will crash a dedicated server with `NoClassDefFoundError`.
- Wired in `build.gradle.kts` as `"modClientImplementation"(files("baritone-api-fabric-1.15.0-8-gbc3dcde2.jar"))`
  — a pre-release build from the Baritone team for 1.21.11 (its manifest's
  `Fabric-Minecraft-Version` matches; verified against `checksums.txt` before use), not yet on a public maven
  repo, hence the local file dependency. `modClientImplementation` (Loom's per-source-set mod-aware
  configuration, named `mod<SourceSet>Implementation` — no Kotlin DSL accessor exists for it, hence the string
  invocation) is what actually scopes it to the `client` source set only and gets it remapped into this
  project's mappings; a plain `implementation`/`files(...)` would leak it onto `src/main`'s classpath too.
- Baritone also needs `nether-pathfinder-1.6.jar` (`dev.babbaj.pathfinder.NetherPathfinder`), wired separately
  as `"clientImplementation"(files("nether-pathfinder-1.6.jar"))` right after the Baritone dependency. Baritone
  embeds it as a Fabric jar-in-jar nested jar (`"jars"` in its own `fabric.mod.json`), but that nested jar has no
  `fabric.mod.json` of its own, and Fabric Loader only unpacks jar-in-jar nested jars for mods discovered from
  the `mods/` folder — a mod pulled in via `modClientImplementation(files(...))` sits directly on the dev
  classpath and never goes through that extraction step, so without this explicit dependency the game builds
  fine but crashes at startup with `NoClassDefFoundError: dev/babbaj/pathfinder/NetherPathfinder` from
  `BaritoneAPI.<clinit>` — confirmed by actually running `./gradlew runClient`, not just `./gradlew build`. Plain
  `clientImplementation`, not `modClientImplementation` — it isn't a Fabric mod and touches no Minecraft classes,
  so it needs no remapping. The jar in the repo root was extracted straight out of the Baritone jar (`unzip -p
  baritone-api-fabric-1.15.0-8-gbc3dcde2.jar META-INF/jars/nether-pathfinder-1.6.jar > nether-pathfinder-1.6.jar`)
  to guarantee it's the exact version that Baritone build was compiled against.

## Bot Logic Architecture (planned, not yet implemented)

No mode manager, task queue, or task classes exist in the codebase yet — this is the target design for when
autonomous bot behavior is built, not a description of current code:

- **HFSM + Task Queue.** Autonomous behavior is driven by a Hierarchical Finite State Machine combined with a
  task queue, not ad-hoc imperative control flow.
- **The Brain (Mode Manager):** high-level modes (Lumbering, Hunting, Farming, ...) are FSM states. A single
  mode manager owns the active mode and drives its tick logic from the client tick event.
- **The Actuators (Task Queue):** a mode must not perform multiple primitive actions (look, swap item, break
  block, ...) within one tick. Modes push primitive tasks (`ScanTask`, `PathTask`, `EquipTask`, `LookTask`,
  `InteractTask`, ...) onto a queue instead, so behavior reads as one human-like input per tick.
- **Async execution only:** never block the client thread and never call `Thread.sleep()` anywhere in this
  logic. A `PathTask` that hands movement off to Baritone must yield and resume from Baritone's async callbacks
  (`IPathingBehavior`/`PathEvent`) rather than blocking or polling — advance the queue only once the callback
  fires.

## Architecture

- **Split environment source sets** (`loom { splitEnvironmentSourceSets() }` in `build.gradle.kts`): common logic
  lives in `src/main/java`, client-only logic (rendering, input, etc.) lives in `src/client/java`. Both source sets
  are registered under the single `sky-automata` mod in the `loom.mods {}` block. Never put client-only code
  (anything touching `net.minecraft.client.*`) in `src/main` — it will crash a dedicated server.
- **Entrypoints**: `SkyAutomata` (`src/main/.../SkyAutomata.java`) implements `ModInitializer` and runs on both
  client and server; `SkyAutomataClient` (`src/client/.../client/SkyAutomataClient.java`) implements
  `ClientModInitializer` and runs client-side only. Both are registered in `src/main/resources/fabric.mod.json`
  under `entrypoints.main` / `entrypoints.client`. `SkyAutomata.MOD_ID` and `SkyAutomata.id(String)` are the
  canonical way to build `Identifier`s for this mod (Mojang renamed `ResourceLocation` to `Identifier` at some
  point between 1.21.8 and 1.21.11 — confirmed by an actual compile failure against the mapped 1.21.11 jar,
  not a version-scheme-wide change).
- **Mixins**: two separate mixin configs — `src/main/resources/sky-automata.mixins.json` (common, package
  `pt.codered.sky.automata.mixin`) and `src/client/resources/sky-automata.client.mixins.json` (client-only,
  package `pt.codered.sky.automata.client.mixin`, applied with `"environment": "client"` in `fabric.mod.json`).
  Any new mixin class must be added to the `mixins` (or `client`) array in the matching config file or it will
  never be applied. Compatibility level is `JAVA_21`.
- **fabric.mod.json** (`src/main/resources/`) is the mod manifest: entrypoints, mixin configs, dependency
  constraints (`fabricloader`, `minecraft`, `java`, `fabric-api`), and metadata. The `version` field is templated
  from `mod_version` in `gradle.properties` via the `processResources` task in `build.gradle.kts` — don't hardcode
  a version string here.
