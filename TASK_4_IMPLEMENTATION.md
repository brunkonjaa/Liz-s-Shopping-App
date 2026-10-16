# Task 4 implementation

## Current status - 2026-10-16

Task 4 added the shared sheets and workflows described below. The current test results and remaining sheet, keyboard and accessibility checks are in [TESTING.md](TESTING.md) and the [Task 12 report](docs/TASK12_VISUAL_REFINEMENT_REPORT.md). Screenshots from the earlier work show only the states captured at that time.

## Shared interaction layer

Task 4 keeps the single-activity Compose architecture and adds `LizBottomSheet` in `Components.kt`. It applies the warm cream/ivory/oat surfaces, olive/sage/caramel accents, rounded upper corners, keyboard insets, scrollable content, 48dp-class touch targets and a consistent drag handle. `Task4Sheets.kt` contains reusable shop selection, shop creation/rename/removal, shopping-options and confirmation sheets.

## Workflows

- Change shop uses stable shop definitions and visibly marks the selected row; shop lists and favourites remain keyed by shop ID.
- Add shop offers Dealz, Euro Shop, European Shop, Spar, Centra and validated custom names. Duplicate names are rejected before persistence.
- Product configuration uses the existing catalogue varieties and sizes, quantity controls constrained to 1-999, optional notes, category selection for custom products and the offline image picker.
- The product editor searches matching catalogue products before custom creation, preserving reusable product IDs and shop-specific favourite state.
- A heart control on catalogue cards toggles the current shop's favourite immediately. List edits, favourite edits and custom product saves remain separate operations.
- Shopping options and custom-shop removal use confirmation sheets for destructive actions. A custom shop is not removable while it still contains list, favourite or previous data.

## State and validation conventions

Forms validate before their save callback; validation failures leave the sheet open. `Rules.category` reuses an existing category case-insensitively after trimming, while `Rules.validateCategoryName` bounds new category names. `ShoppingViewModel` continues to serialize persistence behind its mutex and reports save failures without replacing the in-memory state with an empty state.

## Accessibility and verification boundary

Important selected-shop state is expressed with radio controls and text, not colour alone. Quantity buttons, favourite buttons, drawer entries and list controls expose content descriptions. Unit tests cover category normalization, quantity bounds, shop identity and existing persistence/migration invariants. The connected journey and representative sheet screenshots were verified on the earlier Samsung SM-A127F / Android 13 baseline; the maintained current journey has now passed repeated Samsung runs and GitHub Android 13 CI. This does not re-verify every sheet/editor/management flow. API 37 emulator limitations, large-font/TalkBack checks and device-level persistence after restart remain open.
