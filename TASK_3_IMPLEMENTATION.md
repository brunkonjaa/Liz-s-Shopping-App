# Task 3 visual redesign

## Current status - 2026-10-16

Task 3 established the app's warm editorial design and navigation. Task 4 later added contextual sheets, and follow-up work added direct Add and current-state routing. Private visual references remain local. Current results are in [TESTING.md](TESTING.md) and the [Task 12 report](docs/TASK12_VISUAL_REFINEMENT_REPORT.md); they do not change the scope of this historical design record.

## Done

- Added warm cream, oat, ivory, olive, caramel and terracotta Material 3 tokens.
- Added warm light, dark and system appearance choices persisted in private preferences.
- Added reusable editorial banner, shop mark/card, product thumbnail/card, favourite card, shopping progress, list row and empty-state components.
- Added landing-page SHOP, FAVOURITES, MY LIST and ALL ITEMS sections.
- Added the modal drawer for home, shops, favourites, current list, all items, settings and appearance controls.
- Redesigned catalogue search, category chips, favourites and shopping list while preserving ViewModel operations, quantity rules, shop isolation and offline storage.
- Kept accessible labels and state descriptions for navigation, quantities and completion toggles.
- The Task 3 implementation remains part of the app. The implementation `265b941` passed the then-current lint, APK, unit and Android 13 phone/CI checks; see [TESTING.md](TESTING.md) for newer results. Those checks covered exercised interactions, not full visual-reference fidelity or accessibility.

## Not done yet

- Task 4 now supplies the shared contextual bottom-sheet surface; this document remains the record of the preserved Task 3 editorial UI and navigation boundaries.
- Recently Bought remains omitted because the current previous-item data is an undated reusable set.
- Reference photos are not runtime assets; the app uses offline symbols and the bundled generic hero image.
- Task 2 supplies stable catalogue/shop identities and durable user image references beneath this UI; Task 4 extends those seams without introducing a second navigation architecture.
- The first performance pass is implemented: the home hero is downsampled and shared across home cards. Cold startup, transitions and catalogue scrolling still need separate follow-up measurements and tuning.
