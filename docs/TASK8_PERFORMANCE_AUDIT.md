# Task 8 — UI Performance Audit and Jank Diagnosis

Audit date: 2026-10-16. Scope: read-only application investigation and local performance captures. No production source, test, Gradle, database, or device accessibility settings were changed.

This audit captures the debug build and device conditions from its date. Task 11 later recorded a separate Release performance run, and Task 12 records the current visual changes and checks. See [TESTING.md](TESTING.md) and the [Task 12 report](TASK12_VISUAL_REFINEMENT_REPORT.md) for current status; the measurements below remain historical.

## A. Repository and environment

- Repository branch: `main`, tracking `origin/main`.
- Audited source commit: `9315e7f81beb6843eb9aa4e5c0b85df8c8ce6c09`.
- Working tree at audit start: clean. The installed APK's SHA-256 matched the local debug APK (`3962A6F75694F648FE25EAFAB05C41B14E62B948F39ACBEF6D74302379AD8633`). `app/src/main` at the audited commit matches the application source at `265b941`.
- Device: Samsung SM-A127F, Android 13/API 33, Exynos 850 (`universal3830`), eight CPU cores, 720×1600 display, 60.000 Hz (16.67 ms nominal interval), reported density 300 dpi with 280 dpi override. Thermal status was 0/none; reported AP temperature was 31.3°C before capture and 32.9°C after. System RAM was 3,792,268 KiB; free RAM was 1.5–1.7 GiB in the two snapshots.
- App variant: installed debuggable `debug` APK, package `ie.suric.lizshopping`. It is not a release-like performance build.
- A third-party accessibility service was enabled for all device captures. Its identity is omitted here for privacy. It was not toggled. This is a material environment confounder for the Compose accessibility-tree CPU path below.
- Tools: `adb`, `dumpsys gfxinfo`/`framestats`, Android Perfetto with FrameTimeline, and Simpleperf CPU sampling. Perfetto Trace Processor v58.2 analyzed the local trace. No JankStats dependency or production instrumentation was added.
- The GitHub Actions Android 13 journey uses an API 33 x86_64 Pixel 2 emulator and `disable-animations: true`. Its workflow runs a functional journey, not a sustained performance benchmark; no new remote workflow was started.
- The local AVDs available on the host were API 37, so they were not used in place of the requested Android 13 target.

Private profiling artifacts are in the ignored local directory `verification/task8-performance-audit/`. They include the installed APK copy, UI hierarchy captures, startup samples, `gfxinfo`, Perfetto traces, frame rows, and Simpleperf data/reports. Do not upload this folder.

## B. Executive summary

Catalogue scrolling showed severe and repeatable frame jank on the connected SM-A127F in its current setup. Five repeated `gfxinfo` runs recorded 896 janky frames out of 945 (94.8%); per-run jank ranged from 90.69% to 98.80%. A focused Perfetto capture also recorded app-frame deadline misses and buffer stuffing. Simpleperf placed 87.56% of the app's sampled CPU cycles on its main thread and identified Compose accessibility traversal and geometry sorting as a large inclusive call-graph path during scrolling.

This establishes a real performance problem under the measured conditions. It does not establish the same severity for users without the enabled accessibility service, or prove that changing app semantics alone will fix it. The first Task 9 step should be a paired, matched reproduction that separates the accessibility-service effect from the app's normal scroll cost while preserving accessible controls.

Five process-cold `am start -W` samples ranged from 1,717 to 1,844 ms (median 1,745 ms). This is a fresh current measurement, kept separate from Task 7's historical samples.

## C. Performance measurements

### Startup

Each trial force-stopped the app and ran `am start -W`; this is a process-cold app launch, not a device reboot. The first launch and four repeated launches returned `LaunchState: COLD`.

| Trial | TotalTime |
| --- | ---: |
| 1 | 1,844 ms |
| 2 | 1,735 ms |
| 3 | 1,745 ms |
| 4 | 1,717 ms |
| 5 | 1,750 ms |
| Range / median | 1,717–1,844 ms / 1,745 ms |

### Catalogue scrolling

The catalogue was opened without changing saved data. Each repeat reset `gfxinfo`, then performed eight up/down swipe pairs inside the catalogue's scrollable bounds. No products were added, removed, favourited, or edited.

| Trial | Frames | Janky | Janky % | P50 | P90 | P95 | P99 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 166 | 164 | 98.80% | 69 ms | 125 ms | 150 ms | 150 ms |
| 2 | 186 | 177 | 95.16% | 57 ms | 109 ms | 125 ms | 150 ms |
| 3 | 191 | 186 | 97.38% | 65 ms | 105 ms | 121 ms | 150 ms |
| 4 | 198 | 184 | 92.93% | 57 ms | 105 ms | 125 ms | 150 ms |
| 5 | 204 | 185 | 90.69% | 53 ms | 101 ms | 109 ms | 150 ms |
| Combined | 945 | 896 | 94.8% | — | — | — | — |

The reported `gfxinfo` P50/P90/P95/P99 values are Android's frame-duration histogram summaries; P99 is capped at the 150 ms histogram bucket in these samples. At 60 Hz, even the best run's P50 of 53 ms is well above the nominal 16.67 ms interval.

A separate 30-second Perfetto run captured 24 catalogue swipes followed by idle time. FrameTimeline contained 304 app-layer frames: 219 (72.0%) were classified with `App Deadline Missed`, 258 (84.9%) had a jank classification including `Buffer Stuffing`, and two were classified `None`. Frame duration from the captured app-frame rows was P50 33.0 ms, P90 50.7 ms, P95 64.2 ms, P99 71.7 ms, maximum 141.5 ms. `gfxinfo` for that capture recorded 306 frames, 279 janky (91.18%), 274 slow UI-thread frames, zero slow draw-command frames and zero slow bitmap uploads. These FrameTimeline classifications and `gfxinfo` counters use different definitions and are reported separately.

Simpleperf sampled the debuggable app for 25 seconds during repeated catalogue scrolling: 5,623 CPU-cycle samples, zero lost samples. The main thread (TID 26970) accounted for 87.56% of the app's sampled CPU cycles; RenderThread accounted for 6.69%. The main-thread inclusive call graph attributed 35.61% to `AndroidComposeViewAccessibilityDelegateCompat.setTraversalValues`, which descends into `subtreeSortedByGeometryGrouping` and `geometryDepthFirstSearch`. This is a sampled CPU profile, not an exclusive wall-time measurement. It was collected with the accessibility service noted above enabled.

### Other interactions and images

Search input, drawer, and sheet transitions were touched during an initial mixed-flow trace, but no valid per-interaction latency or animation duration was isolated. Add, quantity, completion, favourite, shop-switch, and editor response times were not measured because this audit preserved household data. The CI journey disables animations and cannot establish animation pacing.

The catalogue and list rows display text/emoji thumbnails rather than decoding product images. Imported image copying and bounds validation are called from `Dispatchers.IO`; the current scroll sample recorded no slow bitmap uploads. The active hero uses `grocery_hero_small.webp` (65,786 bytes). `grocery_hero.png` (2,160,275 bytes) has no source reference. Neither asset's file size establishes a runtime jank cause.

Task 7's startup figures remain historical and are not treated as this audit's measurements. No current shopping-list workloads of 10, 50, or 100 items were created or tested.

## D. Confirmed bottlenecks

### D1. Severe catalogue-scroll frame jank under the measured setup — P1

- **Status:** Confirmed symptom on the connected device and installed debug APK; environment-specific attribution remains open.
- **Source/surface:** Catalogue `LazyColumn` and `ProductCardV2` in [MainActivity.kt](../app/src/main/java/ie/suric/lizshopping/MainActivity.kt) and [Components.kt](../app/src/main/java/ie/suric/lizshopping/Components.kt).
- **Reproduction:** Open All Items, reset `dumpsys gfxinfo`, perform eight up/down swipe pairs, repeat five times. A separate Perfetto run performed twelve pairs during a 30-second trace.
- **Evidence:** Five runs: 896/945 `gfxinfo` janky frames; 90.69–98.80% per run. Perfetto: 219/304 app frames classified with an app deadline miss, max frame duration 141.5 ms. `gfxinfo` marked 274/306 frames as slow on the UI thread while reporting zero slow draw-command frames.
- **Impact:** The catalogue visibly misses 60 Hz frame deadlines and scroll movement cannot be considered smooth in this setup.
- **Explanation:** The delay is concentrated on the app UI thread rather than GPU draw-command submission. A concurrent third-party accessibility service was enabled, so this result must not be generalized to service-disabled devices without a paired run.

### D2. Compose accessibility geometry traversal is a sampled main-thread hotspot — P1 investigation target

- **Status:** Confirmed sampled hot path; causal contribution to the full jank rate is not yet isolated.
- **Source/surface:** Product-card semantics in [Components.kt](../app/src/main/java/ie/suric/lizshopping/Components.kt), rendered as keyed lazy items by [MainActivity.kt](../app/src/main/java/ie/suric/lizshopping/MainActivity.kt).
- **Evidence:** Simpleperf's 25-second scroll profile placed 87.56% of app CPU samples on the main thread and showed an inclusive call path through Compose's `setTraversalValues`, geometry grouping and depth-first semantics traversal. The same run's device had an accessibility service enabled.
- **Impact:** The path consumes substantial sampled main-thread CPU during catalogue scroll; app-frame deadlines are missed in the same workload.
- **Technical explanation:** Compose computes accessibility traversal order from semantics-node geometry. The profile does not determine whether the active service, the catalogue's semantics-node count, Compose runtime behavior, or their interaction is the dominant trigger. Do not remove or merge distinct Add, Configure, Favourite, quantity, or completion actions as a shortcut.

No confirmed P0 freeze, database bottleneck, bitmap decode bottleneck, or render-thread bottleneck was observed.

## E. Likely bottlenecks

1. **Serialized update publication can wait behind a save — P2.** [ShoppingViewModel.kt](../app/src/main/java/ie/suric/lizshopping/ShoppingViewModel.kt) launches each update on `viewModelScope`, takes a mutex, computes and publishes the change, then keeps the mutex while `LocalStore.save` runs on IO. A following tap waits for the prior full-snapshot write before computing/publishing its own optimistic state. This is source-supported, but no add/quantity burst or save-failure latency was measured.
2. **Small list transformations repeat on screen recomposition — P3 pending measurement.** `AllItemsPage` rebuilds/deduplicates/filters products; `ShoppingListPage` sorts items; `FavouritesPage` filters favourites. The lists are lazy and use stable item IDs. Their expected data sizes are modest, so repeated calculation is not established as a bottleneck.
3. **Debug-build interpretation affects absolute CPU evidence.** The installed artifact is debuggable. A release-like build may have different ART behavior; this audit does not supply a release comparison.

## F. Unverified areas

- Paired catalogue profile with the enabled accessibility service absent, on another device, or in a clean API 33 emulator. The service was not changed on the user's phone.
- Sustained catalogue scroll beyond the controlled swipe sequence; category filtering under load; and lists/favourites/drawer/editor/bottom-sheet scrolling.
- Empty and 10/50/100-item shopping lists. Synthetic data was not written to the household database.
- Add, quantity, completion, favourite and shop-switch input-to-visible latency; editor open usability; search latency; and bottom-sheet open, drag, settle, keyboard and dismissal timing.
- Compose recomposition counts and individual composition/layout durations. The captured Perfetto configuration included FrameTimeline and scheduler events, but not Compose runtime trace markers.
- GC pause attribution, imported-image render/decode cost, and cold start after device reboot.
- API 37 runtime. The CI workflow's Android 13 journey is functional and animation-disabled, not a profile run.
- Thermal behavior under sustained load. Thermal status remained 0 during the short audit, but no long-duration thermal test was run.

## G. Prioritised optimisation recommendations

| Priority | Finding / recommendation | Confidence and boundary |
| --- | --- | --- |
| P1 | Reproduce the semantics-traversal hotspot with matched accessibility-service conditions and longer frame traces before changing code. If it persists without the service, prototype a semantics-node reduction in catalogue cards while retaining separate, named actions. | High confidence in measured jank; medium confidence in the cause. |
| P2 | Profile burst quantity/add changes with save duration markers. If a queued write delays later state publication, adjust the update/write queue while preserving rollback, serialized durable writes, and saved-data recovery. | Source-supported risk, not reproduced. |
| P3 | Profile filtering/sorting at realistic dataset sizes; only then memoize or move calculations if they are material in traces. | Low severity; current data volume is small. |
| P3 | Leave product-image loading unchanged unless an actual image-rendering scroll path is introduced and profiled. | No image jank evidence in current catalogue scroll. |

No P0 issue was established, and no architectural rewrite is justified by this evidence.

## H. Task 9 implementation plan

Task 9 should begin with the first validation item below. Only implement a later item if the corresponding measurement reproduces it.

| Order / file | Existing problem | Proposed correction | Expected benefit | Regression risk | Verification |
| --- | --- | --- | --- | --- | --- |
| 1. `app/src/androidTest/java/ie/suric/lizshopping/ShoppingJourneyTest.kt` and local profiling procedure | The phone's enabled accessibility service confounds attribution; current CI journey disables animations and does not scroll for a sustained interval. | Add a repeatable, non-destructive catalogue-scroll profiling scenario and record service/environment state. Use a separate clean test target for the paired service comparison; do not change accessibility settings on the household phone. | Establish whether the measured hot path is app-wide or specific to the present device configuration. | Test fixture could touch real household data if run on the phone; retain a strict empty/disposable-fixture guard. | Five matched 60–120 second runs, Perfetto FrameTimeline, gfxinfo and Simpleperf, with identical device/build/thermal conditions. |
| 2. `Components.kt` and `MainActivity.kt` (conditional on step 1) | Product-card semantics traversal is a large inclusive CPU path during the measured run. | Prototype reducing redundant semantics nodes for noninteractive product text only; keep Add, Configure and Favourite actions individually labeled and independently operable. | Reduce semantics geometry work while preserving screen-reader actions. | Accessibility discoverability, focus order, and touch behavior can regress. | Compare node count and frame metrics; run existing journey plus TalkBack/Accessibility Scanner checks and inspect labels/focus order. |
| 3. `ShoppingViewModel.kt` (conditional on update-burst evidence) | Later changes wait behind mutex-held whole-snapshot serialization before publication. | If traces show user-visible delay, separate ordered state reduction/publication from durable IO writes without weakening rollback or write ordering. | Keep repeated quantity/add feedback prompt under slow storage. | Lost updates, stale rollback, incorrect shop isolation, and persistence-order bugs. | Repeated add/quantity bursts with visible-state timing and independent committed SQLite readback; include injected write-failure recovery. |
| 4. `MainActivity.kt` (conditional on large-list trace evidence) | Filtering/sorting/deduplication is repeated during composition. | Cache/derive only the measured expensive transformations, keyed to the exact inputs that change. | Reduce main-thread work on query or list updates. | Stale filtered results or incorrect ordering after shop/product changes. | Before/after profile at 10/50/100 items and search/category interactions; assert results and stable item identity. |

No database, Gradle dependency, permission, navigation-design, or image-removal change is proposed by this audit.

## I. Verification requirements for Task 10

- Use the same SM-A127F/API 33/60 Hz setup where available and a second clean Android 13 target. Record build type, APK hash, refresh rate, resolution, density, RAM, CPU, thermal state, and accessibility-service state for every run.
- Warm the app, then perform at least five identical 60–120 second scroll runs. Capture `gfxinfo framestats` and Perfetto FrameTimeline; use Simpleperf only in a separate CPU attribution run. Keep cold-start runs separate.
- Measure frame totals, Android jank count/rate, deadline misses, P50/P90/P95/P99/max frame durations, main/UI-thread slices and render-thread delays. Compare the exact same scenario and frame budget before/after.
- For interaction changes, time input to first visible state on a real target and retain the existing functional journey. For persistence-related changes, verify committed SQLite state and rollback on save failure; do not treat Compose state or activity recreation as durable-write evidence.
- Verify large-list fixtures only on an isolated disposable target. Never clear or seed the household phone's database.
- Suggested acceptance target for the affected 60 Hz scroll flow: at least halve the current matched-condition jank rate and reach no more than 5% missed-deadline frames over repeated runs, without accessibility or functional regressions. Report any environment where this target is not met.
- Then run the repository-required unit tests, lint, debug build and emulator journey for the implementation; report each result independently.

## Historical reference

The dated Task 7 startup and small `gfxinfo` samples remain historical and are not recast as current results. Current values above were freshly captured on 2026-10-16. The app's current route implementation has no route `Crossfade`; the old 160 ms transition is not present in the audited source.
