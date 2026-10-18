# Liz’s Shopping List — Approved Redesign Contract

**Status:** Approved product and interaction requirements for future changes. This document consolidates the supplied visual, bottom-sheet, product-catalogue and redesign-plan files. It describes the target contract; it does not claim every requirement is implemented.

## Product intent

Liz’s Shopping List is an offline household shopping app for Lidl, Aldi, Dunnes Stores and SuperValu, with optional custom shops. It should make the weekly shop quicker and easier while keeping the app calm, clear and pleasant to use. Shopping, lists, favourites and product records remain local. Product pricing, online ordering, stock claims, accounts, analytics and cloud sync are outside scope.

## Approved visual direction

- Warm, soft, inviting and polished, with grocery imagery that supports scanning instead of competing with it.
- Cream, oat, ivory, beige and stone surfaces; restrained sage or olive, honey or caramel, and muted terracotta accents; warm charcoal text.
- Clean readable UI typography with distinctive editorial headings such as **SHOP**, **FAVOURITES**, **MY LIST** and **ALL ITEMS**.
- Real-world grocery or lifestyle photography for banners and small product thumbnails where it helps recognition. Use a neutral or category fallback when a suitable image is unavailable.
- Rounded surfaces, gentle separation, restrained shadows, coherent icons and comfortable touch targets. Keep lists compact enough to scan in a shop.
- Light and dark themes share the same warm identity. Light is the primary visual reference; dark uses warm neutrals rather than cold blue-grey.
- The Home page visibly offers all configured shops. A left drawer provides Home, shops, Favourites, the current list, settings and theme controls. Shop switching must remain available without relying on the drawer alone.

The seven supplied screenshots are visual references, not required bundled assets. Supermarket marks in them do not grant image rights or confirm product availability. Do not add reference images to the APK or download imagery at runtime without a separate decision.

## Navigation and interaction

Use normal navigation to open main areas. Use a modal bottom sheet when a task needs selection, creation, configuration or editing and can stay on the current screen. Sheets must follow one shared visual and behavioural convention.

### Immediate actions

These normally happen in place with immediate feedback: ticking a list item, toggling a favourite, changing a visible quantity, expanding an inline section, and opening a main area.

### Contextual sheets

Use a sheet for shop selection or management; product search, creation and configuration; quantity, variety and unit selection; list-entry editing; favourite management; category selection or creation; and contextual list actions. Shop creation should offer Dealz, Euro Shop, European Shop, Spar and Centra as suggestions, plus custom-name entry. Suggested shops are not treated as stocked shops until the user adds them.

### Shared sheet behaviour

- Size sheets to their task: small for a few choices, medium for a common edit, expandable and scrollable for multi-field forms.
- Keep actions reachable while the keyboard is visible. Android Back closes the active sheet before leaving its screen.
- Provide a visible close or cancel action where useful. Swipe or outside dismissal is allowed only when it cannot silently discard meaningful input.
- Cancel leaves saved data unchanged. Save validates before committing. Confirm only destructive actions that remove meaningful user data.
- Prevent duplicate submissions and preserve the underlying screen and shop context when a sheet closes.
- Use accessible labels and state descriptions, readable contrast, coherent focus order and practical touch targets. Do not communicate state through colour alone.

## Product and shopping data rules

Keep shared product definitions separate from shop-specific associations, favourites and shopping trips.

| Record | Required meaning |
| --- | --- |
| Product | Stable identity, canonical name, category, selectable varieties/sizes/units, optional description and optional image reference. Shared across shops. |
| Shop | Stable identity and display name. Preserve the four original shops; allow suggested or user-created shops where the feature is implemented. |
| Shop association | An explicit user choice that a product belongs in that shop’s catalogue. It is not a claim about current local stock. |
| Favourite | A shop-scoped product reference and its own saved configuration/count. Editing a list entry must not change favourite defaults. |
| Shopping-list entry | A shop/list-scoped product reference, selected variety and size/unit, package count, optional note and completion state. |
| Image | A bundled resource key or durable app-private file reference. Missing images use a safe fallback. |

Search the shared catalogue before offering creation. Match case-insensitively after trimming whitespace, but do not silently merge meaningful variants (for example, cherry and beef tomatoes). Reuse product details and images when appropriate. Keep package count separate from package size or loose weight. Use product-appropriate defaults while allowing valid user choices. Ticking or removing a list entry must not alter or delete the reusable product.

Product search, reuse, creation, list editing, favourites, quantities and saved information must work offline. Bundled data is available offline. User-selected images must be copied into app-private storage under generated names, bounded and validated, and referenced durably; do not rely on a temporary picker URI. Keep image loading out of the scrolling path. Do not embed image bytes in portable JSON backups.

## Data safety and compatibility

- Inspect the current persistence and backup formats before changing models or schemas.
- Preserve existing list items, favourites, previous items, custom products/shops, selected shop and user settings during upgrades.
- Use explicit, transactional migrations. On a migration failure, preserve the old data and expose a recoverable error; never replace it with an empty state.
- Keep import size bounded at 1 MiB unless a separately justified change is approved. Validate the complete backup—including IDs, references, counts, shops, products, images and supported version—before replacing saved data.
- Restore atomically. Invalid or unsupported input must leave existing data unchanged.
- Keep old backup import support when introducing a newer export format; document compatibility and test round trips.
- Do not introduce new permissions, servers, accounts, payments, analytics, permanent signing keys or network-backed product databases without explicit approval.

## Acceptance checks for future feature work

1. **Reuse:** Search an existing product from a shop, select it, and verify its name, image, category and options are reused in the correct shop list.
2. **Create:** Create a genuinely new product with valid details and an optional image; confirm it is available in later catalogue searches and after app restart.
3. **Cross-shop:** Reuse one product in another shop while preserving independent list quantities and shop-specific favourites.
4. **Duplicates:** Search using case and whitespace variants; surface matching products without silently duplicating or merging distinct products.
5. **Offline:** Disable connectivity, search, reuse, create, add, edit and reopen saved data successfully.
6. **Image durability:** Select an image, save, restart, and verify it remains available; verify a missing or unreadable image falls back safely.
7. **Sheets:** Check save, cancel, Back, outside/swipe dismissal, validation, duplicate taps, keyboard visibility, scrolling and preserved screen/shop context.
8. **Accessibility/layout:** Review both themes, TalkBack labels/focus, contrast, 360 dp width, portrait and landscape, and 100%, 150% and 200% font scaling.
9. **Recovery:** Test old-format migration, backup round trips, invalid and oversized imports, and rollback on a failed migration/restore.

Build success alone does not establish visual or interaction acceptance. Record device-only and representative-user checks separately from automated evidence.

## Current implementation and verification

The repository’s current implementation includes the offline grocery catalogue, shop-specific lists and favourites, custom shops, local SQLite snapshots and migrations, bounded validated backups, and app-private image storage. Task 12’s visual work and the researched catalogue expansion are on `main`. The implementation has documented remaining UI, accessibility and device checks; those gaps remain governed by [Testing](TESTING.md) and [Features](FEATURES.md).

The supplied source documents were `Liz’s Shopping List_Approved Visual Style Guidelines.md`, `BOTTOM_SHEET_INTERACTION_GUIDELINES.md`, `PRODUCT_CATALOGUE_AND_REUSE_GUIDELINES.md` and `REDESIGN_PLAN.md`. Future changes should update this contract when an approved requirement changes.
