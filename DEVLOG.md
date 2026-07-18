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
