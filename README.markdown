sikuli-slides
=============
sikuli-slides is a new tool that enables you to automate and test Graphical User Interfaces (GUIs) 
using presentation slides by adding screenshots and annotating them. sikuli-slides aims at enabling 
users to automate GUIs and produce a live screenshot by screenshot tutorials without having to write code.

For more info visit [the project website](http://slides.sikuli.org).

## Version 1.6 (What's New)

The 1.6 release modernizes build and test infrastructure and updates native input APIs:

- Updated to jnativehook 2.x (`com.github.kwhat.jnativehook`), replacing legacy `org.jnativehook`.
  - `GlobalScreen` usage is now via static methods (no `getInstance()`), e.g., `GlobalScreen.addNativeKeyListener(...)`.
  - Key constants renamed from `VK_*` to `VC_*` (e.g., `VC_ESCAPE`).
  - `GlobalScreen.unregisterNativeHook()` throws checked `NativeHookException`; calls are wrapped in try/catch.
- Build upgrades for JDK 21 compatibility:
  - Maven Compiler Plugin 3.11.0 with `<release>8</release>` to keep Java 8 bytecode.
  - Maven Surefire Plugin 3.2.5.
  - Mockito upgraded to `mockito-core:5.12.0` for tests.
- Test profiles introduced:
  - `unit-only`: runs fast unit tests, excludes UI/native/hook-heavy tests.
  - `integration`: runs image/OpenCV integration tests, excludes global hook tests.
  - `hooks`: runs only tests that require global keyboard/mouse hooks.
- OpenCV JNI resolution for tests:
  - Added a test-scoped dependency on legacy OpenCV 2.4.9 natives (Windows x86_64) to satisfy `sikuli-api:1.2.0` transitive expectations.

Project/module versions were bumped to 1.6 (including `apps/pom.xml`).

## Prerequisites

- Java: JDK 21 (builds target Java 8 bytecode).
- Maven: 3.9.x or newer.

## Build

```
mvn -q -DskipTests package
```

Shaded app jar is produced in `apps/` module after building that module.

## Running Tests

The test suite contains unit tests, image/OpenCV integration tests, and global input hook tests. Use the Maven profiles to run what is appropriate for your environment.

- Unit tests only (recommended default / CI):
  ```
  mvn -Punit-only test
  ```

- Integration tests (image/OpenCV) without global hooks:
  ```
  mvn -Pintegration test
  ```
  Notes:
  - Uses legacy OpenCV 2.4.9 JavaCPP presets (Windows x86_64 natives) to satisfy `sikuli-api:1.2.0`.
  - On macOS, enable the `macos` profile to pull macOS natives:
    ```
    mvn -Pmacos -Pintegration test
    ```

- Global hook tests (requires desktop session & permissions):
  ```
  mvn -Phooks test
  ```
  Requirements:
  - Run in an interactive desktop session (not headless/CI service session).
  - Ensure antivirus/security tools don't block native DLL extraction for `jnativehook`.
  - The profile sets `-Djava.awt.headless=false` and a writable `-Djava.io.tmpdir`.
  - On macOS:
    - Use the `macos` profile to add OpenCV natives:
      ```
      mvn -Pmacos -Phooks test
      ```
    - These OpenCV presets target Intel (x86_64). On Apple Silicon (ARM64), run an x86_64 JVM under Rosetta (e.g., install an Intel JDK and use that JAVA_HOME) or provide appropriate natives.

## Notes for Testers (Changes from 1.5 → 1.6)

- jnativehook migration:
  - Import package changed to `com.github.kwhat.jnativehook.*`.
  - Static `GlobalScreen` API; ensure listeners are added/removed with static methods.
  - Replace `VK_*` with `VC_*` constants in key-related code.
  - Wrap `GlobalScreen.unregisterNativeHook()` in try/catch for `NativeHookException`.
- OpenCV JNI behavior:
  - Image tests under `integration` rely on JavaCPP presets auto-extracting natives.
  - No manual PATH changes required for Windows x86_64; other OSes may need platform-specific classifier deps.
- Mockito upgrade:
  - Moved from `mockito-all:1.9.5` to `mockito-core:5.12.0`.
  - If adding new tests using Mockito, use the modern API (e.g., `mock()`, `when()` from `org.mockito.Mockito`).
- Profiles behavior:
  - `unit-only` excludes: `**/api/actions/**`, `**/api/slideshow/**`, `**/sikuli/**`.
  - `integration` excludes only hook-dependent tests: `RobotActionTest`, `LatchTest`, `AutomationExecutor*Test`, `SlideShowControllerHotkeyTest`.
  - `hooks` includes only the above hook-dependent tests.

## Troubleshooting

- jnativehook errors (e.g., `Unable to extract JNativeHook.dll`, `NoClassDefFoundError: GlobalScreen`):
  - Run via `-Phooks` only in a real desktop session.
  - Ensure temp dir is writable; the profile sets `-Djava.io.tmpdir=${project.build.directory}/tmp`.
  - Disable antivirus/real-time scanning on the build dir if it locks extracted DLLs.

- OpenCV `UnsatisfiedLinkError`:
  - Confirm the test-scoped OpenCV classifier dependency is present (Windows x86_64). For other OSes, add suitable classifiers under a profile.

