# Liz's Shopping List
Native Kotlin / Jetpack Compose grocery-list app for Lidl, Aldi, Dunnes Stores and SuperValu. No accounts, network permission, adverts or trackers.

## Current status
Source implementation 0.1.0. GitHub Actions run 38496681250 passed unit tests, lint, manifest checks, debug APK assembly and the complete Android 13 shopping journey on commit 6025f96. Representative-user usability, physical Samsung A12 behaviour and the remaining manual acceptance criteria are unverified. See TESTING.md.

## Build
Use JDK 17, Android SDK 37 and Gradle 9.4.1. Install SDK platform 37 and build-tools 36.0.0. Open this folder in Android Studio, set the SDK path in `local.properties`, and sync.

The Gradle wrapper is included. To regenerate it with Gradle 9.4.1 available:
```
gradle wrapper --gradle-version 9.4.1
./gradlew testDebugUnitTest lintDebug assembleDebug
```
APK output: `app/build/outputs/apk/debug/app-debug.apk`.

Run the instrumented journey on an Android 13 emulator:
```
adb shell pm clear ie.suric.lizshopping
./gradlew connectedDebugAndroidTest
```
The debug build uses the standard development certificate; it is not a permanent release-signing configuration. No release credentials have been created.

## Use
Choose a shop, search or browse, select a product, choose size and quantity, then add it. Open Shopping List to tick purchases off. Save favourite configurations in the product editor; add them with one tap from Favourites. Create any other product using Add custom item.

Shopping options contains fresh-list, completed clearing, previous-item reuse and local backup/restore. Clearing needs confirmation. Backup restore replaces all saved data only after validation and confirmation. A previous-item set is kept per shop, not a full historical archive.

Suggested varieties and sizes are generic presets, not confirmed shop inventory. Grocery icons are system emoji; shop names are text identifiers rather than licensed logo assets.

## Data compatibility
The storage snapshot is SQLite schema version 2 and the JSON payload/portable backup format is version 2. Existing schema/payload version-1 data and version-1 JSON backups are migrated with a deterministic product-ID mapping; the original shopping entries, favourites, previous items, custom products, quantities, completion and selected shop are retained. See [ARCHITECTURE.md](ARCHITECTURE.md) and [MIGRATIONS.md](MIGRATIONS.md).

Backups remain limited to 1 MiB. Phone-selected image bytes are stored privately in the app and are not embedded in backups; export replaces those references with generic category fallbacks so a backup does not contain unusable device-local paths. Bundled/category references remain offline. A custom shop is archived on removal so its separate lists and favourites remain recoverable.
