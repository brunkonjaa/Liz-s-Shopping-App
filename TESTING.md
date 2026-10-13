# Testing
## Evidence as of source delivery
No Android build, automated test execution, lint execution, emulator run or representative-user testing has completed. Toolchain installation is a separately tracked job. Source checks alone do not prove APK functionality.

## Automated tests provided
RulesTest: 17 tests covering pack-count separation, normalized duplicate merge, variety/size differences, quantity bounds, edit collision, favourite independence, fresh/reuse, completion reopening, favourite replacement, key catalogue presets, custom weights and invalid text/quantity.
StorageTest: 9 Robolectric Android 13 tests covering backup round-trip, shop independence, SQLite reopen, unknown version/shop, size limit, fractional quantity, failed restore data preservation and plain-text custom input.
ShoppingJourneyTest: Compose Android test for shop selection, search, two cartons of milk, list addition, completion and undo. Run on a clean test installation; it changes app data.

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
Dependency compatibility and compilation need actual Gradle verification. Keyboard, font scaling, process-recreation drafts, file-provider errors and sustained write sequences need device testing. Automated UI coverage needs expansion for shop switching, custom creation, favourite editing and file picker restore.
