# Architecture

Current state - 2026-10-16. Native Kotlin/Jetpack Compose, package `ie.suric.lizshopping`, one activity, one state-owning ViewModel and a private SQLite snapshot store. No server or account layer.

## State and identity

`Product` is a reusable catalogue definition with stable ID, name/category, varieties, package sizes and optional image metadata. Built-in IDs are deterministic; custom products receive persisted IDs.

`Item` is an independent list/favourite/previous configuration with its own ID, package count, variety, size, notes, completion and optional product reference. Its match key includes product/name, variety and size. List edits cannot change a favourite default.

`Shop` has a stable ID/name and built-in flag. The four defaults cannot be removed. Custom-shop removal is refused while list, favourite or previous data exists. `ShopData` remains keyed by stable shop ID, keeping all collections and catalogue associations independent.

Catalogue Add uses saved variety/size or valid catalogue defaults, always adding one package. Favourite Add uses the saved favourite count. Counts remain 1-999 and separate from package size.

## Updates and persistence

`ShoppingViewModel` publishes optimistic small changes, serializes writes behind a mutex and saves on `Dispatchers.IO`. A failed write restores the preceding in-memory state and exposes an error message. Load failure leaves the saved snapshot available for recovery.

`LocalStore` holds one validated JSON snapshot in SQLite schema v2. Explicit v1-to-v2 migration decodes and validates the old state before writing inside SQLite's upgrade transaction. Unsupported or invalid upgrades throw; no destructive fallback is used.

Restore is bounded to 1 MiB and fully decoded/validated before confirmation and transactional replacement. See [MIGRATION.md](MIGRATION.md).

## UI and images

`MainActivity.kt`, `Components.kt`, `Task4Sheets.kt` and `Theme.kt` own the current home/drawer/tab, catalogue/favourite/list and sheet surfaces. Returning users open the selected shop list; first-time users start at Home. Drawer visibility is transient. Routes render current state directly; the earlier Task 7 route Crossfade is historical and is no longer the current route implementation.

`LizBottomSheet` supplies shared Material 3 surfaces. Editor fields retain catalogue reuse, category validation, quantity/notes, favourite and optional image handling. Document-picker image bytes are bounded and copied to generated app-private filenames; metadata supports backup and fallback rendering.

## Verification boundary

`JourneyDiagnostics` exists only in androidTest. It observes ViewModel/message flows, reads committed SQLite with OPEN_READONLY, and captures semantics, accessibility XML, screenshots, window/IME and logs. Optional method tracing affects scheduling and is a separate experiment. Successful injection is not proof of callback delivery; optimistic publication is not proof of a durable write.

The maintained journey uses current Tab semantics, safe fixture preconditions, scrolling/displayed/enabled checks and observable snackbar disappearance. Independent committed SQLite assertions require Aldi followed by Lidl selection with list/favourite counts 7/1; a retained ViewModel after activity recreation is not used as persistence proof. Production code was unchanged during Task 5; consolidation also includes the pre-existing state/count commits from PR #2. See [TESTING.md](TESTING.md).
