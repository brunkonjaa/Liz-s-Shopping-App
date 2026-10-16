# Security and privacy

Current controls and evidence boundary - 2026-10-16.

## Application controls

- Offline native app; no server, account, network SDK, analytics, adverts, payments or permanent release-signing key added.
- Private SQLite snapshots use parameter-bound writes and transactions. Invalid migrations/restores preserve existing data rather than replacing it with an empty state.
- Backup input is limited to 1 MiB and validates format/version, IDs/references, collection/text limits, booleans, counts and image metadata before replacement.
- User images use the document picker, bounded bytes/dimensions, generated private filenames and safe fallback references. No broad filesystem permission is needed.
- Automatic backup remains disabled; manual exports/imports use user-selected document URIs. No device-wide scan or cleartext/network integration.

## Verified scope

The successful GitHub build checks the packaged manifest, including dependency contributions: no network/runtime permissions and only the launcher exported. The compatibility signature permission is not a runtime permission. Unit/lint/build and the Android 13 journey pass; no vulnerability-free or formal security-certification claim is made.

Task 5 diagnostics live only in the test APK. State summaries limit household detail to controlled Bread quantities and counts; screenshots/trees can still contain visible data. Use disposable test state. CI retains selected app/test/SQLite logs on its fresh emulator; phone diagnostic log capture is app-PID limited. Snapshot readback is OPEN_READONLY.

## Local material and remaining review

Visual references, handovers, screenshots/UI XML and raw database snapshots remain ignored. Consolidation preserves original local edits in ignored backups and a retained stash rather than uploading private evidence. No production data or package identity changes were introduced by the CI investigation.

Adversarial restore/URI execution, formal MASVS/dependency review, revoked-URI UI behaviour and release-signing review remain unverified. Current test evidence and coverage limits are in [TESTING.md](TESTING.md).
