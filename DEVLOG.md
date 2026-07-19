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

### Decisions
- Wired up DevAuth (`me.djtheredstoner:DevAuth-fabric`, pinned via `devauth_version` in
  `gradle.properties`) so `runClient` can log into a real Microsoft account instead of an
  offline/fake one — needed for testing against online-mode servers. Added its Azure
  DevOps maven feed to `repositories`, the dependency as `modRuntimeOnly` (dev-only —
  never bundled into the built mod jar), and `-Ddevauth.enabled=true` on the `client` run
  config in `build.gradle.kts`. Verified against the actual DevAuth GitHub README rather
  than assumed coordinates. Tokens land in `~/.config/devauth/microsoft_accounts.json`
  (outside the repo entirely, not `run/`) — unencrypted per DevAuth's own docs, so that
  file should never be shared.
- Setting the run-config VM arg triggers a Loom deprecation warning
  (`vmArgs(vararg)`/`property(...)` are both flagged) on the pinned
  `loom_version=1.17-SNAPSHOT` — confirmed by decompiling the actual pinned Loom jar
  (`fabric-loom-1.17.16.jar`) that `LoomGradleExtensionAPI` only exposes the old
  `NamedDomainObjectContainer<RunConfigSettings>` via `runs { }`, while a newer
  `RunConfiguration` interface (with `getSystemProperties(): MapProperty<...>`) already
  exists in the jar but isn't wired to any public entry point yet. No non-deprecated path
  exists today, so left the warning in place with a comment rather than chasing an
  unstable/internal API.

### Problems found
- First `runClient` after wiring DevAuth failed fast with `RuntimeException: No account
  specified, specify one with the defaultAccount config option or the devauth.account
  property` — DevAuth *was* enabled (loaded, listed in the mod list, HTTP client
  initialized) so the `-Ddevauth.enabled=true` wiring was correct; the problem was one
  level deeper. DevAuth only creates its `config.toml` the first time it's enabled, and
  the auto-generated file ships `defaultAccount = "main"` **commented out** even though
  `[accounts.main]` is present and uncommented — so it has an account defined but doesn't
  know to use it, by design (matches DevAuth's own "unobtrusive by default" philosophy).
  Fixed by uncommenting `defaultAccount = "main"` in `~/.config/devauth/config.toml` — a
  per-machine file outside the repo, not something to fix in `build.gradle.kts`. Verified
  by actually running `./gradlew runClient` in this environment (no display, so it fails
  fast after startup — enough to see the log): re-ran after the fix and DevAuth got all
  the way to printing a real `login.live.com` OAuth URL, confirming the wiring end-to-end.
  Anyone re-running DevAuth setup fresh on a new machine will hit this same first-run step.

### Decisions
- Why any of this version churn: started the mod on 1.21.8 because that was the latest
  official Baritone release at the time. Turns out Hypixel only supports 1.21.11+, so I
  have to update regardless — figured if I'm updating anyway, I might as well go straight
  for the latest version. There were unofficial Baritone ports floating around for newer
  versions, so 26.2 didn't seem like a dead end on that front.
- Attempted a jump straight to Minecraft 26.2 using `sky-automata-template-26.2` (a
  pre-generated scaffold for that version) as a reference, but backed it out: 26.2 turns
  out to use a new year.release version scheme with real breaking changes (Loom plugin
  id dropped its `-remap` suffix and with it `modImplementation`/`modRuntimeOnly` entirely;
  `GuiGraphics` → `GuiGraphicsExtractor` with `Screen.render` → `extractRenderState`;
  `Minecraft.setScreen` → `setScreenAndShow`; `LocalPlayer.displayClientMessage` split into
  `sendSystemMessage`/`sendOverlayMessage`; Fabric API's client command module merged into
  `fabric-command-api-v2`, `ClientCommandManager` → `ClientCommands`) — all confirmed
  against the real mapped 26.2 jars, not guessed. But ParchmentMC has no `parchment-26.2`
  artifact yet (confirmed live against `maven.parchmentmc.org`), and keeping Parchment is
  a hard project convention, so reverted every migration edit (`git restore`, all changes
  were still uncommitted) rather than ship without it.
- Bumped to Minecraft 1.21.11 instead — the latest version Parchment actually supports —
  with real Fabric API/loader/Parchment versions (`fabric_api_version=0.141.5+1.21.11`,
  `parchment_version=2025.12.20`, checked live against `maven.fabricmc.net` and
  `maven.parchmentmc.org`; `loader_version=0.19.3` was already the latest stable,
  unchanged), keeping every other build convention as-is (`fabric-loom-remap` plugin,
  `modImplementation`/`modRuntimeOnly`, layered `officialMojangMappings() +
  parchment(...)`, Java 21) since none of that changed within the old version scheme.
  Bonus: satisfies the Hypixel 1.21.11+ requirement anyway, and lets the eventual Baritone
  wiring use an official (if not yet formally released) build from the Baritone team for
  1.21.11, rather than one of the unofficial ports — worth swapping the root-level
  `baritone-api-fabric-1.15.0.jar` for that build before starting the `wire baritone` todo.

### Problems found
- Even within the old version scheme, `net.minecraft.resources.ResourceLocation` is gone
  in 1.21.11 — renamed to `Identifier` (same rename spotted during the aborted 26.2
  attempt, but evidently landed earlier than the version-scheme jump, not caused by it).
  Only usage was `SkyAutomata.id(String)`; fixed and verified via `./gradlew build`, which
  now passes clean end-to-end (`compileJava`, `compileClientJava`, `remapJar`) and
  produces `build/libs/sky-automata-1.0.0.jar`. No other API breaks found between 1.21.8
  and 1.21.11 — everything else in the bot/GUI/command code compiled unchanged.
- The `git restore` used to back out the aborted 26.2 attempt reverted the touched files
  all the way to `HEAD`, not just to their state before that attempt — and the DevAuth
  wiring (`build.gradle.kts`'s `DevAuth` repo/`runs`/`modRuntimeOnly` block,
  `gradle.properties`'s `devauth_version`, and this file's DevAuth paragraphs above) had
  never actually been committed, so it got silently wiped along with the 26.2 edits.
  Confirmed via `git log -- build.gradle.kts` (only commit touching it is the very first
  `Project Skeleton`) and `git fsck --dangling` (nothing recoverable — the content was
  never `git add`ed, so git never had a blob for it). Reconstructed all three from what
  had been read into context earlier in the session, verified against `./gradlew build`
  succeeding again. **Takeaway: commit or at least `git add` real working-tree progress
  before letting anything run `git restore`/`git checkout -- <file>` on the same paths —
  "uncommitted" and "safe to revert to HEAD" are not the same thing.**

### Decisions
- Wired Baritone in for real, replacing the stale unwired `baritone-api-fabric-1.15.0.jar`
  at the repo root with `baritone-api-fabric-1.15.0-8-gbc3dcde2.jar` — a pre-release build
  the Baritone team already produced for 1.21.11, found among a batch of build variants
  (api/standalone/unoptimized × vanilla/fabric/forge/neoforge) with a `checksums.txt`.
  Verified before trusting it: `sha1sum` against `checksums.txt` matched, and its
  manifest's `Fabric-Minecraft-Version: 1.21.11` confirmed it's actually built for this
  project's Minecraft version, not just conveniently named. `baritone-api-fabric` (not
  `-standalone` or `-unoptimized`) is the right pick — `-api` is the library artifact meant
  to be depended on; `-standalone` is a complete drop-in mod jar, and `-unoptimized` is a
  debug build, neither of which is what a dependency in `build.gradle.kts` wants.

### Problems found
- Guessed the wrong Loom configuration name twice while wiring it in. First tried
  `clientModImplementation` (`<sourceSet>Mod<Configuration>`) — doesn't exist. Actual name
  follows `mod<SourceSet><Configuration>` (`modClientImplementation`), confirmed by
  temporarily adding a throwaway Gradle task that printed `configurations.names` filtered
  to anything containing "lient" or "od", rather than guessing again. Second: even with
  the right name, `modClientImplementation(files(...))` failed Kotlin script compilation
  with "Unresolved reference" — Loom generates the configuration at runtime but doesn't
  generate a typed Kotlin DSL accessor function for it, so it has to be invoked as a
  string: `"modClientImplementation"(files(...))`. Verified the wiring actually took by
  resolving `configurations["modClientImplementation"].files` and
  `sourceSets["client"].compileClasspath.files` in another throwaway task before deleting
  both probes — confirmed the jar resolves and that Loom remaps it into this project's
  mappings (`baritone-api-fabric-1.15.0-8-gbc3dcde2-<hash>.jar` on the client compile
  classpath) rather than trusting a green `./gradlew build` alone, since that would also
  pass if the dependency silently resolved to nothing.
- `./gradlew runClient` built fine but crashed the game at startup with
  `NoClassDefFoundError: dev/babbaj/pathfinder/NetherPathfinder` from
  `BaritoneAPI.<clinit>` — the build succeeding said nothing about the game actually
  running, so this only showed up by launching the client, not from `./gradlew build`.
  Root cause: Baritone embeds `nether-pathfinder-1.6.jar` as a Fabric jar-in-jar nested
  dependency (`"jars"` in its `fabric.mod.json`), but that nested jar has no
  `fabric.mod.json` of its own and Fabric Loader only unpacks jar-in-jar nested jars for
  mods discovered from the `mods/` folder — a mod pulled in via
  `modClientImplementation(files(...))` sits directly on the dev classpath and never goes
  through that extraction step, so the nested jar silently never lands on the classpath.
  Fixed by extracting it straight out of the Baritone jar (`unzip -p
  baritone-api-fabric-1.15.0-8-gbc3dcde2.jar META-INF/jars/nether-pathfinder-1.6.jar >
  nether-pathfinder-1.6.jar`, committed to the repo root alongside the Baritone jar) and
  adding it as its own dependency — plain `"clientImplementation"(files(...))`, not
  `modClientImplementation`, since it isn't a Fabric mod and touches no Minecraft classes
  so needs no remapping. Verified by rerunning `runClient`: log now shows
  `dev_babbaj_nether-pathfinder 1.6` in the loaded-mods list and `[nether-pathfinder]
  Loaded shared library` before the game reaches the main menu cleanly.
