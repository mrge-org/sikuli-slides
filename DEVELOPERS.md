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
1) Build and install API to local Maven repo (skip tests entirely):
```bash
# from repository root
mvn -Dmaven.test.skip=true package install
```
Expected output includes:
```
[INFO] Installing .../target/sikuli-slides-api-1.7.1-SNAPSHOT.jar to ~/.m2/repository/.../sikuli-slides-api-1.7.1-SNAPSHOT.jar
[INFO] BUILD SUCCESS
```

2) Build apps (shaded JAR):
```bash
# from apps/ directory
mvn -DskipTests=true clean package
```
Produces:
- `apps/target/sikuli-slides-1.7.1-SNAPSHOT.jar` (shaded)

Notes:
- You may see a warning about `install4j` `systemPath` in `apps/pom.xml` — OK for now; we will clean this later.

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
