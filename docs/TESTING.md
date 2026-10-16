# Testing

## Task 12 checks - 2026-10-16

The local Release build passed 55 unit tests, lint (0 errors, 16 warnings) and signed Release assembly. We installed and launched it on a Samsung SM-A127F running Android 13, then inspected Home, shop cards, the drawer and Settings. The screenshots appear in the [Task 12 report](TASK12_VISUAL_REFINEMENT_REPORT.md).

The connected shopping journey was attempted but failed at its first Lidl card selector. API 37 runtime, backup/restore UI, custom image selection, dark mode, TalkBack, large fonts, landscape and reboot/update persistence remain unverified. See the [Task 12 report](TASK12_VISUAL_REFINEMENT_REPORT.md) for the failure details.

## Earlier Task 5 CI verification - 2026-10-16

Implementation baseline `265b941` passed the phone/CI checks below. Documentation consolidation `ee734ae` also passed both jobs in [run 38827147754](https://github.com/brunkonjaa/Liz-s-Shopping-App/actions/runs/38827147754). A subsequent review strengthens the journey with committed-database assertions for shop selection and independent quantities; this changes only androidTest, with separate verification recorded below. [GitHub run 38825384833](https://github.com/brunkonjaa/Liz-s-Shopping-App/actions/runs/38825384833) passed both build/security and API 33 x86_64 Pixel 2 instrumentation. The consolidated PR is checked again before merge, and main's push workflow checks the resulting integration.

| Check | Observed evidence |
| --- | --- |
| Unit tests | 50 passed, 0 failures/errors: 34 RulesTest and 16 StorageTest. A full uncached run executed all 83 build tasks. |
| Lint | Passed, 0 errors and 13 existing warnings. |
| Debug and test APK builds | Passed. Task 5 app APK SHA-256 stayed `3962A6F75694F648FE25EAFAB05C41B14E62B948F39ACBEF6D74302379AD8633`. |
| Packaged security | GitHub manifest checks passed; no network/runtime permissions and launcher-only exported component. |
| Samsung direct journey at that revision | 2/2 passed: fresh home 69.295s; saved empty Aldi startup 68.745s. |
| Samsung Gradle journey at that revision | 1 test, 0 failures/errors; XML duration 80.184s; command 1m 58s. Gradle retained 78 diagnostic files. |
| GitHub Android 13 journey | 1 test, 0 failures/errors; testcase 25.391s. Same Pixel 2/API 33/x86_64/display/graphics settings as failed runs. |
| Review-strengthened Samsung journey | Passed: 1 test, 0 failures, 68.805s with committed Aldi/Lidl and quantity readback assertions. Original DB/preferences restored byte-for-byte; app launched. |
| Diagnostics and data guard | Controlled quantity-timeout probe retained valid PNG/XML, full semantics, state/logs and original stack. Existing-data probe refused a non-empty fixture and left database bytes unchanged. |
| Phone preservation | Original DB/preferences restored and byte-verified again after final installation, launch and UI inspection. Four default shops each show zero items, matching preflight state. |

## Commands and prerequisites

Use JDK 17, compile SDK 37.0, build tools 36.0.0 and the wrapper. The workflow provisions its required SDK packages.

```text
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest --console=plain
./gradlew connectedDebugAndroidTest --console=plain
```

Set `ANDROID_SERIAL` when multiple targets exist. Use a disposable empty installation. Gradle may uninstall/reset the target; preserve household data first and reinstall/restore afterward where appropriate. The journey's own fixture guard refuses existing list/favourite/previous/custom-product data; it never clears that data to satisfy assertions.

## Task 5 journey contract at that revision

The journey loads state, explicitly selects Lidl, selects actual Tab-role navigation, searches/replaces Bread text and uses current catalogue/favourite/list controls. Content actions scroll into view and require displayed/enabled state. It waits for the Material snackbar's Polite/Dismiss semantics to disappear before card interaction, with no fixed sleeps or increased state timeout.

It checks first/second Add counts 1/2, five increments and one decrement to 6, completion both ways, favourite default 1, favourite reuse to 7, Aldi isolation, return to Lidl and quantity 7 after activity recreation. Independent read-only SQLite assertions require committed Aldi before returning to Lidl, then committed Lidl with list count 7, favourite count 1 and an empty Aldi list before/after recreation. The Aldi write barrier prevents an older Lidl snapshot satisfying the final assertion while a save is pending or has failed. Activity recreation retains the ViewModel; these database assertions, rather than recreation alone, establish committed selection/quantity state. It does not substitute a direct ViewModel call for UI interaction.

## CI diagnosis and artifacts

The unchanged CI test tapped a fully clipped Add node below the shorter viewport. Scrolling fixed first Add; a second experiment showed the first snackbar covering the second Add centre. Current before/ready/after captures and successful state/readback confirm both interaction corrections. [The structured report](TASK5_CI_INSTRUMENTATION_REPORT.md) preserves failed experiments and successful evidence.

Gradle pulls `additionalTestOutputDir/journey-diagnostics` before uninstall. CI also uploads `app/build/reports/journey-diagnostics` with runner output, filtered logcat, emulator/window configuration and exit status. Artifact upload uses `always()`. Original exception stacks are preserved independently of capture errors.

Optional trace experiment: add `-Pandroid.testInstrumentationRunnerArguments.journeyMethodTrace=true`. Tracing is narrowed to injection/state observation to avoid exhausting the buffer on dumps. A trace entry alone does not prove save success; compare committed readback. Never infer missing callbacks from an overflowing trace.

## Coverage limits and historical evidence

That Task 5 journey was not a complete acceptance/release test. Backup/restore UI error paths, editor/custom products, custom-shop management, image selection/revoked URIs, reboot/in-place-update persistence, TalkBack, large fonts, landscape, contrast and representative-user checks remained unverified. Activity recreation is not process restart or reboot. No local API 33 image was installed; matching emulator execution was on GitHub. API 37 runtime remains unverified.

Earlier dated UI/manual/performance results are retained in [Task 3](TASK_3_IMPLEMENTATION.md), [Task 4](TASK_4_IMPLEMENTATION.md), [historical acceptance](TASK5_ACCEPTANCE_REPORT.md) and [Task 7](TASK7_INTERACTION_AND_PERFORMANCE_REPORT.md). They do not establish current visual fidelity, full sheet coverage or steady-state frame pacing. Private captures/snapshots remain ignored.

## Earlier Task 11 production Release verification - 2026-10-16

`scripts/build-release.ps1` ran `clean testDebugUnitTest lintDebug assembleRelease --console=plain`: **BUILD SUCCESSFUL**; 50 unit tests passed, lint completed with 0 errors and 13 existing warnings, and the R8/resource-shrunk signed Release APK assembled. Connected instrumentation passed on the fresh application fixture: 1 test, 0 failures/errors (XML duration 80.237 s). An initial guard correctly refused the phone's pre-existing non-empty state; the user's Task 11 confirmation authorized resetting only this app, after which the journey passed. The final release was installed and launched separately on the SM-A127F / Android 13.

The installed Release journey verified four default shops, switching and isolated lists, catalogue display/Add, product sheet open/save, package size 2 L with package count ×1, list quantity controls, completion/undo, favourite default remaining 1 L ×1, and state after force-stop/restart. App test state was then cleared with `pm clear ie.suric.lizshopping`; the release remains installed and freshly launched. Five matched 60-second scroll trials yielded 548/17,554 GFXInfo janky frames (3.12%), with per-run details and a separate 30-second Perfetto FrameTimeline analysis in [Task 11 report](TASK11_PRODUCTION_RELEASE_REPORT.md). Accessibility service and phone settings were left unchanged.

Custom-shop management/removal, custom categories, image picker, backup/restore, offline mode with connectivity disabled, dark/system themes, TalkBack interaction/focus, reboot/update persistence and API 37 remain **NOT VERIFIED**. The history comparison to Task 9 is indicative, not a causal estimate; preserve those boundaries when describing release performance.

## Detailed Task 12 verification - 2026-10-16

Fresh `scripts/build-release.ps1` completed successfully: RulesTest 36/36 and StorageTest 19/19 passed; lint reported 0 errors and 16 warnings; signed Release APK assembled. Added coverage for case-insensitive All Items ordering, custom shop image backup round-trip, v2 backup compatibility and a v2-to-v3 SQLite upgrade preserving list state. Release was installed and launched on Samsung SM-A127F / Android 13, and Home, shop cards, drawer and Settings were visually inspected. The connected journey was attempted on the phone. After replacing the existing Release signature, its one test failed at `startAtLidl`: the selector could not find the clickable combined `Lidl` / `0 items` shop card. No Debug build remains installed; the signed Release is installed and launched. API 37 emulator execution remains unverified. See [the Task 12 report](TASK12_VISUAL_REFINEMENT_REPORT.md).
