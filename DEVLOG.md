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
- Moved the concrete mode classes into a new `client.bot.modes` subpackage, separate from
  the core engine (`Mode`/`ModeManager`/`Task`/`TaskQueue`/`ModeRegistry`, which stay in
  `bot`), and added `IdleMode` as the default active mode — `ModeManager` now starts on
  `idle` instead of `null`, so the FSM always has a defined active mode.

## Day 2 — 2026-07-19

### Breakthroughs
- Added mode-change chat feedback: `ModeManager` gained a `ModeChangeListener`
  (`onModeChange(previous, next)`, fired from `setMode()`) and `Mode` gained a
  `getName()` default (backed by `AbstractMode`'s stored name). `SkyAutomataClient`
  registers a listener that posts `LocalPlayer.displayClientMessage(...)` when the local
  player exists, keeping the `Minecraft`/`Component` chat APIs out of the `bot`/`bot.modes`
  packages entirely — the Model layer only exposes the change event, the View-ish reaction
  lives in the composition root, in keeping with the MVVM separation already decided on.
  Verified against the decompiled source that `displayClientMessage` routes straight into
  `Minecraft.getChatListener().handleSystemMessage(...)` with no `connection.send(...)` —
  it's a local-only HUD insert, never sent over the network, so it's never visible to the
  server or other players.
- Registered `/automata <mode>` as a client-side Brigadier command
  (`net.fabricmc.fabric.api.client.command.v2`, already available transitively via the
  `fabric-api` umbrella dependency — no `build.gradle.kts` change needed) in a new
  `ModeCommands` class. It builds one subcommand per id currently in `ModeRegistry`
  (`idle`, `farming`, `mining`, `combat`, `foraging`, `fishing`) by iterating
  `ModeRegistry.ids()`, so a future mode only needs registering in `ModeRegistry` — the
  command tree picks it up automatically, no separate command wiring per mode. Each
  subcommand just calls `MODE_MANAGER.setMode(...)`; the existing mode-change chat
  listener already provides the player-facing confirmation, so the command itself needs
  no separate feedback. Chose the `/automata <mode>` namespaced shape over top-level
  per-mode commands (`/farming`, `/combat`, ...) specifically to avoid claiming common
  top-level words that could collide with other mods.
- Built the mode settings screen (`/automata ui`) — the first real UI, and the first time
  the MVVM split actually gets used instead of just reserved. Added a minimal `Mode`
  extension point (`getSettings()` returning `List<ModeSetting<?>>`, default empty — no
  concrete `ModeSetting` implementations yet since no mode has a real setting to expose)
  and a `ModeRegistry.idOf(Mode)` reverse lookup. New `client.bot.viewmodel.ModeUiViewModel`
  (still zero Minecraft-client imports) tracks a *selected* mode id separately from
  `ModeManager`'s *active* one, so browsing the list in `ModeScreen`
  (`client.gui.ModeScreen`, a fresh package sibling to `client.bot`) never activates
  anything — only an explicit "Activate" button (or the existing `/automata <mode>`
  command) calls `ModeManager.setMode(...)`. Cross-checked `Screen`/`Button`'s actual
  Mojang-mapped API (constructor, `addRenderableWidget`, `rebuildWidgets`, `Button.builder`
  fluent shape) against this project's decompiled sources before writing the screen, same
  as done for the chat API earlier. Not runtime-verified in a real client — no display in
  this environment — compiled clean via `./gradlew build` only; worth a manual
  `runClient` pass to confirm the layout actually looks right.

### Problems found
- `/automata ui` compiled fine but the screen never actually appeared in-game — confirmed
  the runtime gap flagged above was real. Root cause, found in `ChatScreen`'s decompiled
  source: pressing Enter runs `handleChatInput(...)` (which executes our command
  synchronously, calling `setScreen(new ModeScreen(...))`) and then, on the very next line,
  unconditionally calls `this.minecraft.setScreen(null)` to close the chat box — which
  closes whatever screen is current, including the one the command just opened.
- First attempted fix — deferring via `Minecraft.execute(() -> ...)` — turned out to be a
  no-op and didn't actually fix it (confirmed with diagnostic logging: `removed()` fired
  immediately, `render()` never ran once). Root cause, found in
  `BlockableEventLoop.execute()`'s decompiled source: it only queues the task when called
  from a *different* thread (`scheduleExecutables() = !isSameThread()`); called from the
  render thread — which client commands already run on — it just calls `runnable.run()`
  immediately, synchronously. So it changed nothing about the ordering versus calling
  `setScreen` directly.
- Real fix: added `SkyAutomataClient.runNextTick(Runnable)`, a small queue drained inside
  the existing `ClientTickEvents.END_CLIENT_TICK` registration (alongside
  `MODE_MANAGER.tick()`). Since that event only fires once input handling for the frame is
  fully done, `ModeCommands`'s `ui` subcommand now queues the `setScreen(new
  ModeScreen(...))` call there instead of running it inline — genuinely runs after
  `ChatScreen` has closed itself, not just wrapped in a lambda that still runs at the same
  point. General gotcha, not specific to this screen: any future command that opens a
  `Screen` needs `SkyAutomataClient.runNextTick(...)`, not `Minecraft.execute(...)`.
