# Liz's Shopping List
Native Kotlin / Jetpack Compose grocery-list app for Lidl, Aldi, Dunnes Stores and SuperValu. No accounts, network permission, adverts or trackers.

## Current status
Source implementation 0.1.0. GitHub Actions run 38491431476 passed unit tests, lint, manifest checks and debug APK assembly. The Android 13 journey failed after saving eggs. Editor overlay and test diagnostics fixes have been applied; a fresh journey result is pending. See TESTING.md.

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
