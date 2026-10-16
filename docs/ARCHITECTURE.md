# Architecture

The app is a native Kotlin/Jetpack Compose project (`ie.suric.lizshopping`) with one activity, one state-owning ViewModel and a private SQLite snapshot store. It has no server or account layer.

## State and identity

`Product` is a reusable catalogue definition with stable ID, name/category, short description, pictogram symbol, varieties, package sizes and optional image metadata. Built-in IDs are deterministic; custom products receive persisted IDs. Research entries retain a list of the source shops for the All Items filter; the list is a research origin, not a stock guarantee. The built-in catalogue combines the original essentials with deduplicated generic entries from four Ireland supermarket research lists.

`Item` is an independent list/favourite/previous configuration with its own ID, package count, variety, size, notes, completion and optional product reference. Its match key includes product/name, variety and size. List edits cannot change a favourite default.

`Shop` has a stable ID/name and built-in flag, plus optional private image reference and bounded backup bytes. Default logo selection uses stable built-in IDs; custom shops use their saved image or a neutral icon. The four defaults cannot be removed. Custom-shop removal is refused while list, favourite or previous data exists. `ShopData` remains keyed by stable shop ID, keeping all collections and catalogue associations independent.

Catalogue Add uses saved variety/size or valid catalogue defaults, always adding one package. Favourite Add uses the saved favourite count. Counts remain 1-999 and separate from package size.

## Updates and persistence

`ShoppingViewModel` updates the UI promptly, then serializes writes behind a mutex and saves on `Dispatchers.IO`. A failed write restores the preceding in-memory state and shows an error. A load failure leaves the saved snapshot available for recovery.

`LocalStore` holds one validated JSON snapshot in SQLite schema v3. Explicit v1/v2-to-v3 migrations decode and validate the old state before writing inside SQLite's upgrade transaction. Unsupported or invalid upgrades throw; no destructive fallback is used. New backups use v3 while v1 and v2 remain importable.

Restore is bounded to 1 MiB and fully decoded/validated before confirmation and transactional replacement. Descriptions are an optional v3 product field, so older v3 backups remain readable. See [MIGRATION.md](MIGRATION.md).

## UI and images

`MainActivity.kt`, `Components.kt`, `Task4Sheets.kt` and `Theme.kt` own the current home/drawer/tab, catalogue/favourite/list and sheet surfaces. Returning users open the selected shop list; first-time users start at Home. Drawer visibility is transient. Routes render current state directly; the earlier Task 7 route Crossfade is historical and is no longer the current route implementation.

`LizBottomSheet` supplies shared Material 3 surfaces. Editor fields retain catalogue reuse, category validation, quantity/notes, favourite and optional image handling. Document-picker image bytes are bounded and copied to generated app-private filenames; metadata supports backup and fallback rendering.

Section artwork is packaged locally and mapped by route and title. Headings use heading-aware line breaking, and the Shop Management heading has enough width to keep each word intact. All Items sorts product names without regard to case. Liz's portrait appears in the launcher, Android 12+ system splash, Home banner beside a grocery basket, and a low-opacity app background. Shop cards, drawer entries, the app bar and management sheets share the `ShopMark` resolver. User-selected shop images decode off the main thread.

## Verification boundary

`JourneyDiagnostics` exists only in androidTest. It observes ViewModel/message flows, reads committed SQLite with OPEN_READONLY, and captures semantics, accessibility XML, screenshots, window/IME and logs. Optional method tracing affects scheduling and is a separate experiment. Successful injection is not proof of callback delivery; optimistic publication is not proof of a durable write.

The maintained journey uses current Tab semantics, safe fixture preconditions, scrolling/displayed/enabled checks and observable snackbar disappearance. Independent committed SQLite assertions require Aldi followed by Lidl selection with list/favourite counts 7/1; a retained ViewModel after activity recreation is not used as persistence proof. Production code was unchanged during Task 5; consolidation also includes the pre-existing state/count commits from PR #2. The latest Task 12 journey attempt did not pass its initial shop-card selector. See [TESTING.md](TESTING.md) for current results.
