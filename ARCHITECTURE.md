# Architecture

## Runtime structure

- `MainActivity.kt` contains the existing Compose shopping flows and Android document-picker entry points.
- `Catalogue.kt` supplies the offline bundled product definitions and stable built-in category/product IDs.
- `Model.kt` owns shops, product/category identities, list-entry snapshots, validation, matching, and pure business rules.
- `ShoppingViewModel.kt` serializes state updates with a mutex, performs persistence off the main thread, and exposes domain operations to Compose.
- `Storage.kt` stores one validated versioned JSON snapshot in a private SQLite row and implements both the database and backup migrations.
- `ProductImageStore.kt` copies and validates selected local image content into private storage and resolves missing references to category fallbacks.

The application remains offline-first. It uses platform SQLite rather than adding Room: the app works with one small in-memory state snapshot and has no query workload that justifies a second persistence layer. Updates and restore commit the full snapshot in a SQLite transaction; the ViewModel publishes state only after persistence succeeds.

## State ownership

`Product` is the reusable definition: stable ID, name, stable category ID, image reference, supported varieties, sizes, units and description. Bundled products are shared across all shops. User products are stored once in the shared custom catalogue. Product name is not used as the persistent relationship key.

`Shop` has a stable ID independent of its display name. The four original shops use fixed IDs; custom shops use generated IDs. Each shop ID owns a separate `ShopData`, including active entries, favourites, previous items and explicitly selected product associations. The association means the user chose it; it does not assert verified inventory. A custom shop is archived instead of physically deleted so its data stays recoverable. Built-in shops cannot be renamed or removed.

`Item` stores an entry ID, shop ID, product ID, selected variety and size, package count, completion state and optional note. Size is one package; quantity is how many packages. Entry edits and completion do not mutate a product definition. Removing an entry does not remove its product. Adding the same product/configuration merges only within the same shop.

Search is local and returns ranked candidates. It trims and case-folds terms, supports partial tokens and common singular/plural variants, and never merges products. The user retains the final reuse/create choice.

## Storage and migration

SQLite schema version 1 and the JSON `version: 1` snapshot both used four display-name shop keys, product names, and copied `Item` values. Schema version 2 retains the same table and row, but upgrades that payload transactionally to JSON version 2. The migration maps built-ins to fixed IDs and legacy custom names to deterministic SHA-256-based IDs. Existing item IDs, quantities, completion, configurations, favourite defaults, previous items, custom product options, and selected shop are carried forward.

`SQLiteOpenHelper` executes `onUpgrade` in its database transaction. The old payload is read and validated before its row is updated; a decode or write failure aborts the upgrade and leaves both the version and source row intact. There is no destructive fallback. V1 backup import runs the same pure conversion in memory, then remains pending until the existing explicit restore confirmation. Restore replaces state only after full validation and one transactional save.

Version 2 JSON explicitly stores shop IDs, product IDs, categories, product-shop associations and shop-scoped item snapshots. Unknown versions, malformed references, duplicate IDs/names, inconsistent shop ownership, invalid options, oversized payloads and unsupported image references are rejected before replacement. Export/import remains bounded to 1 MiB.

## Product images

An image reference is typed as `bundled`, `local` or `category`. Bundled resource keys and generic category keys are stable app references. User-selected images are copied from Android document-picker streams to `filesDir/product-images`, named with an app-generated UUID and stored only by the generated key. Supported content is JPEG, PNG or WebP, maximum 10 MiB, maximum dimension 4096 and maximum 12 megapixels. The import service runs on `Dispatchers.IO`, checks file signatures and decode bounds, and writes through a temporary file before rename.

The image service never trusts a backup path or a picker URI as a durable key. Missing or corrupt local files resolve to a category fallback. Deletion is allowed only after the product reference is replaced/removed and only when no remaining catalogue product points to the same image. Product bytes are not included in JSON backups; local references are exported/imported as category fallbacks. Backups therefore remain portable and within the 1 MiB limit.

## UI boundary

Task 2 changes data and business logic only. The existing screens still present the four original shops. The approved editorial layout, custom-shop presentation, picker UI and bottom-sheet workflows belong to later tasks. No price, network, account, analytics or external-image behavior is introduced.
