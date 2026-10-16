# Liz's Shopping List

A native Kotlin and Jetpack Compose grocery list for Lidl, Aldi, Dunnes Stores and SuperValu. It works offline and keeps each shop's list separate. The app has no accounts, ads, analytics, payments or online services.

## Current implementation

- A shared catalogue with stable product IDs, varieties, package sizes and optional offline images.
- Separate lists, favourites and previous items for each shop.
- Four protected default shops, plus custom shops with validated names and optional logos.
- Catalogue Add adds one package, using a saved variety and size when available. Favourite Add uses its saved count. Editing a list item does not change its favourite default. Package size and package count are separate; counts range from 1 to 999.
- Material 3 sheets for shop management, product configuration and list actions.
- Private SQLite snapshot storage with explicit v1/v2 migrations. JSON backup restore accepts bounded, validated v1, v2 and v3 files and replaces data transactionally.
- Optional product and shop images selected with the document picker and stored in app-private storage.
- Correct local logos for the four default shops, shared across the app. Custom shops show their chosen image or a neutral icon.
- Section artwork, an alphabetized All Items list, Liz's portrait on the launcher and splash screen, and a faint portrait backdrop.

## Verified state - 2026-10-16

Task 12 is implemented on `task12/visual-refinement`. The current local build passed 55 unit tests, lint (0 errors, 16 warnings) and signed Release assembly. We installed and launched that Release on a Samsung SM-A127F running Android 13, and inspected Home, the shop cards, drawer and Settings. Home and the Android 12+ splash screen show Liz; Home also shows a basket of apples, vegetables and bread. The connected shopping journey was attempted but failed at the initial Lidl card selector. See the [Task 12 report](docs/TASK12_VISUAL_REFINEMENT_REPORT.md) for the screenshots and full verification limits.

The following paragraphs record earlier Task 5 evidence and remain historical. At implementation `265b941`, 50 unit tests passed alongside lint, APK and packaged-security checks, three Samsung Android 13 journey runs and [GitHub Android 13 Pixel 2 instrumentation](https://github.com/brunkonjaa/Liz-s-Shopping-App/actions/runs/38825384833). The CI journey was later strengthened with committed SQLite read-back checks for shop selection and independent quantities.

The strengthened Samsung journey passed in 68.805 seconds. Activity recreation alone retains the ViewModel, so the test reads the committed SQLite snapshot to verify that Aldi and Lidl keep independent data.

Those earlier checks do not cover backup/restore UI, accessibility, reboot/update persistence, every sheet or custom-image flow, or API 37 runtime compatibility. See [TESTING.md](TESTING.md) for the current evidence and open checks.

## Build

Use JDK 17, Android compile SDK 37.0 and the Gradle 9.4.1 wrapper:

```text
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew connectedDebugAndroidTest
```

Instrumentation requires a disposable empty installation and can uninstall the app. Preserve household data before running it; the test refuses non-empty fixtures and does not clear them itself.

## Documentation

The seven current project documents are this README, [Features](FEATURES.md), [Architecture](ARCHITECTURE.md), [Testing](TESTING.md), [Migration](MIGRATION.md), [Security](SECURITY.md) and [Changelog](CHANGELOG.md).

### Task reports

- [Task 3: Visual redesign](TASK_3_IMPLEMENTATION.md)
- [Task 4: Shared interactions](TASK_4_IMPLEMENTATION.md)
- [Task 5: Acceptance report](docs/TASK5_ACCEPTANCE_REPORT.md)
- [Task 5: CI investigation](docs/TASK5_CI_INSTRUMENTATION_REPORT.md)
- [Task 7: Interaction and performance](docs/TASK7_INTERACTION_AND_PERFORMANCE_REPORT.md)
- [Task 8: Performance audit](docs/TASK8_PERFORMANCE_AUDIT.md)
- [Task 9: Performance investigation](docs/TASK9_PERFORMANCE_OPTIMISATION_REPORT.md)
- [Task 10: Performance isolation](docs/TASK10_PERFORMANCE_ISOLATION_AND_REMEDIATION_REPORT.md)
- [Task 11: Production release](docs/TASK11_PRODUCTION_RELEASE_REPORT.md)
- [Task 12: Visual refinement](docs/TASK12_VISUAL_REFINEMENT_REPORT.md)

The performance reports keep their original dates and workloads. Task 8 and Task 9 found severe scroll jank on a debug build while an accessibility service was enabled. Task 11 later measured 3.12% pooled GFXInfo jank on the Release build, but build type and run order differed, so the figures do not establish a cause. The [Task 10 report](docs/TASK10_PERFORMANCE_ISOLATION_AND_REMEDIATION_REPORT.md) explains why a matched service comparison remains open.

Private design references, handovers, raw device captures and database snapshots stay local. The selected Task 12 logo screenshots are included in its report at the user's request.
