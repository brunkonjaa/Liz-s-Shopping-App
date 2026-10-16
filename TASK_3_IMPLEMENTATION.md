# Task 3 visual redesign

## Current status - 2026-10-16

This records the preserved Task 3 design. Task 4 supplies contextual sheets; later commits supply direct Add/current-state routing. Private visual references remain local and ignored. Current verification supersedes the earlier pending-check note without changing historical design scope.

## Done

- Added warm cream, oat, ivory, olive, caramel and terracotta Material 3 tokens.
- Added warm light, dark and system appearance choices persisted in private preferences.
- Added reusable editorial banner, shop mark/card, product thumbnail/card, favourite card, shopping progress, list row and empty-state components.
- Added landing-page SHOP, FAVOURITES, MY LIST and ALL ITEMS sections.
- Added the modal drawer for home, shops, favourites, current list, all items, settings and appearance controls.
- Redesigned catalogue search, category chips, favourites and shopping list while preserving ViewModel operations, quantity rules, shop isolation and offline storage.
- Kept accessible labels and state descriptions for navigation, quantities and completion toggles.
- The Task 3 implementation is preserved. Current lint, APK, unit and Android 13 phone/CI journey checks passed on implementation 265b941; see [TESTING.md](TESTING.md). This verifies exercised interactions, not complete visual-reference fidelity or accessibility.

## Not done yet

- Task 4 now supplies the shared contextual bottom-sheet surface; this document remains the record of the preserved Task 3 editorial UI and navigation boundaries.
- Recently Bought remains omitted because the current previous-item data is an undated reusable set.
- Reference photos are not runtime assets; the app uses offline symbols and the bundled generic hero image.
- Task 2 supplies stable catalogue/shop identities and durable user image references beneath this UI; Task 4 extends those seams without introducing a second navigation architecture.
- The first performance pass is implemented: the home hero is downsampled and shared across home cards. Cold startup, transitions and catalogue scrolling still need separate follow-up measurements and tuning.
