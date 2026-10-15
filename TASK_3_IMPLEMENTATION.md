# Task 3 visual redesign

## Done

- Added warm cream, oat, ivory, olive, caramel and terracotta Material 3 tokens.
- Added warm light, dark and system appearance choices persisted in private preferences.
- Added reusable editorial banner, shop mark/card, product thumbnail/card, favourite card, shopping progress, list row and empty-state components.
- Added landing-page SHOP, FAVOURITES, MY LIST and ALL ITEMS sections.
- Added the modal drawer for home, shops, favourites, current list, all items, settings and appearance controls.
- Redesigned catalogue search, category chips, favourites and shopping list while preserving ViewModel operations, quantity rules, shop isolation and offline storage.
- Kept accessible labels and state descriptions for navigation, quantities and completion toggles.
- The implementation is preserved in this baseline; current verification evidence is tracked in `TESTING.md`, and lint, APK assembly and the connected Android journey remain to be rerun after the final Task 2 edits.

## Not done yet

- The comprehensive shared bottom-sheet workflow remains Task 4.
- Recently Bought remains omitted because the current previous-item data is an undated reusable set.
- Reference photos are not runtime assets; the app uses offline symbols and the bundled generic hero image.
- Task 2 now supplies stable catalogue/shop identities and durable user image references beneath this UI; Task 4's shared contextual sheets remain separate.
- The first performance pass is implemented: the home hero is downsampled and shared across home cards. Cold startup, transitions and catalogue scrolling still need separate follow-up measurements and tuning.
