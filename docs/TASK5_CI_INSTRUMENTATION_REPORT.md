# Task 5: CI instrumentation investigation

Date: 2026-10-16, Europe/Dublin. This report concerns the CI failure brief, separately from the earlier Task 5 acceptance report.

## A. Repository state

- Starting checkout: `fix/ci-journey-state`, `5d331b85ac8133d113d06ec643d194c59abf543f`.
- Starting tree already contained changes to README, TESTING, CHANGELOG, Task 3/4 documents and the journey test, plus local captures. These were not used as the diagnostic baseline or overwritten.
- Isolated worktree: `verification/task5-ci-diagnostics`, branch `fix/task5-ci-diagnostics`, based on the exact starting commit. Production source is unchanged.
- Modified scope: `ShoppingJourneyTest.kt`, new `JourneyDiagnostics.kt` (androidTest only), `scripts/run_android_journey.sh`, `.github/workflows/android.yml`, the seven project documents, and this report. Local snapshots/logs remain ignored and are not included in publishing.
- Final commit and clean-tree verification: pending completion of diagnostic validation.

## B. Root-cause investigation

### Original symptom and baseline

[GitHub run 38818122991](https://github.com/brunkonjaa/Liz-s-Shopping-App/actions/runs/38818122991) failed waiting 30 seconds for the ViewModel to contain Bread at quantity 1, immediately after the first catalogue Add. The workflow's build, unit/lint/APK and packaged-security checks passed. The retained artifact contains XML, runner reports and logcat, but no screenshot, semantics or application state at this timeout. The old screenshot capture belonged to `waitForText`, not `waitForBreadQuantity`.

The unchanged failed job was rerun during this investigation. Job `116719669324` failed at the same helper and call site; reports were uploaded successfully (artifact `12012717530`). This is a reproduced CI failure, not a successful retry. The unchanged application/test APK also passed the full journey on Samsung SM-A127F / Android 13 through direct AndroidJUnitRunner in 31.569 seconds (`OK (1 test)`).

The earlier phone comparison was inspected: both Gradle XML results show one test with zero failures/errors; saved SQLite snapshots from committed and scrolling variants contain Bread quantity 7, favourite default 1 and empty other shop lists. The final phone screenshot shows the empty home state. The historical local failure is a different `waitForText` timeout; it does not establish the current first-add mechanism or CI exclusivity.

### Fresh phone diagnostic observation

The passing diagnostic phone run recorded exactly one enabled, displayed Add with a click action, center `(285, 1154.5)` in a 720x1600 window at density 280 dpi, IME hidden, and a resumed focused activity. Immediately after dispatch the ViewModel had Bread 1 while committed readback still had no Bread; the next capture had durable Bread 1. The observer then recorded quantities through 7, favourite default 1, Aldi empty and the return to Lidl. These are observations of a passing phone run, not proof of the failing emulator mechanism.

An initial diagnostic run failed before Add because Android had not initialized its app-owned external output directory; this diagnostic defect was corrected with the platform directory API and non-masking setup/teardown. Runtime artifact inspection also found that collection-level semantics output defaulted to roots only; the helper now prints each root interaction with descendants.

### Evidence boundary and hypotheses

- Scrolling: an existing local scroll/enable-check edit passes on the phone, as does the committed test. This comparison alone does not establish causation.
- Keyboard/viewport: old CI logcat shows IME activity and a 1080x1920 Compose view. Actual keyboard visibility, clipped Add bounds and scroll position at the failed click were absent. These now have dedicated captures.
- Synchronization: the original 30-second ViewModel wait is preserved. A returned `performClick` only establishes completion of injection; it does not prove the Add callback ran.
- State/rendering/persistence: capture transient ViewModel/message events separately from the visible semantics/hierarchy and committed SQLite row. Optimistic state is not durable proof. Absence of an event can reflect StateFlow conflation; absence of a SQLite exception does not prove success because production catches write errors.
- No failure category or production cause is confirmed from the original artifacts. No production, schema, permissions, navigation or animation change is justified at this stage.

## C. Changes and justification

- At the original Add control: retain the exact selector and click, while recording unique match count, node ID, enabled/click action, clipped root/window bounds and centers, displayed state and ancestor vertical scroll range. Capture activity/lifecycle, window focus, IME and viewport without changing them.
- Around the interaction: record started/returned/thrown dispatch separately from ViewModel/message events and committed Bread state. Optional method tracing (`journeyMethodTrace=true`) records exact save/transaction entry and exit if further evidence is necessary; it is a separate experiment because tracing changes timing.
- At the actual quantity timeout: preserve PNG, accessibility XML, merged/unmerged semantics, target bounds, state/readback report, app-PID logcat and the original exception stack. Independent capture failures are logged without replacing the test exception. Assertions, test timeouts and application behaviour remain unchanged.
- CI script: collect selected logcat tags while the emulator is alive, record configuration/IME/activity details, tee Gradle output, preserve its exit status, and collect remaining evidence in an EXIT trap. This avoids attempting to collect all evidence after the emulator action has stopped the emulator or Gradle has uninstalled the app.
- Workflow: retain the existing always-upload step, XML, Android test reports and Gradle additional output, and include the new host diagnostic directory. Build/security checks and emulator configuration remain unchanged. No dependencies added.
- Seven project documents describe the diagnostic boundary, privacy constraints and outstanding CI acceptance criteria.

## D. Verification

| Check | Fresh result |
| --- | --- |
| Unchanged unit/lint/debug/test APK build | Passed (some tasks from cache) |
| Unchanged Samsung direct journey | Passed, 1 test, 31.569 seconds |
| Unchanged GitHub Android 13 journey rerun | Failed at first Bread quantity 1; artifact upload passed |
| Diagnostic test Kotlin compilation | Passed |
| Diagnostic unit tests, lint, APK/test APK build without cache | Passed: all 83 tasks executed, 50 unit tests, 0 failures/errors; lint 0 errors (13 warnings); build 3m 41s |
| Diagnostic phone runs and timeout-artifact probe | First diagnostic runtime exposed an output-directory initialization defect; corrected. Subsequent journeys passed in 35.716s and 37.386s. Final semantics dumps contain descendants (53-56 KiB). Final ViewModel and committed SQLite both contain Bread 7 and favourite default 1 after recreation. Controlled quantity-999 timeout probe passed: the expected 30-second failure retained valid PNG/XML, full semantics, state/window/button reports, app logs and the original stack, with no capture errors. Normal source/assertions restored and test APK rebuilt |
| CI collection shell syntax and exit-status harness | Passed: failure 7 and success 0 retained runner stderr/stdout, logs/configuration and exact exit status |
| Diagnostic GitHub workflow and actual failure artifacts | Pending publishing and execution |
| Local matching Android 13 emulator | Not available: only API 37 images installed |

Local APK SHA-256 before diagnostic changes: `3962A6F75694F648FE25EAFAB05C41B14E62B948F39ACBEF6D74302379AD8633`. The phone was snapshotted before testing; the original database had no saved state row. Each disposable run is restored from those original bytes, without `pm clear` or uninstalling the target app. Original database/preferences must be restored at completion and verified.

## E. Remaining risks

The exact first-add failure mechanism remains unknown pending diagnostic CI execution. The phone has a different viewport, density, animations, keyboard, ABI and graphics/runtime environment. Method tracing and diagnostic collection can change scheduling, so any passing diagnostic run needs comparison and repetition. A controlled timeout probe verifies artifact generation but cannot establish the cause of the genuine CI failure.

The journey covers quantities, completion, favourites, shop isolation and activity recreation; it does not establish reboot persistence, accessibility coverage or broad release readiness. No corrected-test or reliable-CI claim can be made yet.

## F. Verdict

**PARTIALLY RESOLVED**: the unchanged CI failure was reproduced and targeted diagnostics implemented. Root cause, correction and CI verification remain outstanding. A local pass alone does not resolve this task.
