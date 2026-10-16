# Security and privacy

This page describes the app's current privacy controls and the checks completed so far.

## Application controls

- Offline native app; no server, account, network SDK, analytics, adverts or payments added. Task 11 created the first release-signing identity; it lives outside the repository and is never included in the APK.
- Private SQLite snapshots use parameter-bound writes and transactions. Invalid migrations/restores preserve existing data rather than replacing it with an empty state.
- Backup input is limited to 1 MiB and validates format/version, IDs/references, collection/text limits, booleans, counts and image metadata before replacement.
- User images use the document picker, bounded bytes/dimensions, generated private filenames and safe fallback references. No broad filesystem permission is needed.
- Custom-shop logo references and embedded bytes use the same bounded validation, private storage and backup-size limits as product images. Default supermarket logos are bundled local assets; no logo network requests were added.
- Automatic backup remains disabled; manual exports/imports use user-selected document URIs. No device-wide scan or cleartext/network integration.

## Verified scope

Task 12's local unit tests, lint and signed Release assembly passed. The packaged manifest adds no permissions. We installed the Release on Android 13, but the connected journey failed at its first shop-card selector and is not counted as a pass. These checks are not a formal security review or certification.

Task 5 diagnostics live only in the test APK. State summaries limit household detail to controlled Bread quantities and counts; screenshots/trees can still contain visible data. Use disposable test state. CI retains selected app/test/SQLite logs on its fresh emulator; phone diagnostic log capture is app-PID limited. Snapshot readback is OPEN_READONLY.

## Local material and remaining review

Private visual references, handovers, raw UI/XML captures and database snapshots stay local. Two reviewed Task 12 app screenshots are included in the documentation at the user's request; neither contains shopping-list data. No production data or package identity changes were introduced by the CI investigation.

Adversarial restore/URI execution, formal MASVS/dependency review, revoked-URI UI behaviour and release-signing review remain unverified. Current test evidence and coverage limits are in [TESTING.md](TESTING.md).

## Release signing - Task 11

The local PKCS12 identity is stored at `%USERPROFILE%\.lizshopping\release-signing.p12`; its random password is DPAPI-encrypted in `release-password.dpapi` and restricted to the current Windows account and SYSTEM. Both are outside Git. `scripts/build-release.ps1` decrypts the password only for its Gradle child process, clears the environment values afterward and prints no credential. Do not copy either file into the repository, email, cloud-sync or an unencrypted drive. For a backup, copy the `.p12` to offline storage protected by BitLocker or equivalent and preserve the matching DPAPI blob and Windows account/profile recovery on the same protected backup. The DPAPI blob is account/machine-context protected and is not a portable password export; a migration to another PC needs a separately arranged secure credential recovery before the old profile is retired. No off-device key backup has yet been created or restore-tested. Keep the identity available and use the same one for every future APK update. Fingerprint and repeat-build instructions are in the [Task 11 report](TASK11_PRODUCTION_RELEASE_REPORT.md).
