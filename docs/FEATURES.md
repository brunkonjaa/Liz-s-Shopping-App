# Features

This page describes the current feature set and what has been checked so far. See [TESTING.md](TESTING.md) for commands, results and open coverage.

The approved target behaviour and design rules are consolidated below under [Approved future design contract](#approved-future-design-contract).

## Implemented

- Offline shared catalogue with 890 grocery suggestions, stable product identity, categories, descriptions, pictogram thumbnails, varieties, package sizes and optional user image metadata. Research-derived entries are shown under the shop catalogues they came from and are not stock guarantees.
- Four protected default shops plus persisted custom shops with validated names, rename support and conservative removal safeguards.
- Shop-scoped catalogue associations, All Items, Favourites, Shopping List and reusable previous items.
- Catalogue Add adds one package; favourite Add reuses the favourite count. List counts, completion and favourite defaults remain independent. Counts are 1-999, separate from package size.
- Search, category filters, custom product creation and catalogue reuse/duplicate prevention.
- Warm light/dark/system themes, editorial landing page, drawer and current navigation tabs.
- Distinct offline section artwork, correctly mapped default shop logos throughout navigation, optional custom-shop images, sorted All Items, and a portrait launcher/background treatment.
- Shared bottom sheets for shop selection/management, product configuration, options and confirmations; named quantity, completion and favourite actions.
- Private SQLite v3 snapshots, explicit v1/v2 migrations, bounded validated v1/v2/v3 JSON backup/restore and transactional replacement.
- Bounded offline document-picker image import with durable private references and safe fallbacks.

## Approved future design contract

This section records the approved target for future changes; it does not claim every requirement is implemented.

### Visual design and navigation

- Keep the app calm, warm, inviting and polished. Use cream, oat, ivory and beige surfaces; restrained sage or olive, honey or caramel, muted terracotta accents and warm charcoal text.
- Use readable UI typography with distinctive editorial headings such as **SHOP**, **FAVOURITES**, **MY LIST** and **ALL ITEMS**. Use warm real-world grocery imagery and small product thumbnails where they improve recognition; keep imagery behind the task and use neutral/category fallbacks when assets are unavailable.
- Use rounded surfaces, restrained shadows, coherent icons and comfortable touch targets. Keep lists compact enough to scan while shopping.
- Preserve a matching light and dark theme; light is the visual reference and dark uses warm neutrals rather than cold blue-grey.
- Home must show all configured shops. The collapsible left drawer provides Home, shops, Favourites, the current list, settings and theme controls. Shop switching remains available outside the drawer.
- Suggested shop choices are Dealz, Euro Shop, European Shop, Spar and Centra, plus custom-name entry. A shop association is an explicit user choice, not a stock guarantee.

### Sheets and actions

Use normal navigation for main areas and a shared modal bottom-sheet pattern for contextual selection, creation, configuration and editing. Small sheets suit a few choices; medium sheets suit common product/list edits; multi-field forms expand and scroll.

- Simple one-tap actions remain immediate: tick an item, toggle a favourite, change a visible quantity, expand a section or open a main area.
- Android Back closes the active sheet first. Provide visible close/cancel controls where useful, preserve screen and shop context, and keep actions reachable while the keyboard is visible.
- Swipe/outside dismissal is safe only when it cannot silently discard meaningful input. Cancel leaves saved data unchanged; validate before saving; confirm destructive actions that remove user data; prevent duplicate submissions.
- Keep labels, focus order, state descriptions, contrast and touch targets accessible. Do not convey state by colour alone.

### Product and list semantics

Keep shared product definitions separate from shop-specific associations, favourites and individual shopping trips.

| Record | Meaning |
| --- | --- |
| Product | Stable identity, name, category, selectable varieties/sizes/units, optional description and optional image reference; reusable across shops. |
| Shop association | Explicit user-selected relationship. It does not assert current local stock. |
| Favourite | Shop-scoped product reference and its own saved configuration/count. List edits do not alter favourite defaults. |
| Shopping-list entry | Shop/list-scoped product reference, chosen variety and size/unit, package count, optional note and completion state. |
| Image | Bundled key or durable app-private file reference with a safe fallback when unavailable. |

Search the shared catalogue before offering product creation. Match case-insensitively after trimming whitespace, but do not silently merge meaningful variants such as cherry and beef tomatoes. Keep package count separate from package size or loose weight. List completion or removal must not change or delete the reusable product. All product and list workflows must remain offline.

For local state, keep selected image bytes in bounded app-private files and persist durable references rather than temporary picker URIs or embedded image bytes. Portable backups must remain self-contained: retain bounded image data in the backup format until an equally portable, versioned archive/sidecar is implemented. Reject invalid or oversized input without replacing saved data.

### Migration, backup and acceptance

- Inspect the current storage format before changing models or schemas. Use explicit transactional migrations; on failure preserve old data and surface a recoverable error, never replace it with an empty state.
- Keep backup imports bounded to 1 MiB unless a separately justified change is approved. Validate supported version, IDs, references, counts, shops, products and images before transactional replacement. Continue importing old formats when a new export format is introduced.
- Verify future changes with: existing-product reuse; custom-product creation and restart persistence; cross-shop reuse with independent lists/favourites; duplicate prevention; offline operation; image persistence and fallback; sheet save/cancel/Back/keyboard/scroll/context; both themes and accessibility/layout; old-format migration, round trips and failed-restore rollback.
- Build success alone does not establish visual or interaction acceptance. Record device-only and representative-user checks separately from automated evidence.

## Verified and remaining

The last verified code baseline before this documentation change was `main` at `8b19b50` (tag `v1.0.1`), which passed GitHub Actions run [38918113185](https://github.com/brunkonjaa/Liz-s-Shopping-App/actions/runs/38918113185) on 2026-10-16. Both jobs succeeded: unit tests, lint, debug APK assembly, packaged-permission/exported-component validation, and the Android 13 journey on the hosted emulator. This verifies that code revision and workflow; it does not establish every editor, image, backup, accessibility or device scenario.

The signed v1.0.0 release remains the latest published APK until v1.0.1 is built with the existing signing identity and published. For Task 12, 55 unit tests passed, lint reported 0 errors and 16 warnings, and the signed Release APK assembled successfully. A Samsung SM-A127F running Android 13 was used for the documented visual review and journey. API 37 remains unverified. The [Task 12 report](TASK12_VISUAL_REFINEMENT_REPORT.md) includes details.

Previous items are an undated reusable collection. Catalogue package sizes are suggestions, not stock guarantees. No pricing, purchase history, cloud sync or online purchasing is implemented.

The remaining checks include the full editor, custom-shop, image and backup flows; TalkBack; larger fonts; landscape; reboot and update persistence; representative-user review; API 37 runtime; and steady-state performance. A passing journey covers only the actions it exercises.
