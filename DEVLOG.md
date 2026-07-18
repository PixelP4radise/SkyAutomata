# Sky Automata — Dev Log

A running record of decisions, problems hit, and breakthroughs while building this mod —
kept for later reference (e.g. a devlog/video writeup), not just git history.

---

## Day 1 — 2026-07-18

### Decisions
- Documented the target architecture in `CLAUDE.md` before writing any bot logic:
  Java-only mod code, Mojang + Parchment mappings, client/server sidedness split, and
  Baritone quarantined to client-only code. Also recorded the planned **HFSM + Task
  Queue** design for autonomous behavior (Mode Manager as "brain", primitive tasks like
  `ScanTask`/`PathTask`/`EquipTask` as "actuators", async-only execution driven off
  Baritone's callbacks — never blocking the client thread or polling).
- Keeping the `bot` package free of any `net.minecraft.client.gui`/rendering imports and
  exposing `ModeManager` state via plain getters, so it reads as a UI-agnostic Model layer.
  Future UI (HUD/status/mode controls) should sit behind a ViewModel wrapping
  `ModeManager` rather than rendering code calling into it directly (MVVM).

### Problems found
- `build.gradle.kts` only declared `loom.officialMojangMappings()` — raw Mojang mappings
  carry zero parameter names or javadoc, which doesn't match the Parchment convention
  the project is supposed to follow.
- A `baritone-api-fabric-1.15.0.jar` has been sitting at the repo root, unwired — not yet
  declared as a dependency anywhere in the build. Still todo before any Baritone-backed
  `PathTask` work can start.

### Breakthroughs
- Wired Parchment into `build.gradle.kts` via Loom's layered mappings:
  ```kotlin
  mappings(loom.layered {
      officialMojangMappings()
      parchment("org.parchmentmc.data:parchment-${minecraft_version}:${parchment_version}@zip")
  })
  ```
  with `parchment_version=2025.09.14` (current stable release for MC 1.21.8, per
  Parchment's maven metadata) and the `https://maven.parchmentmc.org` repository added.
- Verified end-to-end: `./gradlew help` resolves the layered mapping set cleanly, and
  `./gradlew genSources` decompiles Minecraft with real Parchment javadoc present (e.g.
  `Block.updateOrDestroy`'s doc comment referencing `Level#setBlock`) — official mappings
  alone ship with *zero* comments, so this confirms the Parchment layer is actually active,
  not just configured.
- Built the HFSM + Task Queue engine itself: `Mode`, `ModeManager`, `Task`, `TaskQueue` in
  the new `pt.codered.sky.automata.client.bot` package, wired to
  `ClientTickEvents.END_CLIENT_TICK` via `SkyAutomataClient.MODE_MANAGER`. Deliberately
  scoped to the engine only — no concrete `Mode`s (Lumbering/Hunting/...) or `Task`s
  (ScanTask/PathTask/...) yet, and `PathTask` is left out entirely until Baritone is wired
  into the build. `TaskQueue.tick()` advances at most one `Task` per call and never blocks;
  a `Mode` is expected to check `TaskQueue.isIdle()` before pushing its next action, which
  is what actually enforces "one primitive action per tick" rather than the queue itself.
- Registered the first five concrete `Mode`s — `FarmingMode`, `MiningMode`, `CombatMode`,
  `ForagingMode`, `FishingMode` (superseding the Lumbering/Hunting/Farming examples in
  `CLAUDE.md`) — via a new `ModeRegistry` (id → `Mode` lookup) so a future
  command/keybind/GUI can switch `ModeManager`'s active mode by id. Each mode currently
  extends `AbstractMode`, which only logs on enter/exit and leaves `tick()` a no-op, since
  no concrete `Task`s exist yet to push. Registration happens in
  `SkyAutomataClient.onInitializeClient()`, which stays the composition root.
