# Architecture
- MainActivity.kt: native Compose screens, editor, navigation, accessible controls, file picker integration.
- Catalogue.kt: static generic grocery presets; alternatives remain editable.
- Model.kt: immutable data and quantity, merge, edit, favourite and reuse rules.
- ShoppingViewModel.kt: lifecycle state, serialized commands, background I/O, user feedback.
- Storage.kt: versioned JSON codec and transactional private SQLite persistence.

Android's SQLiteOpenHelper stores one bounded versioned snapshot. This deliberately uses the platform database instead of Room: the app has one small in-memory state, no complex queries and no relationships that need a code-generation layer. Transactions protect against partial replacement; serialization validates invariants before commit. Writes finish before UI state is published. A mutex serializes updates and exports. Failed loads do not silently reset data.

All four shops are always present in state. Lists and favourite configurations are separate immutable values. Product identity for merging is normalized name + variety + size. Matches sum package counts up to 999 and reopen a completed entry. Editing into an existing identity merges them. Previous items can be added again; a repeated reuse intentionally increments matching quantities.

Last shop is persisted and marked on the chooser. The chooser appears on cold launch. Selected tabs and search text survive activity recreation; unfinished product drafts do not survive process death. Saved lists do.

SAF document pickers grant access only to user-chosen files. No filesystem path supplied by a backup is interpreted. Restoration is validated before confirmation and persisted atomically. Future database/backup versions require explicit migrations; there is no destructive fallback.
