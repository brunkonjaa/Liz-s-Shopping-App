# Features

This page describes the current feature set and what has been checked so far. See [TESTING.md](TESTING.md) for commands, results and open coverage.

## Implemented

- Offline shared catalogue with stable product identity, categories, varieties, package sizes and optional image metadata.
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

For Task 12, 55 unit tests passed, lint reported 0 errors and 16 warnings, and the signed Release APK assembled successfully. We installed it on a Samsung SM-A127F running Android 13 and inspected the main screens. The connected shopping journey failed at its initial Lidl card selector, and API 37 remains unverified. The [Task 12 report](TASK12_VISUAL_REFINEMENT_REPORT.md) includes screenshots and details.

Previous items are an undated reusable collection. Catalogue package sizes are suggestions, not stock guarantees. No pricing, purchase history, cloud sync or online purchasing is implemented.

The remaining checks include the full editor, custom-shop, image and backup flows; TalkBack; larger fonts; landscape; reboot and update persistence; representative-user review; API 37 runtime; and steady-state performance. A passing journey covers only the actions it exercises.
