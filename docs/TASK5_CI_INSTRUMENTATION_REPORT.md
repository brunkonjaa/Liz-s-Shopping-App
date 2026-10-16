# Task 5: CI instrumentation investigation

## Consolidation follow-up - 2026-10-16

After the CI correction passed, the user explicitly authorized consolidating documents/work and merging into main. PR #3 now targets main and includes all PR #2 commits; Task 5 itself remains test-only relative to starting commit 5d331b8. The main integration therefore also includes the already-existing route/current-state and one-package count changes from PR #2. No additional production change was made for consolidation.

Consolidation head ee734ae passed both jobs in [run 38827147754](https://github.com/brunkonjaa/Liz-s-Shopping-App/actions/runs/38827147754). Merge review then correctly identified that recreation retains the ViewModel. The test now independently polls the decoded committed SQLite snapshot: first Aldi must commit (a barrier against an older Lidl snapshot), then Lidl must commit with Bread list count 7, favourite count 1 and Aldi list empty. The same readback is required after recreation. A pending/failed final selection can no longer pass using retained optimistic state. No production hook, LocalStore construction, write, fixed sleep or timeout increase is added. The strengthened test APK built in 33s and the direct Samsung run passed: 1 test, 0 failures, 68.805s. Original DB/preferences were verified before testing, restored byte-for-byte afterward, and the app launched. Final-head PR CI is required before merge; main then runs its push workflow.

All current project documents were reconciled. Task 3/4 local notes were retained and refreshed with current verification; older acceptance/performance reports keep dated measurements and explicit supersession notes. The obsolete divergent Task 2 PR is superseded, not merged over the newer catalogue/storage/UI implementation.

Original six-file local edits were backed up under ignored verification output and retained in a named pre-consolidation stash. The earlier scrolling-only test is superseded by the verified current test; its original bytes are still preserved. Private root captures/guidance remain in place under ignore rules. The original Task 5 repository-state notes below describe the investigation starting/ending boundary; the later authorized consolidation advances main after checks pass.

Date: 2026-10-16, Europe/Dublin. This report concerns the CI failure brief, separately from the earlier Task 5 acceptance report.

## A. Repository state

- Starting checkout: `fix/ci-journey-state`, `5d331b85ac8133d113d06ec643d194c59abf543f`.
- Starting tree already contained changes to README, TESTING, CHANGELOG, Task 3/4 documents and the journey test, plus local captures. These were not used as the diagnostic baseline or overwritten.
- Isolated worktree: `verification/task5-ci-diagnostics`, branch `fix/task5-ci-diagnostics`, based on the exact starting commit. Production source is unchanged.
- Modified scope: `ShoppingJourneyTest.kt`, new `JourneyDiagnostics.kt` (androidTest only), `scripts/run_android_journey.sh`, `.github/workflows/android.yml`, the seven project documents, and this report. Local snapshots/logs remain ignored and are not included in publishing.
- First published diagnostic commit: `bb57f5ae8409661a2891da6a71a252772e99b12c`; [draft PR #3](https://github.com/brunkonjaa/Liz-s-Shopping-App/pull/3) targets `fix/ci-journey-state`. Main and the original checkout remain unchanged. Current implementation commit: `265b941b211a88085e238dfadc431cd1e7de87c7` (current controls and transient-message synchronization). The final evidence commit changes documentation only; the implementation remains at this tested source revision. The isolated worktree is committed/clean; the original checkout retains its original six modified files and local captures. Both main refs remain `27c6f4a88b2ce6db05db44107465aa636ffd12ad`.

## B. Root-cause investigation

### Original symptom and baseline

[GitHub run 38818122991](https://github.com/brunkonjaa/Liz-s-Shopping-App/actions/runs/38818122991) failed waiting 30 seconds for the ViewModel to contain Bread at quantity 1, immediately after the first catalogue Add. The workflow's build, unit/lint/APK and packaged-security checks passed. The retained artifact contains XML, runner reports and logcat, but no screenshot, semantics or application state at this timeout. The old screenshot capture belonged to `waitForText`, not `waitForBreadQuantity`.

The unchanged failed job was rerun during this investigation. Job `116719669324` failed at the same helper and call site; reports were uploaded successfully (artifact `12012717530`). This is a reproduced CI failure, not a successful retry. The unchanged application/test APK also passed the full journey on Samsung SM-A127F / Android 13 through direct AndroidJUnitRunner in 31.569 seconds (`OK (1 test)`).

The earlier phone comparison was inspected: both Gradle XML results show one test with zero failures/errors; saved SQLite snapshots from committed and scrolling variants contain Bread quantity 7, favourite default 1 and empty other shop lists. The final phone screenshot shows the empty home state. The historical local failure is a different `waitForText` timeout; it does not establish the current first-add mechanism or CI exclusivity.

### Fresh phone diagnostic observation

The passing diagnostic phone run recorded exactly one enabled, displayed Add with a click action, center `(285, 1154.5)` in a 720x1600 window at density 280 dpi, IME hidden, and a resumed focused activity. Immediately after dispatch the ViewModel had Bread 1 while committed readback still had no Bread; the next capture had durable Bread 1. The observer then recorded quantities through 7, favourite default 1, Aldi empty and the return to Lidl. These are observations of a passing phone run, not proof of the failing emulator mechanism.

An initial diagnostic run failed before Add because Android had not initialized its app-owned external output directory; this diagnostic defect was corrected with the platform directory API and non-masking setup/teardown. Runtime artifact inspection also found that collection-level semantics output defaulted to roots only; the helper now prints each root interaction with descendants.

### Confirmed original failure mechanism

[Diagnostic CI run 38823320223](https://github.com/brunkonjaa/Liz-s-Shopping-App/actions/runs/38823320223) reproduced the same first-add timeout and successfully uploaded [artifact 12014083518](https://github.com/brunkonjaa/Liz-s-Shopping-App/actions/runs/38823320223/artifacts/12014083518). The downloaded ZIP was inspected, including the actual PNG, accessibility XML, merged/unmerged semantics, state reports and runner/logcat evidence.

- Environment: API 33, x86_64, Pixel 2 profile, 1080x1920, 420 dpi (2.625 density), animations 0. Phone: 720x1600 at overridden 280 dpi (1.75 density). These have similar dp widths but different available dp heights.
- Immediately before Add, exactly one matching enabled button has an OnClick action. Its layout is `(310,1654)-(528,1759)`; its scroll ancestor viewport ends at `y=1584`. Therefore the entire button is outside the visible catalogue viewport.
- The node exists in semantics but `displayed=false`; its clipped `boundsInRoot` and `boundsInWindow` are both zero rectangles, with center `(0,0)`. Scroll range is nonzero and still at the start.
- `performClick` returns, but no Bread publication/message is observed. ViewModel and committed SQLite remain empty immediately afterward and at the 30-second failure. The screenshot independently confirms only the upper part of the Bread card is visible above the navigation bar.
- The keyboard is hidden; activity is RESUMED with window focus before/after/failure. No selector ambiguity exists. Waiting longer does not bring the button into view.

**Supported category: interaction did not reach the intended off-screen control.** The test assumed that a composed enabled node could receive a screen tap without scrolling. This is a test viewport assumption. It does not demonstrate a navigation, animation, ViewModel or SQLite defect. Missing events alone do not prove no persistence attempt; that is not needed to establish that the target is entirely clipped and the original tap cannot hit it.

The minimum correction is `performScrollTo().assertIsDisplayed().assertIsEnabled()` before the existing catalogue Add click, with a new ready-state capture proving the target has usable bounds. The subsequent experiment also established the need to synchronize with transient snackbar disappearance before the next interaction. Keep selectors, application APK, assertions, timeouts, keyboard handling and emulator settings unchanged. Corrected phone repetitions and the maintained current-app CI journey passed; see verification below.

API contract reference: [Compose test interaction API](https://developer.android.com/reference/kotlin/androidx/compose/ui/test/SemanticsNodeInteraction) documents scrolling and displayed checks; the diagnosis above comes from this run's artifacts.

### Second discriminating CI experiment: scroll alone

[Run 38824351866](https://github.com/brunkonjaa/Liz-s-Shopping-App/actions/runs/38824351866), correction commit `8e8fcf067d93ffceaacf227c2cb0dda714f9aaa9`, passed build/security and reproduced a **later** interaction failure. Its [artifact 12015055805](https://github.com/brunkonjaa/Liz-s-Shopping-App/actions/runs/38824351866/artifacts/12015055805) was downloaded and inspected:

- Scrolling changed the Add bounds from zero to `(310,1479)-(528,1584)`, displayed/enabled. First Add then published and durably saved Bread 1. This confirms the first correction addressed the original boundary.
- Second Add was dispatched at the same visible centre `(419,1531.5)`, but Bread remained 1 through the quantity-2 timeout. It is not the original quantity-1 failure.
- The actual ready/after screenshot and semantics show the first Add's Material snackbar covering that centre: snackbar surface `(32,1426)-(1048,1552)`, message `Bread added to Lidl`. It has Polite LiveRegion, Alert pane and Dismiss semantics. Displayed/enabled assertions alone do not establish that a control is unobstructed.
- The message cleared later, but a missed injected tap is not automatically retried. The 30-second quantity wait therefore still failed.

The maintained test now synchronizes **before** card interaction on disappearance of the current snackbar semantics (Polite LiveRegion plus Dismiss action). This is an observable UI condition, not a fixed sleep or an increased timeout. Production snackbar behaviour is unchanged. The final current-app revision passed the same CI emulator with snackbar synchronization; see verification below.

### Journey suitability after app changes

The workflow was added in `aed993cd866564f7e0b2f930c34176e5ffc94916` (2026-10-14 00:08 Dublin); Git attributes that commit to the configured Bruno Suric identity, which does not identify a human versus an agent writer. Both app and test are built from the same current checkout. The direct-add journey was rewritten in `b5497842da704e10f5dcd98e2708ef4c8b422ecd` on 15 October. Its predecessor scrolled editor controls; the new catalogue Add clicks omitted scrolling. This omission fits the demonstrated viewport failure.

The current outcome expectations were checked against project requirements, current Rules/ViewModel/UI code and fresh phone state, without weakening assertions:

| Current behaviour | Journey expectation | Evidence |
| --- | --- | --- |
| Catalogue Add adds one package | First Bread 1, second Bread 2 | Rules.defaultItem count 1; both phone publications/readback |
| Package count controls remain independent of size | 2 + five increments - one decrement = 6 | Current quantity actions; state-event sequence |
| Favourite defaults remain independent | Favourite 1, list 6 + 1 = 7 | Favourite Add and final SQLite snapshots |
| Completion can toggle both ways | Bought / still-to-buy controls swap | Existing UI assertions passed |
| Shops have independent lists | Aldi empty; Lidl retains Bread | Existing UI assertions and saved per-shop snapshots |
| Activity recreation restores selected shop/list | Lidl reopens with Bread 7 | Existing journey plus final state/UI captures |

This single journey is not full feature coverage. The rewritten flow does not cover the earlier editor/custom-item cases, backup/restore UI, custom-shop management, image selection, accessibility, large fonts, landscape or reboot/process-restart persistence. Unit storage/migration checks do not substitute for those UI journeys. They remain explicitly unverified rather than reasons to remove the valid current assertions.

### Follow-up instruction: align the journey with the current app

The user explicitly requested updating the journey after discussing UI changes. After separately validating the minimal scroll correction, the maintained journey now:

- Waits for loaded state and refuses existing list/favourite/previous/custom-product data instead of deleting it or assuming an empty fixture.
- Explicitly selects Lidl using current home/shop-sheet controls, including the current returning-user launch behaviour.
- Selects navigation items by their current Tab role and label, removing first-text-match assumptions.
- Uses displayed/enabled scroll preconditions for the current catalogue, favourite and list card controls; rapid increments still perform five actual taps.
- Replaces search text deterministically and preserves the real direct Add actions.
- Retains every existing business outcome assertion and adds explicit quantity 6/7, independent favourite default 1, and post-recreation quantity 7 checks.
- Reuses the same before/ready/after/timeout diagnostics for favourite Add and captures preparation failures too.

These are test-only updates grounded in current captured semantics and the user's follow-up. They were verified as a separate revision from the first CI scroll experiment; no production changes were made.

## C. Changes and justification

- At the catalogue Add control: retain the exact selector and click, add the evidenced scroll/displayed/enabled precondition, while recording unique match count, node ID, enabled/click action, clipped root/window bounds and centers, displayed state and ancestor vertical scroll range. Capture activity/lifecycle, window focus, IME and viewport without changing them.
- Around the interaction: record started/returned/thrown dispatch separately from ViewModel/message events and committed Bread state. Optional method tracing (`journeyMethodTrace=true`) records exact save/transaction entry and exit if further evidence is necessary; it is a separate experiment because tracing changes timing.
- At the actual quantity timeout: preserve PNG, accessibility XML, merged/unmerged semantics, target bounds, state/readback report, app-PID logcat and the original exception stack. Independent capture failures are logged without replacing the test exception. Original outcome assertions, test timeouts and application behaviour remain unchanged; the corrected interaction adds stronger visibility/enabled assertions.
- CI script: collect selected logcat tags while the emulator is alive, record configuration/IME/activity details, tee Gradle output, preserve its exit status, and collect remaining evidence in an EXIT trap. This avoids attempting to collect all evidence after the emulator action has stopped the emulator or Gradle has uninstalled the app.
- Workflow: retain the existing always-upload step, XML, Android test reports and Gradle additional output, and include the new host diagnostic directory. Build/security checks and emulator configuration remain unchanged. No dependencies added.
- Seven project documents describe the verified correction, diagnostic boundary, privacy constraints and remaining coverage limits.

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
| Diagnostic GitHub workflow and actual failure artifacts | Failed at original first Add as expected; full failure artifacts downloaded and inspected; build/security and always-upload passed |
| Corrected unit/lint/APK/test APK command | Passed in 56s; production unchanged |
| Corrected direct phone journey | 2/2 passed: 39.404s and 39.283s, each 1 test; both snapshots Bread 7, favourite 1, other shop lists empty |
| Scroll-only GitHub journey | First Add fixed and durably saved; second Add missed under snackbar; quantity-2 timeout. Build/security/upload passed |
| Current-app journey revision | Unit/lint/APK/test APK command passed in 50s. A temporary home/drawer selector ambiguity was caught locally and narrowed to the home card. Final direct Samsung runs passed 2/2: fresh-home fixture 69.295s; persisted-empty-Aldi fixture 68.745s. Both finish with list 7, favourite 1 and other shops empty |
| Existing-data guard probe | Passed: refused a non-empty test-created list with the expected clear precondition failure; SQLite bytes unchanged |
| Final current-app Gradle-connected Samsung | Passed: 1 test, 0 failures/errors; XML test duration 80.184s, command 1m 58s. Gradle pulled 78 diagnostic files with no capture errors |
| Final current-app GitHub | [Run 38825384833](https://github.com/brunkonjaa/Liz-s-Shopping-App/actions/runs/38825384833), commit 265b941: both jobs passed, including unit/lint/APK, packaged-security and API 33 Pixel 2 x86_64 journey. XML: 1 test, 0 failures/errors, 25.391s testcase duration. Artifact upload passed |
| Diagnostic Samsung through Gradle | Passed: 1 test, 0 failures/errors, 44.714s XML; Gradle pulled 43 diagnostic files before uninstalling. Reinstalled and restored every original DB/preferences file byte-for-byte, then launched |
| Local matching Android 13 emulator | Not available: only API 37 images installed |

Local APK SHA-256 before diagnostic changes: `3962A6F75694F648FE25EAFAB05C41B14E62B948F39ACBEF6D74302379AD8633`. The phone was snapshotted before testing; the original database had no saved state row. Each disposable run is restored from those original bytes, without `pm clear` or uninstalling the target app. Restore-only checks matched every original database/preferences file byte-for-byte and launched the app. After the final Gradle run, the app was reinstalled and launched; all original database/preferences files were verified byte-for-byte again after UI inspection. The final home UI has all four default shops and four zero-item counts. The final application APK is byte-identical to the starting APK (the same SHA-256 above).

### CI artifact inspection and acceptance mapping

[Successful artifact 12015495253](https://github.com/brunkonjaa/Liz-s-Shopping-App/actions/runs/38825384833/artifacts/12015495253) was downloaded and inspected. First Add has usable displayed/enabled bounds. Second Add's ready semantics contain no snackbar LiveRegion and its state report has `message=null`; the first quantity is durably 1. Final screenshot/accessibility XML visibly show Bread and quantity 7 after recreation; ViewModel and committed SQLite both report Bread 7, favourite default 1. Exit status is 0, XML records zero failures/errors, and no capture-errors report exists. The same graphics warnings occur in the passing run, with emulator settings unchanged.

| Acceptance criterion | Evidence/status |
| --- | --- |
| Supported failure mechanism | Confirmed by two failed CI experiments: completely clipped first target, then snackbar covering second target |
| Correction addresses the mechanism | Confirmed: scroll/displayed/enabled preconditions and snackbar disappearance synchronization; successful same-profile CI captures |
| Existing functionality preserved | Current journey 3/3 Samsung runs (two direct, one Gradle), plus CI; quantities, completion, favourite independence, shop isolation, recreation |
| Local checks pass | 50 fresh unit tests; lint 0 errors; debug and test APK builds; current connected journey |
| GitHub instrumentation/build/security pass | Both jobs green in run 38825384833 |
| Failure artifacts actually available | Failed-run ZIPs and successful ZIP downloaded/inspected; timeout stack, UI/state/logs retained; controlled local timeout probe passed |
| No production/data/unrelated changes | No production/manifest/dependency/schema diff; identical app APK; original root diff unchanged; phone DB/preferences identical after restoration; private artifacts ignored |

## E. Remaining risks

No current journey failure remains in the verified revision. Passing repetitions provide bounded evidence, not a guarantee against every future intermittent failure or configuration. The app still temporarily overlays bottom content with a snackbar on short viewports; test synchronization respects that existing behaviour rather than changing the UI. A future UI change may require updating the role/control/fixture contract again.

Only the CI host supplies the matching local-task emulator profile: no API 33 image was installed on this workstation, and no local API 37 emulator pass is claimed. Samsung density/viewport, keyboard, animations, ABI and graphics differ. The optional method trace changes scheduling; the initial trace overflowed and was discarded as causal evidence. Its narrowed follow-up did not overflow and observed addProduct plus LocalStore.save entry, while committed readback supplied write-success evidence.

This journey does not establish reboot/process-restart persistence, editor/custom-item UI, backup/restore UI, custom shops/images, TalkBack, large fonts, landscape or broad release readiness. The controlled timeout and existing-data probes verify the diagnostic/guard contracts; their expected failures are separate from the passing normal journey.

## F. Verdict

**RESOLVED:** Diagnostic CI evidence establishes both interaction obstructions. The current-interface journey corrects scrolling and observable snackbar synchronization, preserves and strengthens its outcome assertions, passes repeated Samsung execution and GitHub Android 13 instrumentation, and retains useful failure artifacts. Unit, lint, APK and packaged-security checks pass. Production code, application APK, user data and unrelated original work are preserved. This was the verified Task 5 verdict before the separately authorized consolidation/merge follow-up above.
