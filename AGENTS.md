# Project rules
- Native Kotlin and Compose only. Follow the approved warm cream/oat, sage/olive and muted terracotta visual direction while keeping the interface readable and practical.
- Offline household grocery lists; four shops, independent lists and favourites.
- Never add permissions, servers, accounts, analytics, payments or permanent signing keys without explicit approval.
- Keep package size separate from package count. Quantities are 1–999.
- Keep favourite defaults independent from list edits. Preserve saved data on errors.
- Validate complete backups before transactional replacement. Imports are bounded to 1 MiB.
- Run unit tests, lint, build and the emulator journey before claiming a tested APK.
- Maintain the seven project documents. Report unverified behaviour explicitly.
- Do not use destructive schema upgrades. Add and test explicit migrations.

# Documentation and local evidence
- The seven current project documents are the root `README.md` plus `docs/FEATURES.md`, `docs/ARCHITECTURE.md`, `docs/TESTING.md`, `docs/MIGRATION.md`, `docs/SECURITY.md` and `docs/CHANGELOG.md`. Keep this `AGENTS.md` at the repository root. Put approved product and interaction requirements in `docs/FEATURES.md`; do not add a competing current contract document. Link dated task reports to current status, and keep historical measurements clearly dated.
- Keep raw design references, handovers, raw UI dumps and database snapshots ignored. Add only a small set of app screenshots to documentation when the user asks; inspect each image first and keep household data out of captures.
