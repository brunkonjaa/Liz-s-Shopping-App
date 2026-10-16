# Liz's Shopping List

A native Kotlin and Jetpack Compose grocery-list app for Lidl, Aldi, Dunnes Stores and SuperValu. Lists work offline, with no accounts, network services, adverts, analytics, payments or pricing. The existing cream/olive editorial interface and light/dark/system appearance are preserved.

## Current implementation

- Shared catalogue with stable product IDs, varieties, package sizes and optional offline images.
- Independent shop lists, favourites, previous items, package counts and completion states.
- Four protected default shops and validated custom-shop creation/rename/removal.
- Direct catalogue Add adds **one package**, using a saved variety/size when available. Favourite Add uses that favourite's saved count; list edits never change the favourite default. Package size and package count are separate; counts are 1-999.
- Contextual Material 3 sheets for shop management, product configuration and list/backup actions.
- Private SQLite snapshot storage, explicit v1-to-v2 migration, bounded validated v1/v2 JSON restore and transactional replacement.
- Offline document-picker image import into app-private storage. No broad storage or network permission.

## Verified state - 2026-10-16

Implementation `265b941` passed 50 unit tests, lint (0 errors, 13 existing warnings), debug/test APK assembly, packaged-security checks, three current Samsung Android 13 journey runs and [GitHub Android 13 Pixel 2 instrumentation](https://github.com/brunkonjaa/Liz-s-Shopping-App/actions/runs/38825384833). Failed and successful CI artifacts were inspected. The test now accounts for off-screen controls and transient snackbar coverage without sleeps, weaker outcomes or production changes for Task 5.

This consolidation brings the later state/count fixes and the verified test/documents together on the main integration path. It does not certify untested backup/restore UI, accessibility, reboot/update persistence, all sheet/custom-image flows or API 37 runtime compatibility. See [TESTING.md](TESTING.md) for current evidence and limits.

## Build

Use JDK 17, Android compile SDK 37.0 and the Gradle 9.4.1 wrapper:

```text
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew connectedDebugAndroidTest
```

Instrumentation requires a disposable empty installation and can uninstall the app. Preserve household data before running it; the test refuses non-empty fixtures and does not clear them itself.

## Documentation

The seven current project documents are this README, [FEATURES](FEATURES.md), [ARCHITECTURE](ARCHITECTURE.md), [TESTING](TESTING.md), [MIGRATION](MIGRATION.md), [SECURITY](SECURITY.md) and [CHANGELOG](CHANGELOG.md).

Task records: [Task 3 UI](TASK_3_IMPLEMENTATION.md), [Task 4 sheets](TASK_4_IMPLEMENTATION.md), [historical acceptance](docs/TASK5_ACCEPTANCE_REPORT.md), [historical Task 7 performance](docs/TASK7_INTERACTION_AND_PERFORMANCE_REPORT.md), and [CI diagnosis/completion](docs/TASK5_CI_INSTRUMENTATION_REPORT.md). Historical measurements retain their dates and do not supersede current testing evidence. Local visual references, handovers, captures and database snapshots stay ignored and are not uploaded.
