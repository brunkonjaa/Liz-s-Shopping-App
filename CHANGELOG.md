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
