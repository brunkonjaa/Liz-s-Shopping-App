# Storage and backup migration notes

## Current versions

| Layer | Version 1 | Version 2 |
|---|---|---|
| SQLite `user_version` | One `state` table and name-keyed JSON snapshot | Same table and row, versioned ID-based JSON snapshot |
| JSON `version` | Four name-keyed shops; copied item values; custom product names/options | Stable shops/products/categories; explicit associations and shop-scoped product references |
| Portable backup | Export/import v1 | Export v2; continue importing v1 |

The Android application ID remains `ie.suric.lizshopping`. The SQLite file and table are retained; no migration drops or recreates saved tables.

## V1 mapping

1. Validate the complete v1 JSON payload and all four expected shop records.
2. Map the original shop names to `shop:lidl`, `shop:aldi`, `shop:dunnes-stores` and `shop:supervalu`.
3. Map built-in catalogue products by their known names to stable bundled IDs. Map v1 custom products and any uncatalogued legacy items to deterministic `product:legacy:<sha256-prefix>` IDs based on normalized names.
4. Preserve existing item IDs, product names, variety, size, package count, completion state, favourite default quantities, previous items and selected shop. Assign each entry the stable shop/product references it lacked. Preserve custom product options and assign the `Other` category when v1 had no category.
5. Validate the complete v2 state before writing it to the existing snapshot row.

The mapping is deterministic across application launches and backup imports. Similar names are not automatically merged; names already present in the v1 data map according to exact case-insensitive catalogue identity. Search suggestions are separate from migration identity.

## Failure and recovery

Android `SQLiteOpenHelper` runs `onUpgrade` inside a transaction. The migration reads the source row, converts and validates in memory, serializes v2, then updates the row. An exception aborts the transaction, including SQLite's version update, leaving the v1 payload available for a future app fix or recovery. The app reports a load error and does not initialize an empty replacement state.

External backup import validates and converts before presenting the existing replacement confirmation. Cancel does not write. Confirm writes one validated snapshot transactionally. Invalid, malformed, unsupported-version, inconsistent-reference and over-1-MiB backups leave current data untouched. Keep a copy of the original backup until restore is confirmed successful.

Downgrading a v2 database is deliberately rejected; an older application cannot understand the expanded payload. Recovery is to reinstall the newer compatible app and import a valid backup. Before upgrading, users who need a portable recovery point should export a v2 JSON backup. The app retains no second hidden copy of every historical snapshot.

## Image portability and rollback

Local image bytes are app-private files and are intentionally excluded from JSON backups. Portable product records encode a local image as a category fallback, never as a device path or temporary picker URI. If a local file disappears or fails resolution, UI code should show the generic category image. Importing a backup does not attempt to fetch or restore image bytes.

When replacing a product image, import and validate the new image first, persist the product reference, then call reference-aware deletion for the old key. If state persistence fails, keep the old file and remove only the newly staged/unreferenced file. Deletion is refused while any product still references a file. This ordering keeps the existing image recoverable across a failed replacement.

## Validation evidence

`StorageTest` contains the version-1 fixture, field-preservation assertions, SQLite user-version upgrade, simulated malformed migration rollback, v1 backup import, v2 round-trip, invalid-backup preservation and local-image fallback cases. See `TESTING.md` for commands and fresh run results.
