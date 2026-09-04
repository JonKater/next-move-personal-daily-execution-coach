### Task 1: Restore a reproducible and honest build baseline

**Files:**
- Create: `gradlew`
- Create: `gradlew.bat`
- Create: `gradle/wrapper/gradle-wrapper.jar`
- Modify: `app/build.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `gradle/libs.versions.toml`
- Modify: `metadata.json`
- Modify: `README.md`
- Modify: `app/src/test/java/com/example/ExampleRobolectricTest.kt`
- Delete: `app/src/test/java/com/example/GreetingScreenshotTest.kt`
- Delete: `app/src/test/screenshots/greeting.png`

**Interfaces:**
- Consumes: the existing Android application module and `gradle-wrapper.properties` pinned to Gradle 9.3.1.
- Produces: `gradlew` and `gradlew.bat` entry points that build the project without manual signing edits; a test suite with no unresolved `Greeting` symbol.

- [ ] **Step 1: Add a failing baseline check**

Run from PowerShell:

```powershell
Test-Path .\gradlew.bat
Test-Path .\gradle\wrapper\gradle-wrapper.jar
```

Expected: both commands print `False` before the fix.

- [ ] **Step 2: Generate the complete Gradle wrapper**

Use Android Studio’s bundled Gradle or a temporary Gradle 9.3.1 installation:

```powershell
gradle wrapper --gradle-version 9.3.1 --distribution-type bin
```

Expected: `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar` exist, and the existing distribution URL remains `gradle-9.3.1-bin.zip`.

- [ ] **Step 3: Restore default debug signing**

Remove the custom `debugConfig` block and replace the current debug build type in `app/build.gradle.kts` with:

```kotlin
buildTypes {
  release {
    isCrunchPngs = false
    isMinifyEnabled = false
    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    signingConfig = signingConfigs.getByName("release")
  }
  debug {}
}
```

Keep the release signing configuration unchanged.

- [ ] **Step 4: Remove unused AI and networking surface**

Remove the Google services and secrets plugin aliases from both Gradle plugin blocks. Remove the Firebase platform, Firebase AI, Firebase App Check, Retrofit, Moshi, OkHttp, logging interceptor, and their KSP codegen dependency from `app/build.gradle.kts`. Remove only the now-unreferenced aliases and versions from `gradle/libs.versions.toml`.

Set `metadata.json` to:

```json
{
  "name": "Next Move",
  "description": "Local-first Personal Daily Execution Coach that helps choose the next action.",
  "requestFramePermissions": [],
  "majorCapabilities": []
}
```

- [ ] **Step 5: Repair generated tests and documentation**

Change the Robolectric assertion to:

```kotlin
assertEquals("Next Move", appName)
```

Delete the unresolved `GreetingScreenshotTest.kt` and corrupted `greeting.png`. Rewrite the README run steps to require Android Studio, SDK 36.1, JDK 17 or newer, and `./gradlew`/`gradlew.bat`; remove `.env`, API-key, signing-line deletion, and Play upload-key instructions.

- [ ] **Step 6: Verify the baseline**

Run:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Expected: all tasks pass and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 7: Commit**

```powershell
git add gradlew gradlew.bat gradle app/build.gradle.kts build.gradle.kts metadata.json README.md
git commit -m "build: restore reproducible Android baseline"
```

---

