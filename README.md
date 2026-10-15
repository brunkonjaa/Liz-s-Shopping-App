# Liz's Shopping List

## Task 5 CI investigation - 2026-10-16

The unchanged Android 13 emulator journey failed again on GitHub while the unchanged Samsung journey passed. CI artifacts establish that Add is fully clipped below the shorter emulator viewport. A second CI run confirmed the first Add works after scrolling, then exposed a snackbar covering the second tap. The maintained test scrolls, asserts displayed/enabled, and waits for the observable snackbar to clear before card interaction; production is unchanged. Corrected CI validation is pending. See [the CI investigation report](docs/TASK5_CI_INSTRUMENTATION_REPORT.md). Earlier phone/build passes do not establish CI reliability.

Native Kotlin and Jetpack Compose grocery-list app for Lidl, Aldi, Dunnes Stores and SuperValu. It is offline-first and uses private SQLite snapshot storage; it has no accounts, network services, adverts, trackers or pricing.

## Implemented

- Stable reusable catalogue product IDs and shop IDs.
- Independent shop lists, favourites, previous items, quantities and completion states.
- Persisted custom shops with rename and conservative deletion safeguards.
- Version-2 JSON backup/restore with bounded validation and version-1 import compatibility.
- Explicit SQLite schema v1-to-v2 migration with transactional failure preservation.
- Bounded Android document-picker image import into app-private storage, with durable references and backup metadata.
- Task 3 warm editorial UI, drawer, light/dark/system appearance, catalogue, favourites and shopping-list flows.
- Task 7 responsive interaction pass: direct catalogue Add actions, optimistic quantity feedback with rollback, saved-list launch routing, target-aware transitions and explicit action semantics.

## Build and verification

Use JDK 17, Android SDK 37 and the included Gradle wrapper:

```text
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew connectedDebugAndroidTest
```

On 2026-10-15, unit tests, lint, debug APK assembly and the connected Android journey all passed on the available Samsung SM-A127F running Android 13. The API 37 emulator remains unverified because its previously observed startup/high-CPU issue has not been resolved; see `TESTING.md` for the evidence boundary.

Task 1 repository audit and architecture review are complete. Task 2 catalogue architecture recovery and Task 3 visual redesign are preserved in this branch. Task 4 adds shared Material 3 bottom sheets for shop selection/creation/management, product configuration and shopping-list options while preserving the Task 3 architecture and offline data model. See `TASK_4_IMPLEMENTATION.md` and `TESTING.md` for the implementation boundary and fresh verification evidence.

Task 7 evidence and limitations are recorded in `docs/TASK7_INTERACTION_AND_PERFORMANCE_REPORT.md`.

## Scope limits

Large-font/TalkBack checks, representative-user checks, screenshot comparison, physical-device persistence verification and release-signing validation remain open until directly executed. Private visual references and handover notes stay ignored and are not uploaded to GitHub.
