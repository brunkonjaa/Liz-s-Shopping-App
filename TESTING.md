# Testing

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

## Coverage limits

The connected journey does not cover custom-shop creation, product image selection, v1 database upgrade on a physical target, backup/restore UI error paths, large font sizes, TalkBack, landscape or representative-user behaviour. The new sheet workflows still require a fresh connected-device run and are not claimed verified from compilation alone.
