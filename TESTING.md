# Testing
## Evidence as of source delivery
GitHub Actions run 38491431476 (2026-10-14, commit ab1cef6) passed unit tests, lint, manifest inspection and debug APK assembly. Its Android 13 journey failed at ShoppingJourneyTest.kt:59 after saving eggs: the Shopping List navigation node was absent. APK assembly success does not establish runtime correctness.

The editor now hides previous confirmation snackbars and clears keyboard focus on entry so overlays cannot cover editor actions. Journey navigation explicitly waits for the destination controls. Screenshots and semantics are written to the instrumentation additional-output directory, which Gradle retrieves before uninstalling the app; the old adb collector produced `run-as: unknown package` text files instead of PNG images. Fresh validation passed in [GitHub Actions run 38496681250](https://github.com/brunkonjaa/Liz-s-Shopping-App/actions/runs/38496681250) on commit 6025f9633ab5c37a416656eac6cdc3168d7392ce: unit tests, lint, debug APK assembly, packaged manifest checks and the Android 13 journey all succeeded. The journey reports show one test, zero failures and zero errors. Actual PNG screenshots and semantics were retained; shopping-list and milk-editor captures were inspected. This is one successful emulator run, not a claim of physical-device or comprehensive usability validation.

Delivered APK SHA-256: `2c1d2c32a109c057d00f19c459398811c65ad0ca2aaeae8d81af41731a64d616`.

## Automated tests provided
RulesTest: 22 unit tests covering stable identities, product reuse, shop-scoped merge behaviour, search suggestions, configurable shops/categories, independent favourites, quantity/configuration rules, and validation.
StorageTest: 16 Robolectric Android 13 tests covering v1 migration, SQLite schema upgrade and failure rollback, v1 backup import, v2 round-trip, custom shop/product/category persistence, image references/fallback, malformed data and preservation after rejection.
ShoppingJourneyTest: Compose Android test for shop selection, search, two cartons of milk, completion and undo, eggs, favourite quantity independence, shop isolation and custom items. Run on a clean test installation; it changes app data.

## Task 2 coverage added
The updated `RulesTest` covers stable product/shop/category IDs, shop-scoped duplicate merging, cross-shop separation, tomato singular/plural search suggestions, explicit distinct product creation without mutation, duplicate-name rejection, custom shop validation/archive, custom category/product references and unchanged catalogue definitions.

The updated `StorageTest` uses a concrete v1 JSON fixture and covers deterministic migration of entry IDs, configurations, quantities, completion, favourites, previous items, custom products and selected shop; SQLite schema 1 → 2; rollback after a simulated malformed v1 row; v1 backup import; v2 round-trip; custom shop/category/product restart persistence; local image key persistence in the private snapshot; portable image fallback; missing-image category fallback; and malformed/oversized/unknown-reference rejection without replacement.

`ProductImageStore` validates JPEG, PNG and WebP signatures, file size and image bounds before copying to app-private storage. This task adds the storage service but not the visual image picker or rendered image cards. Picker/device image flows remain unverified until the later UI tasks and emulator/device checks.

## Required validation commands
`./gradlew testDebugUnitTest lintDebug assembleDebug`
`./gradlew connectedDebugAndroidTest`
Retain exit statuses, unit XML reports, lint report and APK checksum. Inspect merged manifest permissions and launcher export.

## Reproducible usability acceptance (unverified)
1. Fresh launch: identify and choose Lidl without instruction.
2. Search Milk; choose 2 L; increase packages to 2; add. Add one 12-pack Free Range Eggs.
3. List reads Milk 2 L ×2 and Eggs Free Range 12 pack ×1.
4. Tick/untick eggs with visible state; completion moves entry down.
5. Change to Aldi; Lidl items remain isolated; switching back restores them.
6. Save a Lidl favourite with usual quantity 2; change list quantity to 3; favourite remains 2.
7. Add and reuse an arbitrary named custom item with only the name entered.
8. Restart and reboot; verify saved data survives.
9. Start fresh with confirmation; favourites remain; previous items can be reused.
10. Backup, change data, restore with confirmation; malformed backup changes nothing.

Accessibility: inspect at 360 dp width, portrait and landscape, 100%, 150% and 200% font size, TalkBack enabled. Verify all targets ≥48 dp, meaningful labels, readable contrast, scroll reachability, no overlapping controls, predictable system Back and no colour-only state. Check offline after disabling network.

A 10/10 target requires all functional criteria, no critical usability blocker and direct observation of representative users completing ordinary tasks without help. No score is claimed. Samsung A12-class responsiveness and visual fit remain unverified.

## Known pending risks
Keyboard, font scaling, process-recreation drafts, file-provider errors and sustained write sequences need device testing. Automated UI coverage needs expansion for shop switching, custom creation, favourite editing and file picker restore.
