# Project rules
- Native Kotlin and Compose only. Preserve the simple white/green design.
- Offline household grocery lists; four shops, independent lists and favourites.
- Never add permissions, servers, accounts, analytics, payments or permanent signing keys without explicit approval.
- Keep package size separate from package count. Quantities are 1–999.
- Keep favourite defaults independent from list edits. Preserve saved data on errors.
- Validate complete backups before transactional replacement. Imports are bounded to 1 MiB.
- Run unit tests, lint, build and the emulator journey before claiming a tested APK.
- Maintain the seven project documents. Report unverified behaviour explicitly.
- Do not use destructive schema upgrades. Add and test explicit migrations.
