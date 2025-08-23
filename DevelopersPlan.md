# Developers Plan

## Active TODOs (numbered for reference; order unchanged)

1. [High][In Progress] Revise retry to pre-wait cheaply (probe/visibility) before heavy NCC/hint; add retry-attempt logs
   - Location: `TargetAction.tryNCCFallback()`
   - Notes: Pre-wait loop in place; tune via `slides.hint.prewait.ms` and `slides.hint.prewait.interval.ms`.

2. [High][Pending] Inspect `target/debug/hints/*` images and logs
   - Verify chosen hint, region size, and selection logs (slide-based vs image-driven vs fallback).
   [Feedback] I think that region can be a bit smaller, may be it will make run a little bit faster.

3. [Pending] Validate environment matches recorded patterns
   - If mismatched, re-record patterns to avoid spurious NCC matches.

4. [Pending] Add more logging in `computeAutoHintRegion()`
   - Dump chosen slide element bounds, background picture bounds, and mapping details.

5. [Pending] Optional: overlay rectangle on saved hint snapshots
   - Draw a yellow outline on hint captures saved by `TargetAction` under `target/debug/hints/`.

6. [Pending] Clean lints / unused imports
   - Examples: remove unused `Finder` in `TargetAction.java`, unused `URL` in `SlideParser.java`.

7. [Pending] Expose a hint-source preference beyond `disable_slide_hint`
   - e.g. `-Dslides.hint.source=auto|slide|image` to control selection policy.

8. [Revisit] Change hint highlight/visualization to yellow
   - `apps/.../VerifyPatternInRegion.java` outline is now yellow.
   [Feedback] I still see red outline on the image, it is not yet yellow.

9. [High][In Progress] Implement proper Log4j logging (console + rolling file) and forbid System.out/err in code
   - Location: `apps/.../ExecuteMain.configureLogging()`, `apps/.../ExecuteMain.main()`, build guard in `build-all.sh`.
   - Scope:
     - Always attach ConsoleAppender and RollingFileAppender (path set via `slides.logfile`).
   DONE  - Replace `System.out/err` prints in app with `LOG.*` calls; keep a small tee only to capture third‑party stdout/stderr.
   DONE  - Build guard fails if new `System.out/err.print*` appear in repo Java sources.
   - Expected result when complete:
     - The timestamped run log (e.g., `sikuli-slides-<pptx name>-YYYYMMDD_HHMMSS.log`) contains ALL Log4j INFO/DEBUG lines seen on the console, not just SikuliX `[log]` entries.
     - Console and file logs have consistent formatting and levels; level controlled by `-log DEBUG|INFO|...`.
   SKIP  - Rolling policy keeps up to 3 backups, ~5MB each; long sessions don’t lose earlier logs.
   CANNOT UNDERSTAND  - No raw `System.out/err` in code except the internal `TeeOutputStream` implementation.
   DONE  - Easier post‑mortem: single log file is sufficient to diagnose slide mapping, hint selection, and NCC phases.
   LOW   - Optional: initialize final log filename after `parseArgs()` (create correct name from start) instead of renaming.

10. [Done] When run finishes, print a summary of the run
   - Emits one-line summary at INFO in `apps/.../ExecuteMain.execute()` finally block:
     - Success: `SUMMARY: Test passed`.
     - Failure: `SUMMARY: Test failed on slide <n>: <action>` when available; otherwise: `SUMMARY: Test failed`.
   

## Notes

- Toggle semantics: `disable_slide_hint=true` now means "prefer image-driven hint"; hints are never disabled. We compute both slide-based and image-driven hints and select based on preference with fallback, preferring the smaller region when both exist.
- Retry policy: We repeatedly try within the selected hint region until a pre-wait budget elapses before expanding, then try coarse image-driven, then full-region.
- Build: use `./build-all.sh` locally (skips compiling/running tests) to refresh the shaded apps JAR.

## Test Migration

Goal: migrate tests off legacy `org.sikuli.api` to SikuliX `org.sikuli.script` and our `Context`, re‑enable them incrementally in `pom.xml`.

Principles:

- Keep migrated tests compile‑only initially using `@Ignore` to avoid UI/image dependencies.
- Replace legacy `Target`, `DesktopScreenRegion`, `DesktopCanvas`, and mocks with `Pattern` and `Context`.
- Narrow `<testExcludes>` in `pom.xml` as soon as a test compiles on new APIs.
- Prefer the project build script `./build-all.sh` for validation.

Mapping (legacy -> new):

- `org.sikuli.api.Target` -> `org.sikuli.script.Pattern`
- `DesktopScreenRegion`/`Region` -> `Context.getScreenRegion()`
- Canvas/viewer classes -> avoid in unit tests; rely on `@Ignore` or refactor to pure logic
- Legacy mocks -> Mockito for `Action` only; no image/region mocks

Steps per test class:

1. Import `org.sikuli.script.Pattern` and `org.sikuli.slides.api.Context`.
2. Rewrite constructors/usages to pass `Pattern` into actions (e.g., `new TargetAction(pattern, child)`).
3. Add `@Ignore("compile-only migration")` and avoid `action.execute(...)` calls unless headless-safe.
4. Update `pom.xml` `<testExcludes>` to remove that test.
5. Run `./build-all.sh` to ensure compile succeeds.

Status and checklist:

- [x] `AssertActionTest` migrated to `Pattern` + `Context` and enabled for compilation.
- [x] `TargetActionTest` migrated to `Pattern`; enabled for compilation.
- [ ] `WaitActionTest` – replace with `Pattern`, mark ignored, then enable.
- [ ] `ExecutionListenerTest` – decouple from canvas/viewers; make compile‑only first.
- [ ] `RobotActionTest` – keep excluded; move under dedicated profile `hooks` later.
- [ ] Viewer/Interpreter tests – refactor to reader‑only logic; keep UI tests excluded for now.

Build profiles and running:

- Default: `./build-all.sh` (preferred; aligns with repo scripts).
- Unit‑only: `mvn -Punit-only test` (excludes action/UI heavy tests).
- Hooks: `mvn -Phooks test` (only global hook dependent tests; macOS may need permissions).

Enabling strategy in `pom.xml` (`maven-compiler-plugin` > `testExcludes`):

- Remove excludes for migrated compile‑only tests immediately.
- Keep excludes for UI/viewer/driver and hook‑dependent tests until refactored or profiled.


## Prioritization
10

- You can assign numbers or notes here per item above, e.g.:
  - P1: Inspect hints artifacts/logs
  - P2: Environment validation
  - P3: Add mapping logs
  - P4: Overlay rect on hint snapshots
  - P5: Clean lints
  - P6: Expose hint-source CLI

## Monitoring

- We will keep an eye on this file. Feel free to add TODOs, notes, or change priorities here; we’ll follow your updates in subsequent iterations.
