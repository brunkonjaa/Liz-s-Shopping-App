# Task 9 — Controlled Performance Investigation

Date: 2026-10-16. Scope: reproduce catalogue scrolling on the existing Samsung target and assess whether current evidence justifies production changes. No application, test, Gradle, schema, dependency, permission, or device accessibility setting was changed. Profiling artifacts are local and ignored under `verification/task9-performance-optimisation/`.

This report preserves the Task 9 debug-build investigation. Task 11 later measured the Release build, and Task 12 records the current visual implementation. Those later results do not make the Task 8/9 profiling runs a matched comparison; see [TESTING.md](TESTING.md) for current checks.

## A. Repository baseline

- Starting branch: `main`; starting commit: `9315e7f81beb6843eb9aa4e5c0b85df8c8ce6c09` (the Task 8 reference baseline).
- A Task 9 branch, `task9/performance-optimisation`, was created before this report was added.
- Pre-existing working-tree changes were preserved: modified `README.md` and `CHANGELOG.md`, plus untracked `docs/TASK8_PERFORMANCE_AUDIT.md`. These Task 8 files were not replaced.
- Read the project instructions and all seven current project documents, `docs/TASK8_PERFORMANCE_AUDIT.md`, and historical `docs/TASK7_INTERACTION_AND_PERFORMANCE_REPORT.md`.
- Reviewed the local performance/animation guidance and approved visual-style, bottom-sheet and product-catalogue interaction specifications; the ignored reference material remains local.
- Inspected `MainActivity.kt`, `Components.kt`, `ShoppingViewModel.kt`, `Model.kt`, `Catalogue.kt`, the existing task reports, and the available device/profiling tools.

## B. Controlled reproduction

### Environment

- Samsung SM-A127F, Android 13/API 33, Exynos 850, eight online CPU cores, 720×1600, 60 Hz, 300 dpi physical / 280 dpi override.
- Existing installed debug APK; local debug APK SHA-256: `3962A6F75694F648FE25EAFAB05C41B14E62B948F39ACBEF6D74302379AD8633`. Package `ie.suric.lizshopping`, version `0.1.0`, target SDK 36.
- Accessibility remained enabled (`accessibility_enabled=1`); the enabled third-party service identity is omitted. No service or animation setting was changed. Window, transition and animator scales were each 0.5.
- Thermal status was 0 before/after. Post-run AP temperature was 34.2°C, battery 33.6°C; battery level 62%. RAM was 3,792,268 KiB total and 1,334,392 KiB available after capture.
- Catalogue contained 72 built-in products. The selected SuperValu list showed 0 items at preflight. Scrolling changed no list, favourite, quantity or shop data. The app was returned to the Shopping List tab after capture.
- Workload: warm the app, reset `dumpsys gfxinfo`, then repeat a consistent 420 ms up/down swipe pair inside the catalogue for 60 seconds per trial. Each run recorded `dumpsys gfxinfo` and framestats. Five runs took 60.4–61.1 seconds and included 46–53 swipe pairs.
- A separate 30-second Simpleperf CPU sample and a separate 30-second Perfetto FrameTimeline capture used the same catalogue scroll flow. Artifacts and exact Perfetto config are in the ignored local directory named above.
- Artifact files: `scroll-trial-1.txt` through `scroll-trial-5.txt`, `framestats-trial-1.txt` through `framestats-trial-5.txt`, `catalogue.pftrace`, `perf.data`, `simpleperf-report.txt`, and `perfetto-config.pbtxt`.

Commands used (serial shortened here only in the report):

```text
adb -s R58R93QQ3GZ shell dumpsys gfxinfo ie.suric.lizshopping reset
adb -s R58R93QQ3GZ shell input swipe 360 1190 360 500 420
adb -s R58R93QQ3GZ shell input swipe 360 500 360 1190 420
adb -s R58R93QQ3GZ shell dumpsys gfxinfo ie.suric.lizshopping
adb -s R58R93QQ3GZ shell dumpsys gfxinfo ie.suric.lizshopping framestats
adb -s R58R93QQ3GZ shell simpleperf record --app ie.suric.lizshopping -o /data/local/tmp/task9-perf.data -e cpu-clock -f 4000 -g --duration 30
```

Perfetto used the local-only `perfetto-config.pbtxt` in the ignored artifact folder, with `android.surfaceflinger.frametimeline`, `sched/sched_switch`, `sched/sched_waking`, and `gfx`, `view`, `wm` atrace categories. Capture duration was 30 seconds. No app or test instrumentation was added.

### Five GFXInfo runs

| Run | Frames | Janky | Jank rate | P50 | P90 | P95 | P99 | Slow UI thread | Slow draw commands |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 789 | 738 | 93.54% | 85 ms | 150 ms | 200 ms | 300 ms | 729 | 4 |
| 2 | 1,303 | 1,165 | 89.41% | 46 ms | 93 ms | 117 ms | 150 ms | 1,146 | 0 |
| 3 | 1,506 | 1,253 | 83.20% | 42 ms | 81 ms | 93 ms | 129 ms | 1,222 | 0 |
| 4 | 1,563 | 1,246 | 79.72% | 40 ms | 77 ms | 93 ms | 125 ms | 1,202 | 0 |
| 5 | 1,655 | 1,230 | 74.32% | 38 ms | 73 ms | 81 ms | 113 ms | 1,186 | 2 |
| Combined | 6,816 | 5,632 | 82.63% | — | — | — | — | 5,485 | 6 |

GFXInfo's combined jank percentage is the weighted count across these five trials. Per-run percentile summaries are retained per trial; they are not merged into one percentile distribution. The pronounced warm-up/run-order trend is material and was not attributed to a code change.

### Separate Perfetto FrameTimeline capture

Trace Processor v58.2 found 593 app-layer frames: 392 classified `App Deadline Missed` (66.1%) and 490 with `Buffer Stuffing` (classifications can overlap). App-layer frame durations were P50 29.19 ms, P90 37.51 ms, P95 42.25 ms, P99 55.44 ms, maximum 100.75 ms. These FrameTimeline classifications are not interchangeable with GFXInfo's janky-frame count. Trace Processor reported seven negative-timestamp events dropped; treat this capture as useful but imperfect.

### CPU attribution

Simpleperf sampled for 29.94 seconds with 72,359 samples and zero lost samples; it reported 49,798 cut callchains, so stack percentages are indicative rather than definitive. The app main thread accounted for the listed Compose paths: inclusive `getCurrentSemanticsNodes` 36.56%, `scheduleScrollEventIfNeeded` 23.18%, `setTraversalValues` 17.93%, and `geometryDepthFirstSearch` 15.29%. RenderThread's top thread-loop entry was 9.21%. This is sampled CPU attribution under the enabled service, not wall time or proof that the service caused the jank.

GFXInfo listed 729/789 slow-UI-thread frames in the first trial and only four slow draw-command frames; its GPU P50 was 4 ms for that run. This aligns the observed work with UI-thread processing in this setup. It does not isolate Compose, accessibility-service work, or their interaction as the sole cause.

### Baseline comparability and limitations

Task 8 recorded 896/945 janky frames (94.8%, per-run 90.69–98.80%) on this phone, APK and enabled-service setup using eight up/down pairs per trial. Task 9 used continuous 60-second trials with 46–53 pairs. The runs reproduce the severe symptom, but the workload differs; the numbers are **not** a matched before/after comparison. The declining rate during the five current runs also shows why run order and warm-up must be controlled.

No Android 13 disposable emulator/device was available. The only local AVDs are API 37; prior project investigation found unresolved high host CPU there. The household phone's accessibility service was not disabled, changed, or identified in the report. Thus the service-off comparison and attribution remain unverified. No add/quantity/favourite/completion/shop-switch input latency or 10/50/100-item synthetic-list profile was performed.

Accessibility-node-count/order effects, cold-start timing, search responsiveness, catalogue switching, favourites/list scrolling and sheet transitions were not measured in Task 9.

## C. Root-cause assessment

| Suspected issue | Assessment | Evidence and limit |
| --- | --- | --- |
| Severe catalogue jank in the current phone/service setup | **Confirmed** | Five fresh GFXInfo trials; separate FrameTimeline trace. Not generalized to service-off environments. |
| Compose accessibility traversal consumes main-thread CPU during catalogue scrolling | **Strongly supported** | Current Simpleperf sample again shows traversal functions; same service was enabled throughout. Cut callchains limit precision. |
| Accessibility service is the cause or necessary trigger | **Unverified** | No matched service-off target was available; Task 8 and Task 9 both kept it enabled. |
| UI-thread work is a larger observed contributor than GPU draw submission in these captures | **Strongly supported** | Slow-UI counts are high, slow-draw counts low, GPU P50 4 ms in GFXInfo; FrameTimeline and CPU sample also show app UI work. No release-like or cross-device profile. |
| Mutex-held snapshot persistence delays successive visible changes | **Unverified** | Source serializes writes while holding the mutex, but publishes each optimistic state before IO. No input-latency or slow-write test was run. |
| Filtering/sorting/deduplication at 72 products is a material scroll bottleneck | **Unverified** | Transformations are visible in source; no composition timing or 10/50/100-item controlled profile. |
| Bitmap decode/render is a scroll bottleneck | **Not observed in these captures** | Catalogue uses emoji thumbnails; GFXInfo reported no slow bitmap uploads in the captured summaries. |

## D. Implementation decision

No production-code optimization is justified by the available controlled evidence. Task 8's service confounder remains unresolved, and Task 9 reproduces the workload only with that same service enabled. Removing or merging semantics now risks losing distinct Add, Configure and Favourite actions without establishing the expected benefit. Persistence and list-transformation changes are also unsupported by user-latency measurements.

Therefore Task 9 changes are limited to this report and factual README/CHANGELOG references. No source, test, dependency, permission, database schema, visual design, or accessibility behavior changed. This is the diagnostic-only completion path allowed by the task brief; it does not claim an optimization or performance improvement.

## E. Comparative results and acceptance targets

| Measure | Task 8 reference | Task 9 reproduction | Comparison boundary |
| --- | --- | --- | --- |
| GFXInfo scroll | 896/945 janky (94.8%); eight up/down pairs per trial | 5,632/6,816 janky (82.63%); continuous 60-second trials, 46–53 pairs | Different workload and run-order/warm-up profile; not a before/after result. |
| FrameTimeline | 219/304 deadline misses (72.0%); 24 swipes then idle | 392/593 deadline misses (66.1%); continuous 30-second scroll | Both are 30-second captures but gesture schedules differ; not a matched effect estimate. |
| Service/build/device | SM-A127F, Android 13, debug APK, service enabled | Same phone, Android 13, APK hash and service state | Matched environment, but not matched workload. |
| Production change | None in Task 8 | None in Task 9 | No optimized implementation exists to compare. |

There is no valid optimized before/after pair because no production change was made and the Task 8 scroll workload differs from the Task 9 reproduction. The current Task 9 82.63% GFXInfo rate and 66.1% FrameTimeline deadline-miss rate do not meet either target (halve matched-condition jank; at most 5% missed-deadline frames). They are diagnostic results, not target evaluations under matched conditions.

## F. Functional, accessibility, persistence and security verification

| Check | Result | Evidence / boundary |
| --- | --- | --- |
| Unit tests | **PASS** | `testDebugUnitTest --rerun-tasks --console=plain`: 50 tests (34 RulesTest, 16 StorageTest), zero failures/errors; all 28 actionable tasks executed. |
| Lint | **PASS** | `testDebugUnitTest lintDebug assembleDebug --console=plain`: `BUILD SUCCESSFUL`; lint reported 0 errors and 13 warnings. Lint task was up-to-date. |
| Debug APK assembly | **PASS, UP-TO-DATE** | Same combined command succeeded; `assembleDebug` was up-to-date, so no fresh APK was produced. Existing APK SHA-256 remains `3962A6F75694F648FE25EAFAB05C41B14E62B948F39ACBEF6D74302379AD8633`. |
| Android 13 instrumentation journey | **NOT VERIFIED** | No disposable Android 13 installation was available. The household phone was not subjected to potentially destructive instrumentation. |
| TalkBack labels, independent actions and focus traversal | **NOT VERIFIED** | No accessibility service setting was changed and no manual TalkBack inspection was performed. This work made no semantics changes. |
| Persistence/readback and rollback under injected write failure | **NOT VERIFIED** | No write/failure workload was exercised; no household data was modified. |
| Packaged security/permission audit | **NOT RE-RUN** | No manifest, dependency, signing, or app source changed in Task 9. Existing security evidence remains in `TESTING.md`; this is not a new Task 9 security pass. |
| Data/schema/backup compatibility | **PRESERVED BY SCOPE** | No data, schema, backup code, or migration was changed. No migration test was needed for this documentation-only task. |

## G. Outstanding issues

- Run a matched enabled/disabled accessibility-service comparison on a disposable Android 13 target. Keep device, app build, catalogue, animation scales, gestures, thermal state and capture tools constant; use at least five alternating runs per condition.
- Capture the same 60-second workload in both conditions with GFXInfo, framestats and Perfetto FrameTimeline. Repeat Simpleperf separately and report complete/cut/lost samples.
- Only if the service-off condition still reproduces the hotspot, prototype the smallest semantics-node reduction while retaining independently labeled Add, Configure and Favourite controls, then repeat the matched comparison and accessibility checks.
- Measure state-publication latency separately from durable-write completion under a deliberately slow disposable store before considering ViewModel/write-queue changes. Include failure rollback and independent committed SQLite readback.
- Profile 10/50/100-item synthetic lists on a disposable target before caching or moving catalogue transformations.
- Re-run TalkBack/focus checks and the repository's complete Android 13 journey on a disposable installation before any performance implementation is described as verified.

## H. Task 10 handover

Use `task9/performance-optimisation` as the investigation branch. The exact local baseline captures are under ignored `verification/task9-performance-optimisation/`; do not commit or upload those traces, screenshots, UI dumps or device-specific files. The Task 8 reference captures remain under ignored `verification/task8-performance-audit/`.

For an independent matched comparison, use a disposable Android 13 device/emulator and this exact debug APK/source commit. Record APK SHA-256, service state, animation scales, display, RAM, thermal state, catalogue contents and run order. Warm up first, then run five 60-second scroll trials per condition, alternating service-on and service-off order; do not touch the household phone's service setting. For every run save GFXInfo summary and framestats. In separate runs capture Perfetto FrameTimeline and Simpleperf. Keep GFXInfo jank and FrameTimeline deadline-miss definitions separate. Report per-run values and warm-up/order effects; do not pool incompatible workloads.

If no disposable API 33 target can be provisioned, keep the service effect marked unverified and do not change production semantics based only on the current correlation. Preserve existing shopping data and use the guarded Android journey only on an empty disposable installation.
