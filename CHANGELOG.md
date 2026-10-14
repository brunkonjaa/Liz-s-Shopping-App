# Changelog
## Validated fix — 2026-10-14
- Keep confirmation snackbars out of the product editor and clear carried-over search focus/keyboard when opening it.
- Wait for navigation controls in the Android journey instead of clicking before the editor has closed.
- Retain actual PNG screenshots and semantics through Gradle additional test output rather than querying an already-uninstalled app.
- GitHub Actions run 38496681250 passed unit tests, lint, manifest checks, APK assembly and the complete Android 13 journey on commit 6025f96. Physical-device and representative-user checks remain unverified.

## 0.1.0 — 2026-10-13 (unverified source implementation)
Initial native Compose application, grocery catalogue, four shop lists, custom products, independent favourites, SQLite persistence, validated local backups, list reuse, automated test sources and project documentation. APK build and runtime validation pending.
