# Testing

## Task 4 evidence - 2026-10-15

- `./gradlew.bat testDebugUnitTest lintDebug assembleDebug --console=plain` passed after the Task 4 changes.
- `./gradlew.bat connectedDebugAndroidTest --console=plain` passed on the connected Samsung SM-A127F / Android 13: 1 test, 0 failures, 0 errors.
- Manual launch, UI-tree inspection, change-shop sheet inspection and product-sheet scrolling were completed on the same device. Screenshots are stored under ignored `verification/` output; no clipping was observed in the inspected views.
- A standalone restart produced no Liz app crash-buffer lines, but it did not establish data persistence because the instrumentation run had reset app data before the standalone launch. Persistence remains unverified at the device level; storage round-trip and migration tests remain covered by `StorageTest`.
- The API 37 emulator remains unverified because of its previously observed startup/high-CPU issue.

## Task 4 focused coverage

- `RulesTest` covers case/whitespace-insensitive category reuse, invalid short categories and the distinction between suggested and built-in shops.
- `ShoppingJourneyTest` exercises product configuration, quantity controls, favourites, shop switching, list isolation and custom item creation. Its assertions still require a fresh Task 4 device run.

## Current evidence — 2026-10-15

- `./gradlew testDebugUnitTest --no-build-cache` passed. The source and Android test files contain 35 test methods across `RulesTest`, `StorageTest` and `ShoppingJourneyTest`.
- `./gradlew lintDebug --no-build-cache` passed.
- `./gradlew assembleDebug --no-build-cache` passed. The resulting debug APK SHA-256 was recorded locally as `D06D04FDE4E7121101C34E68F7EC4D46FA29F547AC2E43642CA6497B2F12A870`.
- `./gradlew connectedDebugAndroidTest --console=plain --no-build-cache` passed on Samsung SM-A127F / Android 13: 1 test, 0 failures, 0 errors.
- `./gradlew installDebug --console=plain --no-build-cache` installed successfully on that device. A manual launcher start resolved `ie.suric.lizshopping/.MainActivity`; the UI tree showed the editorial home, four default shops, favourites, current list, all items and settings, with no matching crash-buffer output.

The API 37 emulator remains unverified. Its previous startup/high-CPU issue remains outstanding, and no claim is made that the emulator journey passed.

## Task 5 acceptance evidence - 2026-10-15

- Fresh `testDebugUnitTest`, `lintDebug`, `assembleDebug` and `connectedDebugAndroidTest` runs passed on the current checkout. The connected test completed 1 test with 0 failures on Samsung SM-A127F / Android 13.
- A debug APK was installed without clearing the dedicated test installation. Manual evidence under ignored `verification/task5/` covers first launch, Lidl product search/configuration, quantity/favourite/add-to-list, completion, force-stop/restart persistence, Aldi list isolation, and light/dark theme inspection.
- The manual restart retained the controlled Lidl completion state and Aldi remained independent. Device reboot, in-place update, backup/restore UI, TalkBack, large fonts, landscape and API 37 remain **NOT VERIFIED**.
- The Task 5 acceptance report records the exact command results, APK checksum, screenshots, security checks and release-readiness decision.

## Coverage limits

The connected journey does not cover custom-shop creation, product image selection, v1 database upgrade on a physical target, backup/restore UI error paths, large font sizes, TalkBack, landscape or representative-user behaviour. The new sheet workflows still require a fresh connected-device run and are not claimed verified from compilation alone.
