# Storage and backup migration

The current database and backup format is version 3. Task 12 added optional custom-shop image fields while keeping v1 and v2 data readable.

## SQLite

The private database is schema v3, containing one `state` row with a bounded validated JSON snapshot. Explicit v1/v2-to-v3 upgrades decode the prior payload, preserve its lists/favourites/previous items and shop/product identity, validate the converted state and write it inside SQLite's upgrade transaction. Version 2 remains compatible when older shop definitions omit logo fields.

Invalid conversion or unsupported upgrade throws before a successful version advance. There is no destructive fallback, automatic empty replacement or silent recovery deletion. Application load errors preserve the saved payload for recovery.

## JSON restore

New exports are v3; valid v1 and v2 backups remain importable. v3 includes stable shop/product IDs, catalogue definitions, associations, lists/favourites/previous entries, package counts, completion, notes, product image metadata and optional custom-shop image metadata. Import reads are bounded to 1 MiB. Decode validates versions, identities, references, collection/text limits, quantities and image bounds before confirmation and transactional replacement.

Custom image bytes are bounded and represented as metadata/base64 where available. A generated app-private image reference is not assumed to be a portable external path; missing images have a safe fallback.

## Invariants and verification

The four default shops and IDs remain protected; list/favourite identity and counts stay independent; removing an item does not remove its product. Custom-shop removal is refused while associated list/favourite/previous data exists.

The current unit suite includes 19 storage tests and 36 rules tests. It covers v1 migration, v2 backup compatibility, the v2-to-v3 SQLite migration, shop-image backup round-tripping and preservation of invalid state. The local unit, lint and Release build checks passed. Physical-device upgrade and document-picker restore journeys remain unverified; see [TESTING.md](TESTING.md). Any future schema change needs an explicit, tested migration. Destructive upgrades are prohibited.
