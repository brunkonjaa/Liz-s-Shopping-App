# Storage and backup migration

## Database

The private SQLite database is schema version 2. Version 1 contains one `state` row with a version-1 JSON payload. Opening a version-1 database invokes `LocalStore.onUpgrade`, decodes the old payload, assigns stable product references, seeds stable shop definitions, rebuilds shop associations, validates the result and writes one version-2 payload inside the SQLite upgrade transaction.

If decoding, conversion or validation fails, the upgrade throws. No empty replacement is written and the source payload is not deliberately deleted or advanced as a recovery shortcut. The application reports a controlled load failure so a valid manual backup can be restored.

## JSON backups

Version 1 backups remain importable. New exports use version 2 and include stable product/shop IDs, catalogue definitions, associations, list/favourite/previous entries, quantities, completion, notes and image metadata. Inputs remain bounded to 1 MiB and are fully validated before the restore confirmation is shown.

Custom image bytes are bounded and included as base64 metadata in v2 when available; the corresponding app-private generated reference is never treated as a portable filesystem path. A missing local image file therefore falls back safely, while a v2 restore can retain the portable bytes for the product.

## Compatibility rules

- The four default shops are always present and keep their IDs.
- List and favourite IDs, quantities and completion states are preserved.
- Product definitions are shared; removing an item does not remove its product.
- Custom shops are only removed when their lists, favourites and previous items are empty.
- Future changes require a new explicit schema/backup version and tests; destructive upgrades are not permitted.
