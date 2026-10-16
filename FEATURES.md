# Features

Current implementation and evidence boundary: 2026-10-16. See [TESTING.md](TESTING.md).

## Implemented

- Offline shared catalogue with stable product identity, categories, varieties, package sizes and optional image metadata.
- Four protected default shops plus persisted custom shops with validated names, rename support and conservative removal safeguards.
- Shop-scoped catalogue associations, All Items, Favourites, Shopping List and reusable previous items.
- Catalogue Add adds one package; favourite Add reuses the favourite count. List counts, completion and favourite defaults remain independent. Counts are 1-999, separate from package size.
- Search, category filters, custom product creation and catalogue reuse/duplicate prevention.
- Warm light/dark/system themes, editorial landing page, drawer and current navigation tabs.
- Shared bottom sheets for shop selection/management, product configuration, options and confirmations; named quantity, completion and favourite actions.
- Private SQLite v2 snapshots, explicit v1 migration, bounded validated JSON backup/restore and transactional replacement.
- Bounded offline document-picker image import with durable private references and safe fallbacks.

## Verified and remaining

The current journey passes repeated Samsung Android 13 execution and GitHub API 33 x86_64 Pixel 2 CI. It verifies direct Add, quantities, completion, favourite independence, shop isolation and activity recreation. Diagnostics preserve UI/semantics, state, committed readback and logs at failures.

Previous items are an undated reusable collection. Catalogue package sizes are suggestions, not stock guarantees. No pricing, purchase history, cloud sync or online purchasing is implemented.

Complete editor/custom-shop/image/backup UI coverage, TalkBack, large fonts, landscape, reboot/update persistence, representative-user review, API 37 runtime and steady-state performance profiling remain unverified. A green journey does not imply complete feature acceptance.
