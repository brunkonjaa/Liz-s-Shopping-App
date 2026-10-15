# Liz's Shopping List

Native Kotlin and Jetpack Compose grocery-list app for Lidl, Aldi, Dunnes Stores and SuperValu. It is offline-first and uses private SQLite snapshot storage; it has no accounts, network services, adverts, trackers or pricing.

## Implemented

- Stable reusable catalogue product IDs and shop IDs.
- Independent shop lists, favourites, previous items, quantities and completion states.
- Persisted custom shops with rename and conservative deletion safeguards.
- Version-2 JSON backup/restore with bounded validation and version-1 import compatibility.
- Explicit SQLite schema v1-to-v2 migration with transactional failure preservation.
- Bounded Android document-picker image import into app-private storage, with durable references and backup metadata.
- Task 3 warm editorial UI, drawer, light/dark/system appearance, catalogue, favourites and shopping-list flows.

## Build and verification

Use JDK 17, Android SDK 37 and the included Gradle wrapper:

```text
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew connectedDebugAndroidTest
```

On 2026-10-15, unit tests, lint, debug APK assembly and the connected Android journey all passed on the available Samsung SM-A127F running Android 13. The API 37 emulator remains unverified because its previously observed startup/high-CPU issue has not been resolved; see `TESTING.md` for the evidence boundary.

Task 1 repository audit and architecture review are complete. Task 2 catalogue architecture recovery and Task 3 visual redesign are preserved in this branch. Task 4's comprehensive shared bottom-sheet workflows remain pending.

## Scope limits

Task 4's comprehensive shared bottom-sheet workflows, dated purchase history, large-font/TalkBack checks, representative-user checks and release-signing validation remain open. Private visual references and handover notes stay ignored and are not uploaded to GitHub.
