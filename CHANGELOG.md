# Changelog

## Task 2 architecture recovery — 2026-10-15

- Added stable reusable product IDs, product references on shopping entries and stable shop definitions.
- Added persisted custom-shop creation, rename support and conservative removal safeguards.
- Added v2 JSON backup data with v1 import compatibility and explicit v1-to-v2 SQLite migration.
- Added bounded offline custom-image import into app-private storage and safe image metadata validation.
- Preserved the Task 3 warm Compose design and wired the existing drawer/home/editor to dynamic shops and catalogue references.
- Added migration, identity, reuse, shop-safety, image-reference and failure-recovery tests.

## Verification baseline — 2026-10-15

- Unit tests, lint, debug APK assembly and the connected Android journey passed on Samsung SM-A127F / Android 13 after the final Task 2/3 integration edits.
- The API 37 emulator remains unverified because its previously reported startup/high-CPU issue remains unresolved.
- Task 1 repository audit and architecture review are complete; Task 4 shared bottom-sheet workflows, dated history, broad accessibility checks and release validation remain open.

## Task 4 shared interactions - 2026-10-15

- Added the reusable `LizBottomSheet` Material 3 surface and consistent shop/options/confirmation sheets.
- Added change-shop selection, suggested/custom shop creation, rename and guarded removal flows without changing stable shop IDs.
- Presented the existing product editor in the shared sheet, added catalogue reuse suggestions from the drawer, custom-category validation, optional notes and immediate catalogue favourite toggles.
- Added focused category and shop-rule tests. Connected-device interaction, screenshots and persistence-after-restart must be recorded separately from compilation and unit-test results.

## Task 5 acceptance evidence - 2026-10-15

- Recorded fresh unit-test, lint, debug-assembly and connected-device results, plus manifest, crash-buffer and APK checksum evidence.
- Recorded a non-destructive Samsung Android 13 manual flow covering product configuration, favourites, list completion, force-stop/restart persistence, shop isolation and light/dark theme inspection.
- Classified backup/restore UI, TalkBack, device reboot/update persistence, the full visual matrix, several management/reuse journeys and API 37 as **NOT VERIFIED** rather than passing them by inference.
- Release assessment is **CONDITIONALLY READY** with no confirmed P0/P1 defects in the tested scope. See `docs/TASK5_ACCEPTANCE_REPORT.md`.
