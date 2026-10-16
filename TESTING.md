# Testing

## Current verified implementation - 2026-10-16

Source revision `265b941` is the current app/journey implementation; subsequent consolidation edits update documentation and ignore rules. [GitHub run 38825384833](https://github.com/brunkonjaa/Liz-s-Shopping-App/actions/runs/38825384833) passed both build/security and API 33 x86_64 Pixel 2 instrumentation. The consolidated PR is checked again before merge, and main's push workflow checks the resulting integration.

| Check | Observed evidence |
| --- | --- |
| Unit tests | 50 passed, 0 failures/errors: 34 RulesTest and 16 StorageTest. A full uncached run executed all 83 build tasks. |
| Lint | Passed, 0 errors and 13 existing warnings. |
| Debug and test APK builds | Passed. Task 5 app APK SHA-256 stayed `3962A6F75694F648FE25EAFAB05C41B14E62B948F39ACBEF6D74302379AD8633`. |
| Packaged security | GitHub manifest checks passed; no network/runtime permissions and launcher-only exported component. |
| Current Samsung direct journey | 2/2 passed: fresh home 69.295s; saved empty Aldi startup 68.745s. |
| Current Samsung Gradle journey | 1 test, 0 failures/errors; XML duration 80.184s; command 1m 58s. Gradle retained 78 diagnostic files. |
| GitHub Android 13 journey | 1 test, 0 failures/errors; testcase 25.391s. Same Pixel 2/API 33/x86_64/display/graphics settings as failed runs. |
| Diagnostics and data guard | Controlled quantity-timeout probe retained valid PNG/XML, full semantics, state/logs and original stack. Existing-data probe refused a non-empty fixture and left database bytes unchanged. |
| Phone preservation | Original DB/preferences restored and byte-verified again after final installation, launch and UI inspection. Four default shops each show zero items, matching preflight state. |

## Commands and prerequisites

Use JDK 17, compile SDK 37.0, build tools 36.0.0 and the wrapper. The workflow provisions its required SDK packages.

```text
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest --console=plain
./gradlew connectedDebugAndroidTest --console=plain
```

Set `ANDROID_SERIAL` when multiple targets exist. Use a disposable empty installation. Gradle may uninstall/reset the target; preserve household data first and reinstall/restore afterward where appropriate. The journey's own fixture guard refuses existing list/favourite/previous/custom-product data; it never clears that data to satisfy assertions.

## Current journey contract

The journey loads state, explicitly selects Lidl, selects actual Tab-role navigation, searches/replaces Bread text and uses current catalogue/favourite/list controls. Content actions scroll into view and require displayed/enabled state. It waits for the Material snackbar's Polite/Dismiss semantics to disappear before card interaction, with no fixed sleeps or increased state timeout.

It checks first/second Add counts 1/2, five increments and one decrement to 6, completion both ways, favourite default 1, favourite reuse to 7, Aldi isolation, return to Lidl and quantity 7 after activity recreation. It does not substitute a direct ViewModel call for UI interaction.

## CI diagnosis and artifacts

The unchanged CI test tapped a fully clipped Add node below the shorter viewport. Scrolling fixed first Add; a second experiment showed the first snackbar covering the second Add centre. Current before/ready/after captures and successful state/readback confirm both interaction corrections. [The structured report](docs/TASK5_CI_INSTRUMENTATION_REPORT.md) preserves failed experiments and successful evidence.

Gradle pulls `additionalTestOutputDir/journey-diagnostics` before uninstall. CI also uploads `app/build/reports/journey-diagnostics` with runner output, filtered logcat, emulator/window configuration and exit status. Artifact upload uses `always()`. Original exception stacks are preserved independently of capture errors.

Optional trace experiment: add `-Pandroid.testInstrumentationRunnerArguments.journeyMethodTrace=true`. Tracing is narrowed to injection/state observation to avoid exhausting the buffer on dumps. A trace entry alone does not prove save success; compare committed readback. Never infer missing callbacks from an overflowing trace.

## Coverage limits and historical evidence

The current journey is not a complete acceptance/release test. Backup/restore UI error paths, editor/custom products, custom-shop management, image selection/revoked URIs, reboot/in-place-update persistence, TalkBack, large fonts, landscape, contrast and representative-user checks remain unverified. Activity recreation is not process restart or reboot. No local API 33 image was installed; matching emulator execution was on GitHub. API 37 runtime remains unverified.

Earlier dated UI/manual/performance results are retained in [Task 3](TASK_3_IMPLEMENTATION.md), [Task 4](TASK_4_IMPLEMENTATION.md), [historical acceptance](docs/TASK5_ACCEPTANCE_REPORT.md) and [Task 7](docs/TASK7_INTERACTION_AND_PERFORMANCE_REPORT.md). They do not establish current visual fidelity, full sheet coverage or steady-state frame pacing. Private captures/snapshots remain ignored.
