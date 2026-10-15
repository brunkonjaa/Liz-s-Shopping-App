# Architecture

Liz's Shopping List is a native Kotlin/Jetpack Compose, offline-first application with one Compose activity, one state-owning ViewModel and a small platform SQLite snapshot store. The package/application ID remains `ie.suric.lizshopping`.

## State model

- `Product` is a reusable catalogue definition with a stable `id`, name, category, varieties, package sizes and optional durable image metadata. Built-in product IDs are deterministic; user products receive a persisted custom ID.
- `Item` is a shopping-list/favourite/previous configuration. It keeps its own ID, package count, variety, size, notes, completion state and optional `productId`; editing a list item cannot mutate a favourite default.
- `Shop` has a stable ID, display name and built-in flag. Lidl, Aldi, Dunnes Stores and SuperValu are seeded and cannot be removed. User shops are persisted and can be renamed; removal is refused while associated data exists.
- `ShopData` remains shop-scoped, with independent items, favourites, previous items and explicit catalogue associations.

The shared catalogue is `Catalogue.products + ShoppingState.custom`; product definitions are not deleted when list entries are removed. Shop state is keyed by stable shop ID, while the Compose layer presents display names.

## Persistence and migration

`Storage.kt` retains one bounded JSON snapshot in private SQLite. The database schema is now version 2. Existing schema-version-1 rows are decoded with the permanent v1 decoder, converted to stable product/shop references, validated, encoded as v2 and committed by the SQLite upgrade transaction. A failed conversion throws before the database version can advance, leaving the legacy payload available for recovery.

Backup format v2 contains shop definitions, catalogue products, stable IDs, shop associations, lists, favourites, previous items, quantities, completion, notes and image metadata. Valid v1 backups remain importable. Every restore is fully bounded and validated before `LocalStore.save` performs transactional replacement.

## Images

Bundled products use stable symbolic/image references and an emoji fallback. User-selected PNG, JPEG or WebP content is read through the Android document picker, bounded to 256 KiB and 4096px dimensions, copied under `files/product-images/` with a generated filename, and never stored as an untrusted path. Small image metadata is included in v2 backups so a restored product does not silently depend on the original picker URI. Missing files remain safe to render with a fallback.

## UI boundaries

`MainActivity.kt`, `Components.kt` and `Theme.kt` preserve the Task 3 warm editorial design, drawer, themes, catalogue, favourites and list flows. Dynamic shop definitions are shown through the existing home cards and drawer. The current focused product editor exposes the document picker without introducing a new navigation framework. Comprehensive shared bottom-sheet workflows remain Task 4.

## Deliberate constraints

No Room, network service, account, analytics, price, broad storage permission or release signing key was added. Database operations remain off the UI thread behind the ViewModel mutex. Package size remains separate from package count, with quantities constrained to 1–999.
