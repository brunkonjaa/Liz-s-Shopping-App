# Task 7 — Interaction and Performance Report

## Historical scope and current follow-up - 2026-10-16

This is the interaction and performance record from 2026-10-15. Its samples and test counts describe that build. Catalogue Add now adds one package while retaining the saved variety and size; Favourite Add uses its own count. The 160 ms route Crossfade described below was later removed, and routes now render current state directly. Current checks are listed in [TESTING.md](TESTING.md), with the latest visual work in the [Task 12 report](TASK12_VISUAL_REFINEMENT_REPORT.md). No new steady-state performance profile was collected during CI repair.

Date: 2026-10-15
Branch: `development/task2-task3-integration`
Starting commit: `4383069a86cda79ba1c5beb16fcf3c5d06ff8b87`

## 1. Repository baseline

Liz's Shopping List remains a native Kotlin/Jetpack Compose, offline-only application with package ID `ie.suric.lizshopping`. No servers, accounts, analytics, payments, new permissions or network dependencies were added. The existing SQLite snapshot model and four independent shop lists remain in place.

## 2. Tracker comparison methodology

Bruno's Personal Training Tracker was inspected read-only on the connected Samsung SM-A127F running Android 13. The comparison used the launch surface, UI tree semantics and visible interaction structure only; no tracker files, settings or data were changed.

The tracker exposed the task, current state and immediate controls together: an exercise row showed the planned load/repetitions, explicit increase/decrease controls and a visible `DONE` action. This informed Liz's product-card Add action, named quantity controls and visible completion state.

## 3. Findings

- Liz previously required opening the product editor from a catalogue card whose visible plus glyph was not itself an action.
- Quantity changes waited for the serialized persistence operation before the UI reflected them.
- A returning user could be presented with transient home/drawer state instead of the current saved shopping list.
- The app already used lazy Compose containers for catalogue and list content; the main opportunity was interaction latency and state routing rather than a wholesale UI rewrite.

## 4. Performance baseline measurements

The measurements are directional, not a production benchmark. Each launch timing is a cold `am start -W` after force-stop on the same Samsung device.

| Surface | Cold launch samples | Small gfxinfo sample |
|---|---:|---:|
| Liz before Task 7 | 1,731 / 1,779 / 1,763 ms | 5 frames, 5 janky |
| Tracker comparison | 1,364 / 882 / 829 ms | 5 frames, 4 janky |
| Liz after Task 7 | 1,936 / 1,863 / 1,843 ms | 8 frames, 8 janky |

The gfxinfo samples cover startup only and contain very few frames; they are not a representative steady-state scroll or gesture profile. The after sample therefore does not prove or disprove smooth scrolling. No literal zero-jank claim is made.

## 5. Interaction changes

- Added a visible, product-specific `+ Add` button to catalogue cards; configuration is now a distinct secondary action.
- One-tap catalogue Add reuses saved shop configuration and favourite quantity where available, otherwise selecting the first valid catalogue options.
- Added explicit Add/remove-favourite semantics and product-specific Add semantics in both catalogue and favourite cards.
- Quantity and completion actions remain on the shopping list and expose named controls with at least 48dp touch targets.
- Returning users with a saved shop now open directly to that shop's shopping list; first-time users still start at Home.
- Added a short 160ms route crossfade for screen, tab, shop and editor changes.
- Drawer visibility is transient and is not restored over the saved list after a process restart.

## 6. Compose optimisation

The implementation keeps catalogue and shopping-list content in `LazyColumn` containers, uses stable product/list keys where already available, and avoids introducing blocking work in Compose callbacks. Route-based `Crossfade` uses the target route as its content selector so it does not animate identical content accidentally. Quantity changes publish an optimistic in-memory state before the disk write while the existing mutex still serializes writes.

## 7. Image performance

No network image loading or new image pipeline was introduced. Existing local drawable resources and app-private image references remain the image source. The new catalogue card keeps the existing lightweight product thumbnail path. Image-heavy scrolling and large imported-image profiling remain unverified.

## 8. Database and persistence

Small list changes are written on `Dispatchers.IO` under the existing mutex. If a write fails, the in-memory state rolls back to the state immediately preceding that write and the user receives an error message. The default-item rule reads saved item/favourite configuration without changing favourite defaults during list edits. No destructive schema upgrade was added.

## 9. Before / after interaction comparison

| Interaction | Before | After |
|---|---|---|
| Add familiar catalogue product | Open card/editor, configure, save | Visible one-tap Add; Configure remains available |
| Quantity tap | UI feedback followed persistence | Immediate optimistic feedback with serialized durable write |
| Returning launch | Could show transient home/drawer state | Saved shop list is the launch route |
| Screen/tab transition | Existing content switched without a route transition | 160ms target-aware crossfade |
| Favourite Add | Generic visible `+ Add` | Product-specific accessible Add action |
| Shop isolation | Existing model preserved | Connected journey verifies Lidl/Aldi separation |

## 10. Seven-journey acceptance results

1. **Launch into the current list — PASS.** Fresh restart inspection showed the drawer closed, shopping-list route visible and Home/All Items content absent when a saved shop existed.
2. **Direct catalogue add — PASS.** Connected journey adds Bread twice through its explicit Add action; manual inspection also verified Milk merging into one row.
3. **Rapid quantity changes — PASS.** Connected journey performs repeated increase/decrease actions; unit coverage verifies sequential bounded updates through 999.
4. **Completion without navigation — PASS.** Connected journey toggles Bread completion off and on using named controls.
5. **Favourites reuse — PASS.** Connected journey saves Bread as a favourite, opens Favourites and adds it through the product-specific action.
6. **Independent shop lists — PASS.** Connected journey switches to Aldi, verifies Bread is absent, returns to Lidl and verifies Bread remains.
7. **Persistence after recreation — PASS for activity recreation; manual force-stop route also inspected.** The connected journey recreates the activity and verifies the Lidl list. Device reboot, in-place update and backup/restore UI remain unverified.

## 11. Regression results

- `testDebugUnitTest --no-build-cache`: PASS, 41 tests, 0 failures.
- `lintDebug --no-build-cache`: PASS; 13 existing warnings remain, no lint errors.
- `assembleDebug --no-build-cache`: PASS.
- `connectedDebugAndroidTest --no-build-cache`: PASS on Samsung SM-A127F / Android 13, 1 test, 0 failures.
- Crash-buffer inspection during manual checks found no Liz crash entries.
- Manifest and offline constraints were preserved; no new permissions or network services were introduced.

## 12. Limitations

The requested “no jerkiness whatsoever” standard cannot be established from a short launch sample. Steady-state frame pacing during long list scrolls, repeated swipes, bottom-sheet drags, keyboard input, large text, TalkBack and low-battery/thermal conditions was not profiled with Perfetto or a frame-time trace. The API 37 emulator remains unverified because of its previous high-CPU/startup issue. Only one physical device was available.

## 13. Defects

No confirmed P0/P1 defect was found in the tested scope. The Android test still uses the deprecated `createAndroidComposeRule` API and emits a warning; it is not a runtime defect but should be migrated in a follow-up. The startup gfxinfo sample is an evidence limitation, not a diagnosed rendering defect.

## 14. Recommendations

1. Profile a representative 60–120 second gesture script with Perfetto/JankStats on at least one mid-range and one high-end Android device.
2. Add a release-like macrobenchmark for cold start, catalogue scroll, quantity bursts and bottom-sheet drag settling.
3. Migrate the connected test to Compose test APIs v2, then retain the product-specific semantics as the interaction contract.
4. Repeat the acceptance matrix on the API 37 emulator after its CPU/startup issue is resolved.

Overall assessment: Task 7 materially reduces interaction friction and persistence latency, adds target-aware transitions and improves accessible action targeting. It is suitable for continued development and device testing, but “iPhone-level, zero-observable-jank” remains a measured performance target rather than a claim established by this run.
