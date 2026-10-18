# Features

This page describes the current feature set and what has been checked so far. See [TESTING.md](TESTING.md) for commands, results and open coverage.

The approved target behaviour and design rules are consolidated in [APPROVED_REDESIGN_CONTRACT.md](APPROVED_REDESIGN_CONTRACT.md).

## Implemented

- Offline shared catalogue with 890 grocery suggestions, stable product identity, categories, descriptions, pictogram thumbnails, varieties, package sizes and optional user image metadata. Research-derived entries are shown under the shop catalogues they came from and are not stock guarantees.
- Four protected default shops plus persisted custom shops with validated names, rename support and conservative removal safeguards.
- Shop-scoped catalogue associations, All Items, Favourites, Shopping List and reusable previous items.
- Catalogue Add adds one package; favourite Add reuses the favourite count. List counts, completion and favourite defaults remain independent. Counts are 1-999, separate from package size.
- Search, category filters, custom product creation and catalogue reuse/duplicate prevention.
- Warm light/dark/system themes, editorial landing page, drawer and current navigation tabs.
- Distinct offline section artwork, correctly mapped default shop logos throughout navigation, optional custom-shop images, sorted All Items, and a portrait launcher/background treatment.
- Shared bottom sheets for shop selection/management, product configuration, options and confirmations; named quantity, completion and favourite actions.
- Private SQLite v3 snapshots, explicit v1/v2 migrations, bounded validated v1/v2/v3 JSON backup/restore and transactional replacement.
- Bounded offline document-picker image import with durable private references and safe fallbacks.

## Verified and remaining

The current `main` head `8b19b50` (tag `v1.0.1`) passed GitHub Actions run [38918113185](https://github.com/brunkonjaa/Liz-s-Shopping-App/actions/runs/38918113185) on 2026-10-16. Both jobs succeeded: unit tests, lint, debug APK assembly, packaged-permission/exported-component validation, and the Android 13 journey on the hosted emulator. This verifies that commit and the tested workflow; it does not establish every editor, image, backup, accessibility or device scenario.

The signed v1.0.0 release remains the latest published APK until v1.0.1 is built with the existing signing identity and published. For Task 12, 55 unit tests passed, lint reported 0 errors and 16 warnings, and the signed Release APK assembled successfully. A Samsung SM-A127F running Android 13 was used for the documented visual review and journey. API 37 remains unverified. The [Task 12 report](TASK12_VISUAL_REFINEMENT_REPORT.md) includes details.

Previous items are an undated reusable collection. Catalogue package sizes are suggestions, not stock guarantees. No pricing, purchase history, cloud sync or online purchasing is implemented.

The remaining checks include the full editor, custom-shop, image and backup flows; TalkBack; larger fonts; landscape; reboot and update persistence; representative-user review; API 37 runtime; and steady-state performance. A passing journey covers only the actions it exercises.
