# Changelog
## Task 2 — Catalogue and storage model
- Added stable product, category and shop IDs with shared product definitions, explicit product/shop associations, independent shop-scoped lists/favourites/previous items, and archive-based custom shop removal.
- Added offline product match suggestions, custom product/category/shop domain operations, and per-entry product/shop references while keeping package size separate from package count.
- Added transactional SQLite v1 → v2 payload migration, deterministic legacy product mapping, version-2 export and continued version-1 backup import.
- Added app-private image-copy validation for JPEG/PNG/WebP (10 MiB, 4096 px and 12 MP limits), durable generated references, category fallback resolution and reference-aware deletion. Portable backups exclude local image bytes and use generic category fallbacks.
- Added real v1 fixture, schema migration/rollback, v2 round-trip, invalid backup, shop/product/category and image-reference tests. See TESTING.md for fresh verification status.

## Validated fix — 2026-10-14
- Keep confirmation snackbars out of the product editor and clear carried-over search focus/keyboard when opening it.
- Wait for navigation controls in the Android journey instead of clicking before the editor has closed.
- Retain actual PNG screenshots and semantics through Gradle additional test output rather than querying an already-uninstalled app.
- GitHub Actions run 38496681250 passed unit tests, lint, manifest checks, APK assembly and the complete Android 13 journey on commit 6025f96. Physical-device and representative-user checks remain unverified.

## 0.1.0 — 2026-10-13 (unverified source implementation)
Initial native Compose application, grocery catalogue, four shop lists, custom products, independent favourites, SQLite persistence, validated local backups, list reuse, automated test sources and project documentation. APK build and runtime validation pending.
