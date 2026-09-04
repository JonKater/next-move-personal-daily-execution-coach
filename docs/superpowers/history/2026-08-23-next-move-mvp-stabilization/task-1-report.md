# Task 1 report: reproducible and honest build baseline

## Status

`DONE_WITH_CONCERNS`. The requested wrapper, build configuration, dependency cleanup, metadata, documentation, and generated-test repairs are in place. The required Android build/test gate could not execute on this machine because the worktree path contains non-ASCII characters and no Android SDK is installed or configured.

Commit: `7f6a071 build: restore reproducible Android baseline`.

## Implementation details

- Generated the complete Gradle 9.3.1 wrapper: `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar`. The wrapper properties remain pinned to `https\://services.gradle.org/distributions/gradle-9.3.1-bin.zip`.
- Removed custom `debugConfig`; the `debug` build type is now the default empty configuration. The existing `release` signing configuration and its release build-type reference are unchanged.
- Removed Google services/secrets plugins and the Firebase/AI/networking dependency surface, including their version-catalog aliases and versions.
- Set the exact requested local-first metadata.
- Changed the Robolectric app-name assertion to `assertEquals("Next Move", appName)`, and deleted the unresolved screenshot test and corrupted image.
- Rewrote the README prerequisites and run instructions for Android Studio, Android SDK 36.1, JDK 17+, and the project wrapper.

## Files changed

- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.jar`
- `app/build.gradle.kts`
- `build.gradle.kts`
- `gradle/libs.versions.toml`
- `metadata.json`
- `README.md`
- `app/src/test/java/com/example/ExampleRobolectricTest.kt`
- deleted `app/src/test/java/com/example/GreetingScreenshotTest.kt`
- deleted `app/src/test/screenshots/greeting.png`

## Commands and results

### Pre-fix baseline check

```powershell
Test-Path .\gradlew.bat
Test-Path .\gradle\wrapper\gradle-wrapper.jar
```

Output:

```text
False
False
```

`Get-Command gradle -All` produced no result, and Android Studio was not installed at `C:\Program Files\Android\Android Studio`.

### Wrapper generation

The initial direct download was sandbox-blocked:

```powershell
Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-9.3.1-bin.zip' -OutFile "$env:TEMP\gradle-9.3.1-bin.zip"
```

Output:

```text
Invoke-WebRequest: Der Zugriff auf einen Socket war aufgrund der Zugriffsrechte des Sockets unzulässig.
```

After explicit permission, the official archive downloaded. The first temporary Gradle invocation was blocked by the Android non-ASCII path guard. The corrected documented one-off override generated the wrapper:

```powershell
& (Join-Path $env:TEMP 'gradle-9.3.1\bin\gradle.bat') '-Pandroid.overridePathCheck=true' wrapper --gradle-version 9.3.1 --distribution-type bin
```

Output:

```text
> Configure project :app
WARNING: The option setting 'android.overridePathCheck=true' is experimental.

> Task :wrapper

BUILD SUCCESSFUL in 16s
1 actionable task: 1 executed
```

Post-check output:

```text
True
True
True
distributionUrl=https\://services.gradle.org/distributions/gradle-9.3.1-bin.zip
```

### TDD RED/GREEN evidence

The requested generated-test repair was approached test-first. Before the test/deletion changes, this targeted command was run:

```powershell
.\gradlew.bat '-Pandroid.overridePathCheck=true' testDebugUnitTest --tests com.example.ExampleRobolectricTest
```

It could not reach compilation or execute the assertion because Gradle reported:

```text
Could not determine the dependencies of task ':app:testDebugUnitTest'.
> SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local properties file.
```

Consequently, an executable RED/GREEN result for the assertion is unavailable in this environment. Static post-change checks confirm `strings.xml` contains `Next Move`, `ExampleRobolectricTest.kt` expects `Next Move`, and the unresolved `GreetingScreenshotTest.kt`/`greeting.png` paths are deleted.

### Required verification gate

Exact required command:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Output:

```text
FAILURE: Build failed with an exception.

An exception occurred applying plugin request [id: 'com.android.application', version: '9.1.1']
> Failed to apply plugin 'com.android.internal.application'.
   > Your project path contains non-ASCII characters. This will most likely cause the build to fail on Windows.

BUILD FAILED in 6s
```

Diagnostic rerun with Android's documented one-time override:

```powershell
.\gradlew.bat '-Pandroid.overridePathCheck=true' testDebugUnitTest lintDebug assembleDebug
```

Output:

```text
FAILURE: Build failed with an exception.

Could not determine the dependencies of task ':app:testDebugUnitTest'.
> SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local properties file.

BUILD FAILED in 7s
```

Configuration-only validation of the edited scripts and version catalog:

```powershell
.\gradlew.bat '-Pandroid.overridePathCheck=true' help
```

Output:

```text
> Configure project :app
WARNING: The option setting 'android.overridePathCheck=true' is experimental.

> Task :help

BUILD SUCCESSFUL in 1s
1 actionable task: 1 executed
```

Static verification command:

```powershell
git diff --check
Test-Path .\gradlew
Test-Path .\gradlew.bat
Test-Path .\gradle\wrapper\gradle-wrapper.jar
rg -n 'debugConfig|libs\.firebase|libs\.retrofit|libs\.moshi|libs\.okhttp|libs\.logging|libs\.converter\.moshi|libs\.plugins\.secrets|libs\.plugins\.google\.services|MissingGoogleServicesStrategy|Greeting' app\build.gradle.kts build.gradle.kts app\src\test gradle\libs.versions.toml
```

Output: `git diff --check` found no whitespace errors; all three wrapper checks printed `True`; `rg` returned no matches.

### Commit

```powershell
git commit -m "build: restore reproducible Android baseline"
```

Output:

```text
[codex/next-move-mvp-stabilization 7f6a071] build: restore reproducible Android baseline
11 files changed, 364 insertions(+), 113 deletions(-)
delete mode 100644 app/src/test/java/com/example/GreetingScreenshotTest.kt
delete mode 100644 app/src/test/screenshots/greeting.png
create mode 100644 gradle/wrapper/gradle-wrapper.jar
create mode 100644 gradlew
create mode 100644 gradlew.bat
```

## Self-review

- Confirmed the Task 1 wrapper artifacts exist and are pinned to Gradle 9.3.1.
- Confirmed the release signing block remains present while debug uses default signing.
- Confirmed no removed Google/Firebase/network aliases remain in the edited Gradle build files, version catalog, or test tree.
- Confirmed all listed Task 1 modified and deleted paths are staged for the required commit.
- Confirmed no build success, test success, lint success, or APK output is claimed: the full gate is environment-blocked.

## Concerns

1. The exact verification command is blocked in this checked-out path by AGP's non-ASCII Windows path guard. This task intentionally does not change `gradle.properties` to add the experimental override.
2. Even with the documented one-off override, no Android SDK is installed/configured (`ANDROID_HOME`/`sdk.dir` missing), so unit tests, lint, assembly, and APK generation cannot be verified here.
3. `app/build/outputs/apk/debug/app-debug.apk` was not created because assembly could not begin.

## Review round 1 fixes

### Files changed

- deleted `.env.example`, which documented Gemini API keys, AI Studio secret injection, and Gemini API calls
- modified `gradle.properties` to remove the now-unused `googleServices.missing.passthrough=true` setting and its comment

### Exact checks and results

```powershell
git diff --check
```

Output: no output; no whitespace errors.

```powershell
rg -n -i --glob '!.git/**' --glob '!.worktrees/**' --glob '!.superpowers/**' --glob '!docs/**' --glob '!**/review*/**' 'GEMINI_API_KEY|Gemini|Firebase|google-services|googleServices' .
```

Output: no matches in product, build, or runtime files. The scan explicitly excludes Git metadata, worktrees, task/report artifacts, planning docs, and historical review artifacts.

```powershell
.\gradlew.bat '-Pandroid.overridePathCheck=true' help
```

Output:

```text
> Configure project :app
WARNING: The option setting 'android.overridePathCheck=true' is experimental.

> Task :help

BUILD SUCCESSFUL in 1s
1 actionable task: 1 executed
```

### Commit

`9dcdf10 chore: remove obsolete AI configuration`

### Self-review

- Confirmed the obsolete `.env.example` path is deleted and no longer advertises AI/API-key setup.
- Confirmed no Google-services configuration remains in `gradle.properties`.
- Confirmed the required repository scan has no matches outside the explicitly excluded planning/history paths.
- Confirmed configuration-only Gradle validation still succeeds.
- The prior SDK-dependent `testDebugUnitTest lintDebug assembleDebug` limitation remains unchanged; this review fix does not claim tests, lint, assembly, or APK generation passed.
