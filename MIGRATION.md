# Storage and backup migration

Current schema/backup contract - 2026-10-16. The CI correction and documentation consolidation introduce no new schema or backup version.

## SQLite

The private database is schema v2, containing one `state` row with a bounded validated JSON snapshot. Explicit v1-to-v2 upgrade decodes legacy data, assigns stable product/shop references, validates the converted state and writes it inside SQLite's upgrade transaction.

Invalid conversion or unsupported upgrade throws before a successful version advance. There is no destructive fallback, automatic empty replacement or silent recovery deletion. Application load errors preserve the saved payload for recovery.

## JSON restore

New exports are v2; valid v1 backups remain importable. v2 includes stable shop/product IDs, catalogue definitions, associations, lists/favourites/previous entries, package counts, completion, notes and image metadata. Import reads are bounded to 1 MiB. Decode validates versions, identities, references, collection/text limits and quantities before confirmation and transactional replacement.

Custom image bytes are bounded and represented as metadata/base64 where available. A generated app-private image reference is not assumed to be a portable external path; missing images have a safe fallback.

## Invariants and verification

The four default shops and IDs remain protected; list/favourite identity and counts stay independent; removing an item does not remove its product. Custom-shop removal is refused while associated list/favourite/previous data exists.

The current 16-test StorageTest suite and related Rules tests pass, including real SQLite migration/round-trip and invalid-state preservation cases. This does not verify every physical-device upgrade or document-picker restore journey. Those UI/device checks remain open in [TESTING.md](TESTING.md). Future schema changes require a new explicit, tested migration; destructive upgrades remain prohibited.
