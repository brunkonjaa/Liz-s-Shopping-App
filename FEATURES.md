# Features

## Done

- Shared offline catalogue with stable product identity, reusable product definitions, varieties, package sizes and optional image metadata.
- Four protected default shops plus persisted custom shops with stable IDs, rename support and data-loss safeguards on removal.
- Independent All Items, Favourites, Shopping List and previous-item areas per shop.
- Product references are reused across shops; list quantities, completion and favourite defaults remain independent.
- Package count is separate from package size; valid counts are 1–999.
- Search, category filters, custom product creation and duplicate prevention for the catalogue.
- SQLite snapshot persistence with explicit schema v1-to-v2 migration.
- Versioned v2 JSON backup/restore with validated v1 import, bounded input and transactional replacement.
- Offline document-picker image import copied into app-private storage with safe generated references and fallback metadata.
- Warm Task 3 themes, editorial landing page, drawer and existing shopping flows are preserved.

## Intentional limits / remaining work

- Previous items are a reusable per-shop collection, not dated purchase history.
- Product sizes are suggestions, not verified stock. There are no prices, cloud features, notifications or online purchasing.
- The UI currently uses the product symbol/fallback path for most catalogue thumbnails; a broader photographic inventory is outside this recovery task.
- Comprehensive shared bottom-sheet workflows remain Task 4.
