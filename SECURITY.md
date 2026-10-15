# Security and privacy

## Task 5 diagnostic artifacts

Diagnostic code is packaged only in the test APK. It adds no production permissions, dependencies, telemetry or storage hooks. State reports include Bread quantities and collection counts instead of a household backup. UI trees/screenshots can contain visible data: use disposable test state. CI captures selected app/test/SQLite log tags on its fresh emulator; phone log capture is limited to the app PID. Private database snapshots and phone captures remain in ignored verification output.

## Controls implemented

- No Android permissions, `INTERNET` permission, network SDK, account, analytics, adverts or server were added.
- Grocery data remains in private SQLite storage. Snapshot writes use parameter-bound values and a transaction; failed writes leave the previous state intact.
- Database schema upgrades are explicit. Version 1 is decoded and migrated to version 2 only after complete validation; failed migration does not intentionally replace data with an empty state.
- Backup input is bounded to 1 MiB and validates format versions, stable ID uniqueness, shop/product references, text, quantities, booleans, image metadata and collection sizes before restore.
- Restore uses a validated pending state and transactional save. Existing data is not changed when parsing or saving fails.
- User images use the Android document picker, bounded byte/dimension processing, generated app-private filenames and path-traversal checks. Ephemeral picker URIs are not persisted as the only reference.
- Automatic cloud backup remains disabled. Manual backups are user-selected through SAF; no device-wide file scanning is used.

## Remaining review items

Adversarial input execution, formal MASVS review, full accessibility review and release-signing review remain unverified. Custom encryption/authentication is not planned for this small private household app.
