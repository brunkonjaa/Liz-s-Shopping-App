# Features (implemented in source; execution pending)
- Four large shop choices and last-used hint.
- Separate All Items, Favourites and Shopping List tabs per shop.
- Search and compact horizontal category filters.
- Generic catalogue, product emoji, editable variety/size fields and 1–999 package count.
- Custom product creation with only a required name, saved for reuse.
- Shop-specific favourites with independent configurations, edit, remove and one-tap add.
- Shopping list edit, remove, plus/minus, completion/undo and completed entries at the bottom.
- Duplicate merging by normalized name, variety and size.
- Confirmed fresh list and completed clearing; previous-item reuse preserves favourites.
- SQLite persistence for lists, favourites, custom products, completion and last shop.
- User-picked JSON backup/export and validated, confirmed replacement restore.

## Intentional limits
One previous-item collection per shop, not dated history. Quantity means package count; loose weight is typed into package size (e.g. 375 g ×1). Presets are not stock data. No cloud, notifications, online purchasing or prices. Product images use system emoji rather than photographic assets.
