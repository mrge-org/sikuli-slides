# Sikuli Slides — Developers Guide (Concise)

## Overview
- **Modules**
  - `sikuli-slides-api` (root): core library and APIs.
  - `apps/`: CLI apps (recorder/executor) as shaded JARs.
- **Java level**: source/target 1.8.
- **Logging**: SLF4J + Log4j (shaded in `apps`).

## Prerequisites
- **JDK**: 8+ (Java 8 target).
- **Maven**: 3.6+.
- **macOS**: grant Accessibility to your Terminal/IDE for UI control.

## Repository layout
- Core: `src/main/java/`, `src/test/java/`, `pom.xml`
- Apps: `apps/src/main/java/`, `apps/pom.xml`
- Native/libs: `sikulixlibs/`, `apps/lib/`

## Build

1) Preferred: one-shot build (API + apps)
```bash
# from repository root
./build-all.sh
```
Produces both the API JAR (installed to your local Maven repository — default `~/.m2/repository` unless overridden) and the shaded apps JAR.

2) Optional: manual Maven commands (if you are not using the script)
```bash
# from repository root (API)
mvn -Dmaven.test.skip=true clean package install

# from apps/ (shaded JAR)
(cd apps && mvn -Dmaven.test.skip=true clean package)
```
Notes:
- We avoid `-DskipTests=true` because it still compiles tests and can fail on legacy tests; use `-Dmaven.test.skip=true` or just the script above.
- You may see a warning about `install4j` `systemPath` in `apps/pom.xml` — OK for now; we will clean this later.

Tip: print your local Maven repository path
```bash
mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout
```

## Run
- **Recorder** (new option: selectable capture backend):
  - `--recorder_capture`: `sikuli | awt_raw | both`
  - Recommended: `awt_raw` to avoid Retina/downsampling issues.
```bash
java -jar apps/target/sikuli-slides-1.7.1-SNAPSHOT.jar \
  --recorder_capture awt_raw \
  --region 100,100,800,600 \
  --output out.pptx
```

- **Executor**:
```bash
java -cp apps/target/sikuli-slides-1.7.1-SNAPSHOT.jar org.sikuli.slides.apps.ExecuteMain \
  --input out.pptx
```

### Pattern/Region verifier (no-scaling)
- Minimal tool to verify a saved pattern against a saved region with strict 1:1 matching (no scaling, no preprocessing).
- Class: `org.sikuli.slides.apps.VerifyPatternInRegion`
- Prints best location and scores (SSD lower=better, NCC [-1..1] higher=better). Optional visualization output.

Build (apps module):
```bash
# from apps/
mvn -Dmaven.test.skip=true clean package
```

Run against saved failure artifacts:
```bash
java -cp apps/target/classes org.sikuli.slides.apps.VerifyPatternInRegion \
  "/Users/gpetrov/mirror_src/sikuli-slides-PoC/target/failed-search/slide-1-region.png" \
  "/Users/gpetrov/mirror_src/sikuli-slides-PoC/target/failed-patterns/slide-1-pattern.png" \
  "/Users/gpetrov/mirror_src/sikuli-slides-PoC/target/verify-slide-1.png"

# Alternatively using the shaded jar on the classpath
java -cp apps/target/sikuli-slides-1.7.1-SNAPSHOT.jar org.sikuli.slides.apps.VerifyPatternInRegion \
  "/Users/gpetrov/mirror_src/sikuli-slides-PoC/target/failed-search/slide-1-region.png" \
  "/Users/gpetrov/mirror_src/sikuli-slides-PoC/target/failed-patterns/slide-1-pattern.png" \
  "/Users/gpetrov/mirror_src/sikuli-slides-PoC/target/verify-slide-1.png"
```

Notes:
- Arguments order is `<region.png> <pattern.png> [out-visual.png]`.
- No thresholding/exit code logic is applied; this is diagnostic-only.

## Recorder diagnostics
- `ScreenshotEventDetector` writes probe artifacts when capture dimensions change:
  - `probe.awt.png`, `probe.sikuli.png`
  - `probe.meta.txt` (ROI, image dims, computed scale)
- Recorded click coordinates are scaled to screenshot pixel space to align with saved images.

## Matching and clicking behavior
- `TargetAction.execute()` performs visual matching in the execution region.
- `LeftClickAction` clicks the **center** of the matched region using AWT Robot (fallback: SikuliX).
- If you need to click the **recorded point** instead of center, we can add a recorded offset (dx, dy) pipeline. [TODO]

## Retina/iOS Simulator tips
- Use `--recorder_capture awt_raw` for native device-pixel screenshots.
- Verify `probe.meta.txt` scale ≈ 1.0 to avoid mismatches.
- Prefer a smaller ROI during experiments (e.g., 800x600) for faster captures.

## IDE / Debugging [Placeholders]
- **Preferred IDE**: [TODO: Document IntelliJ/VS Code setup]
- **VS Code**:
  - `.vscode/launch.json` exists. [TODO: add/verify launch configs for Recorder/Executor with CLI args]
- **Logging configuration**: [TODO: Provide override instructions for log4j properties]

## Dependencies [Placeholders]
- **SikuliX**: `com.sikulix:sikulixapi:2.0.6-SNAPSHOT` (shaded in `apps`). [TODO: Doc how to build/pin exact version]
- **OpenCV/Tess4J/JNA**: pulled via Maven. [TODO: Native notes for Linux/Windows]
- **install4j**: `apps/lib/i4jruntime.jar`. [TODO: Packaging steps and licensing]

## Roadmap / TODOs
- Implement recorded click-offset playback. [TODO]
- Clean POM `systemPath` usage. [TODO]
- Guided recording mode (Enter to capture, Esc to finish, beep cues). [TODO]
- CI/CD with GitHub Actions. [TODO]
- Expand docs: dependency builds, IDE run/debug, troubleshooting. [TODO]
