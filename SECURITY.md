# Security and privacy
## Threat model
Data is an ordinary household grocery list. Relevant risks: malicious or malformed backup files, accidental bulk deletion, interrupted writes, unnecessary exposure and unintended network collection. Rooted/unlocked device access and exported backup access are outside the app's private-storage boundary.

## Controls
- Zero requested Android permissions; no INTERNET permission, network SDK, analytics, ads, secret or server.
- One exported component: launcher activity, required for Android to launch the app. No services or providers.
- Private SQLite database. Parameter-bound ContentValues writes and fixed queries; input never becomes SQL or HTML.
- Atomic snapshot transactions; failed writes leave prior state intact.
- Bounded backup read (1 MiB); known format/version/shops, string length and control-character checks, strict integer quantity and boolean status checks, bounded collections, unique identities.
- Restore confirms replacement only after complete validation. Bulk list operations require confirmation.
- SAF file grants only, no storage permission. Exported files are readable by anyone the user shares them with. No device-wide file scanning.
- Android automatic cloud backup is disabled. Manual backups remain under user control.
- No custom encryption/authentication: private Android app storage is proportionate for this data.

MASVS storage, platform and code principles informed this design. This is not a MASVS certification. Merged manifest, dependency inspection, lint and adversarial input execution remain pending. No permanent signing credentials were generated.
