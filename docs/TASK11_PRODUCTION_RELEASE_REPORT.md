# Task 11 — Production Release, Installation and Performance Report

Date: 2026-10-16. Assessment: **CONDITIONALLY READY**. The signed optimized APK installs and launches without Android Studio, completed the core household shopping journey on the target phone, and produced substantially smoother scrolling measurements than the earlier debug build. Release acceptance remains conditional because several management, backup, accessibility and appearance flows were not exercised, and the signing identity has not been backed up off-device.

## 1. Baseline and release configuration

- Dedicated branch: `task11/production-release`.
- Starting commit: `0f286471f79e6c9a05251d67aa7dc841abc758a` (Task 10 report and Task 1–10 integrated source).
- Application ID remains `ie.suric.lizshopping`; target version is `1.0.0`, code `1`. No existing release tag or higher version code was present in the inspected history.
- `app/build.gradle.kts` configures Release as non-debuggable, R8 optimized/minified, and resource-shrunk, using the optimized Android default ProGuard configuration. No custom keep rule, dependency, app feature, permission, schema, or visual redesign was added.
- Android configuration: min SDK 26, target SDK 36, compile SDK 37. Existing Compose, SQLite, JSON backup/restore and image code built in the optimized variant. The manual Release journey checked core Compose/storage behavior; backup/restore and all compatibility paths were not exercised in Release.
- `.gitignore` excludes `/release-output/`; test traces and private screenshots/XML remain in the already-ignored `/verification/` directory.

## 2. Signing identity and repeat builds

A 3072-bit RSA PKCS12 signing identity was generated outside the repository at `%USERPROFILE%\.lizshopping\release-signing.p12`. The unique random password is DPAPI protected in `%USERPROFILE%\.lizshopping\release-password.dpapi`; directory access is restricted to the current account and SYSTEM. `scripts/build-release.ps1` supplies secrets to Gradle via process environment, prints no credentials, and clears those environment variables when finished. The APK contains no private credential.

`apksigner verify --verbose --print-certs` passed using APK Signature Scheme v2, with one signer. Certificate SHA-256 fingerprint: `446ccafd68ef8a5d2cc4e5caea6a06776cf6c050b2dde62343db8dde90007e39`.

To rebuild on this Windows account, run `powershell -ExecutionPolicy Bypass -File .\scripts\build-release.ps1`. It reuses the same local identity and runs clean, unit tests, lint and Release assembly. Do not create a new identity for an update. Increase `versionCode` for each distributed update, then repeat APK signing verification and installation checks. Keep the `.p12` on offline storage protected by BitLocker or equivalent and preserve the matching protected secret/account recovery material. The DPAPI password blob is tied to the Windows protection context; it is not a portable password export. No off-device backup or restore test has been performed, so arrange and verify secure recovery before retiring this Windows profile or machine.

## 3. Build, APK and package security

Command executed by the helper: `gradlew.bat clean testDebugUnitTest lintDebug assembleRelease --console=plain`.

| Check | Result |
| --- | --- |
| Clean build / unit tests | **PASS** — `BUILD SUCCESSFUL`; 50 unit tests passed. |
| Lint | **PASS** — 0 errors, 13 existing warnings. |
| Optimized Release assembly | **PASS** — R8 and resource shrinking completed; APK signed. |
| APK signature | **PASS** — one RSA-3072 signer, v2 verified. |
| APK manifest | **PASS** — ID `ie.suric.lizshopping`, version `1.0.0` / code `1`, min 26 / target 36; no Android runtime permissions or Internet permission. The only declared permission is AndroidX's generated signature-level dynamic-receiver permission. No unexpected exported app component was found. |
| Release debuggability | **PASS** — build config `debuggable=false`; installed package flags did not include `DEBUGGABLE`. |

Artifact: `release-output/Lizs-Shopping-List-v1.0.0-release.apk` (3,211,359 bytes; about 3.06 MiB). SHA-256: `9850982EF4143C2141F05AAD25EC579BF968DD02DE1C81B1E9FEFF24A4F10406`. The APK is deliberately not committed; its output directory is ignored.

The AndroidX native path library could not be stripped by the NDK during packaging; it remained packaged and did not prevent assembly, install or the tested flows. The app's existing backup and restore implementation was not changed. Unit coverage passed, but an optimized-APK backup/restore transaction was not run.

## 4. Device installation and functional verification

Target identity was rechecked before installation: Samsung SM-A127F, Android 13 / API 33, serial `R58R93QQ3GZ`. The existing app was replaced only after the signed APK passed verification, under the user's explicit Task 11 confirmation that no shopping data required preservation. `adb install` returned `Success`; the launcher activity resolved and opened. The app ran as a normal installed application, without Android Studio or a debug bridge requirement. No crash or ANR was observed during the tested flows.

| Requirement | Result | Observed evidence / limit |
| --- | --- | --- |
| Launch and four default shops | **PASS** | Lidl, Aldi, Dunnes Stores and SuperValu appeared. Release package launched and stayed running. |
| Shop switching and list isolation | **PASS** | Added Milk to Lidl; Aldi showed an empty independent list; switched back to Lidl. |
| Catalogue and direct Add | **PASS** | Catalogue rendered; Milk was added to Lidl. |
| Product configuration and package size/count independence | **PASS** | Edited list Milk from 1 L to 2 L and saved with count ×1. List readback showed `Whole · 2 L`; the favourite independently remained `1 L` and ×1. |
| Quantity controls | **PASS** | Incremented and decremented the list count; restored the test item to ×1. |
| Completion state | **PASS** | Marked the item bought and returned it to active. |
| Favourites | **PASS, partial** | Saved Milk as a favourite; its default size/count remained 1 L / ×1 while the list item was edited. Favourite removal was not exercised. |
| Force-stop/restart persistence | **PASS** | List, size/count and selected shop were read back after force-stop and relaunch. |
| Product visuals and bottom sheets | **PASS, representative** | Catalogue/product imagery, shop drawer, list and configuration sheet were inspected; sheet opened, saved and closed. |
| Custom shops / custom categories / custom product and image picker | **NOT VERIFIED** | Not exercised in the Release journey. |
| Backup/restore | **NOT VERIFIED** | No disposable export/import fixture was used. |
| Runtime offline operation | **NOT VERIFIED** | No connectivity setting was changed; packaged app declares no Internet permission. |

The connected instrumentation journey passed separately on the fresh fixture: 1 test, 0 failures/errors, XML duration 80.237 s. Its initial safety guard refused to run on the phone's pre-existing non-empty state; no state was cleared by that refusal. The user's Task 11 confirmation then authorized a fresh app-only fixture. After Release manual checks, `pm clear ie.suric.lizshopping` removed only this app's temporary test state; the Release package remained installed and was relaunched to a clean first-run screen. No unrelated device data or settings were changed.

## 5. Visual and accessibility inspection

Inspected Release screens: landing/home, catalogue, Lidl list, favourites, shop drawer and product configuration sheet. The warm light theme, four shops, catalogue rows, package controls and bottom navigation were legible with no clipped controls observed. The decorative `CONFIGURE` heading wraps as `CONFIGU` / `RE` in the narrow hero; it is awkward but not clipped or a functional blocker, and no speculative design change was made. Product emoji imagery rendered in the sample catalogue.

Add and Favourite had separate accessibility labels in the UI hierarchy; Configure and the editor controls were present as distinct actions. The existing third-party accessibility service remained enabled throughout and was not identified or toggled. **TalkBack operation, focus traversal, dark theme, system theme, large fonts, landscape and contrast were not verified.** Representative semantics inspection is not a TalkBack acceptance test.

## 6. Release performance measurements

Target: Samsung SM-A127F / Android 13 / 60 Hz. Release APK hash is listed above. Catalogue contained 72 products. The same accessibility-service state was preserved (enabled); animation scales remained at 0.5. Battery was 64%, thermal status 0 and battery temperature 33.8 °C at the recorded preflight; no device setting was altered. A 60-second warm-up/discard preceded five measured catalogue trials. Each used continuous alternating upward/downward swipes of 420 ms; 51–55 measured pairs per trial (warm-up 55 pairs). No crash, ANR, visible freeze or lost control response was observed.

| Trial | Duration | Swipe pairs | Frames | GFXInfo janky | Jank % | Legacy janky % | P50 / P90 / P95 / P99 frame time (ms) |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| 1 | 60.81 s | 55 | 3,535 | 118 | 3.34% | 5.35% | 17 / 19 / 19 / 28 |
| 2 | 60.93 s | 55 | 3,535 | 116 | 3.28% | 4.50% | 16 / 19 / 20 / 27 |
| 3 | 61.09 s | 51 | 3,477 | 96 | 2.76% | 4.46% | 15 / 19 / 19 / 25 |
| 4 | 60.47 s | 55 | 3,505 | 121 | 3.45% | 4.31% | 15 / 19 / 19 / 25 |
| 5 | 60.04 s | 55 | 3,502 | 97 | 2.77% | 3.37% | 15 / 19 / 19 / 22 |
| Combined | 303.34 s | 271 | 17,554 | 548 | **3.12%** | **4.40%** | Per-run histogram percentiles shown above |

GFXInfo's janky-frame rate and legacy jank-rate are different summary fields; both are retained as reported. The combined jank rate is a weighted frame count, not an average of trial percentages. Perfetto separately captured 30.26 seconds / 23 swipe pairs and 1,652 app-layer frames. Trace Processor classified 51 frames (3.09%) as `App Deadline Missed`; frame duration percentiles were P50 17.90 ms, P90 18.62 ms, P95 18.84 ms, P99 20.56 ms, max 25.99 ms. `Buffer Stuffing` was classified on 978 frames (59.2%); categories may overlap and are not interpreted as app deadline misses. Three negative-timestamp events were dropped by Trace Processor, so trace-health caveat applies. The local trace and parser output are ignored and not committed.

### Comparison to Task 9 debug evidence

| Measurement | Task 9 debug | Task 11 Release | Boundary |
| --- | ---: | ---: | --- |
| GFXInfo scroll jank | 5,632 / 6,816 (**82.63%**) | 548 / 17,554 (**3.12%**) | Same phone, Android version, 72-product catalogue, enabled accessibility-service state and repeated 60-second alternating 420 ms swipe workload. Different APK/build type and session/run order; Task 9 jank declined across its five ordered runs (93.54% to 74.32%), indicating a substantial warm-up/order effect. Release includes an explicit warm-up. This is strong observed evidence that the APK Liz will use scrolls much better in this setup, but not a controlled causal estimate of the effect of R8, release configuration, or another individual factor. |
| Perfetto app deadline misses | 392 / 593 (**66.1%**) | 51 / 1,652 (**3.09%**) | Separate trace captures and gesture samples; FrameTimeline categories are distinct from GFXInfo. Do not treat the percentage-point difference as an exact causal effect. |

Thus Task 11 answers the practical question with fresh Release measurements: this tested production APK did not reproduce the severe jank rate observed on the debug build. It does **not** establish that the earlier Compose accessibility traversal hotspot is fixed or explain the cross-build difference. The accessibility service remained enabled; no service-off attribution test was performed. Keep Task 9/10 diagnosis distinct from this Release outcome.

## 7. Outstanding acceptance and decision

The APK is signed, installable, non-debuggable and optimized; core household shopping operations were exercised on the actual Release install, and the measured scrolling result is responsive in the repeated workload. Those facts support **CONDITIONALLY READY** for ordinary household use. Conditions for an unqualified release acceptance: securely back up and restore-test the persistent signing identity; exercise custom shop/product/category/image flows and backup/restore on disposable state; test offline behavior and the supported appearance/accessibility matrix; and obtain representative-user confirmation. These are unverified coverage items, not reported failures. No data loss, crash, permission or functional defect was observed in the checked scope.

## 8. Artifact and source-control closeout

- APK: `release-output/Lizs-Shopping-List-v1.0.0-release.apk` (ignored local artifact; installable via `adb install -r` or Android's file installer).
- Local performance evidence: ignored `verification/task11-release/`; includes private UI captures and the approximately 61 MB Perfetto trace. Do not upload it.
- Parser: official Perfetto Trace Processor wrapper is local under the ignored Task 11 verification folder; it provides trace analysis without a VS Code extension. No VS Code extension was installed.
- Branch and commit are recorded in the final Task 11 handoff after documentation changes are committed. The private key, password material, APK, trace, screenshots and UI dumps are not tracked.
