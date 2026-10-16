# Task 10 — Controlled Performance Isolation and Remediation

Date: 2026-10-16. Scope: inspect the Task 8/9 evidence, check for a safe disposable comparison target, review the catalogue rendering path and record the strongest supported conclusion. No production or test code, device settings, household data, schema, dependency or permissions were changed. Profiling artifacts remain local under the ignored `verification/` directory.

Task 10's service comparison remained unverified because no disposable Android 13 profiling target was available. Task 11 later recorded Release measurements; Task 12 added visual and storage changes and has its own current test results in [TESTING.md](../TESTING.md). Neither report changes the historical conditions recorded below.

## 1. Repository baseline

- Task 10 branch: `task10/performance-isolation-remediation`.
- Application source baseline: `9315e7f81beb6843eb9aa4e5c0b85df8c8ce6c09` (Task 8/9 reference source).
- The carried Task 8 and Task 9 reports and status references were preserved in baseline commit `6d071e3f2f18a744816cfc4307356c8a52465dec`. Task 10 changes are documentation only.
- The Task 8/9 reports, seven current project documents, Task 7 history and local performance, visual, catalogue and sheet guidelines were reviewed. Local references and captures were not staged as project artifacts.

## 2. Testing environment and data protection

- `adb devices -l` showed one connected target: the household Samsung SM-A127F on Android 13/API 33. It is not a disposable installation. Its accessibility settings and app data were not touched during Task 10.
- The installed local AVDs and SDK system images are API 37 only. No API 33 system image is installed, and no second disposable Android 13 device is available. The API 37 emulator was not substituted: earlier local investigation recorded unresolved high host CPU there, and it cannot provide the required Android 13/service comparison.
- Task 8/9 therefore remain the available on-device evidence. Their captures are ignored local artifacts; no new traces or screenshots were uploaded or committed.
- The existing GitHub API 33 emulator journey is functional instrumentation, not a profiling target or sustained scrolling benchmark. No workflow was added or started.

## 3. Accessibility-service comparison

**UNVERIFIED:** No matched enabled/disabled service comparison could be run on a disposable Android 13 target. The Task 8/9 phone runs kept the same third-party accessibility service enabled; the service identity is intentionally omitted. No accessibility setting was changed on the household phone.

As a result, the available evidence cannot establish whether that service is necessary for the observed traversal cost, amplifies it, or is incidental. It also cannot establish the performance of this flow without the service. TalkBack action discovery and focus traversal were **NOT TESTED** in Task 10.

## 4. Profiling methodology and existing measurements

Task 9 used the same phone, debug APK hash, 72-product catalogue and enabled-service setup recorded in Task 8. It warmed the app and ran five 60-second continuous scroll trials with 46–53 up/down swipe pairs each. GFXInfo/framestats were captured per run. Separate 30-second scroll captures supplied Perfetto FrameTimeline and Simpleperf evidence. These are historical Task 9 captures, reviewed for this report; no new benchmark was run.

| Task 9 GFXInfo run | Frames | Janky | Jank rate | P50 | P90 | P95 | P99 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 789 | 738 | 93.54% | 85 ms | 150 ms | 200 ms | 300 ms |
| 2 | 1,303 | 1,165 | 89.41% | 46 ms | 93 ms | 117 ms | 150 ms |
| 3 | 1,506 | 1,253 | 83.20% | 42 ms | 81 ms | 93 ms | 129 ms |
| 4 | 1,563 | 1,246 | 79.72% | 40 ms | 77 ms | 93 ms | 125 ms |
| 5 | 1,655 | 1,230 | 74.32% | 38 ms | 73 ms | 81 ms | 113 ms |
| Combined | 6,816 | 5,632 | 82.63% | — | — | — | — |

The declining jank rate across the ordered runs is a material warm-up/order effect. Percentiles are per-run GFXInfo histogram values; the combined row is a weighted jank count, not a pooled frame-time distribution.

The separate Task 9 Perfetto capture classified 392/593 app frames (66.1%) as `App Deadline Missed`; its frame-time P50/P90/P95/P99 were 29.19/37.51/42.25/55.44 ms and maximum was 100.75 ms. GFXInfo jank and Perfetto deadline classifications use different definitions and must remain separate. Trace Processor reported seven negative-timestamp events dropped.

The separate 29.94-second Simpleperf sample contained 72,359 samples, zero lost samples and 49,798 cut callchains. Inclusive app-thread samples included `getCurrentSemanticsNodes` (36.56%), `scheduleScrollEventIfNeeded` (23.18%), `setTraversalValues` (17.93%) and `geometryDepthFirstSearch` (15.29%). This is sampled CPU attribution in a debuggable build, not wall-time measurement or proof that the service causes jank; the cut callchains limit precision.

Task 8's 94.8% GFXInfo jank and 72.0% Perfetto deadline misses used different gesture schedules. Task 8 and Task 9 are separate symptom reproductions, **not** a matched before/after pair. The Task 9 results exceed the proposed 5% missed-deadline target, but no optimized implementation exists and the target is not evaluated under matched service conditions.

## 5. Catalogue implementation review

- The catalogue uses a `LazyColumn` and stable product IDs as lazy item keys. No `contentType` is specified; with this single product-row type and 72 products, the source alone does not show that adding content types would materially improve frame pacing.
- `ProductCardV2` keeps Add, Configure and Favourite as separate labeled controls. Removing or merging semantics would risk discoverability and independent operation.
- Filtering and the per-product favourite lookup are performed while composing the catalogue page. Their cost at the current catalogue size was not measured, and the trace does not attribute a material portion of jank to them. Caching or moving those computations is therefore **UNVERIFIED** as a benefit.
- The sampled Compose accessibility traversal path is **STRONGLY SUPPORTED** as a main-thread hotspot in the enabled-service setup. The specific cause and its share of total frame delay remain **UNVERIFIED**.
- No Task 10 evidence implicated SQLite persistence, image decoding, GPU draw submission, or a specific recomposition boundary. These were not independently benchmarked here.

## 6. Production changes and performance comparison

No production optimization was justified or implemented. No service-off measurement exists, and the Task 8/9 workloads have different gesture schedules and run-order behavior. A speculative semantics reduction could impair accessibility without a demonstrated gain; caching/filter changes have no measured latency case.

| Metric | Baseline | Optimized | Change |
| --- | --- | --- | --- |
| GFXInfo jank | Task 9: 5,632/6,816 (82.63%) | Not measured | No optimized build |
| Perfetto deadline misses | Task 9: 392/593 (66.1%) | Not measured | No optimized build |
| Frame-time P50/P90/P95/P99 | Task 9: 29.19/37.51/42.25/55.44 ms | Not measured | No matched pair |
| Relative jank reduction target | At least 50% under matched conditions | Not evaluated | Service comparison unavailable |
| Deadline-miss target | At most 5% under matched conditions | Not evaluated | Service comparison unavailable |

## 7. Accessibility and functional regression verification

No UI semantics or interaction code changed, so Task 10 introduces no intended accessibility behavior change. Add, Configure, Favourite, quantity, completion and sheet controls were not exercised with TalkBack in this task; accessibility behavior remains **NOT VERIFIED** here.

No Task 10 tests were run because there was no production change. Task 9's earlier evidence remains historical and was not rerun: 50 unit tests passed; lint passed with zero errors and 13 warnings; debug APK assembly was reported up-to-date, so that command did not freshly produce an APK. A disposable Android 13 instrumentation journey was **NOT VERIFIED**. See [Task 9](TASK9_PERFORMANCE_OPTIMISATION_REPORT.md) and [TESTING.md](../TESTING.md) for scope and limits. No regression is claimed from unrun checks.

## 8. Security, persistence and design

No manifest, permission, dependency, persistence, backup, schema or UI file changed. Offline operation, stored user data, the existing visual interface and accessibility semantics were left untouched. Task 10 did not rerun security or persistence tests.

## 9. Status summary

- **CONFIRMED:** Severe catalogue-scroll jank recurs on the tested SM-A127F/debug-build/enabled-service setup in Task 8 and Task 9.
- **STRONGLY SUPPORTED:** Compose accessibility semantics traversal consumes sampled main-thread CPU in that setup.
- **UNVERIFIED:** Whether the service is causal or required; the same workload without the service; release-like behavior; a measured benefit from list or semantics changes; optimized performance targets.
- **NOT TESTED:** TalkBack/focus behavior in Task 10, API 33 disposable instrumentation, and fresh unit/lint/build/journey checks.
- **RESOLVED:** Task 10 reviewed the strongest available safe evidence, documented the target limitation and made no unsupported production change. The underlying performance issue is **not resolved**.

## 10. Limitations and next action

The missing evidence is a matched API 33 disposable run with identical APK, catalogue, display, animation scales, gestures, warm-up and thermal conditions, alternating service-enabled and service-disabled trials. The household device cannot safely supply the disabled condition, and the current local API 37 AVDs are not a comparable substitute.

**Recommendation: continue targeted Jetpack Compose optimization after a disposable Android 13 comparison target is available.** Run at least five alternating 60-second trials per condition with GFXInfo/framestats and Perfetto FrameTimeline; collect a separate Simpleperf profile. If the traversal hotspot persists with the service off, test one minimal semantics-node change while preserving all independently labeled actions, then remeasure and verify TalkBack on the disposable target. Do not infer that another rendering architecture is necessary from the current evidence.

## 11. Git and artifacts

- Branch: `task10/performance-isolation-remediation`.
- Application source baseline: `9315e7f81beb6843eb9aa4e5c0b85df8c8ce6c09`.
- Task 8/9 documentation baseline commit: `6d071e3f2f18a744816cfc4307356c8a52465dec`.
- Task 10 report and status-link commit: the focused documentation commit adding this report and its README/CHANGELOG references.
- Working-tree status: clean after the Task 10 documentation commit.
- Profiling traces, CPU samples, screenshots, UI dumps and database snapshots remain ignored under `verification/` and were not committed.
