# Task 5 Acceptance Report

## Historical scope and current follow-up - 2026-10-16

This is the historical 2026-10-15 manual acceptance report, distinct from the later Task 5 CI-failure investigation. Its commands, APK hash and release verdict refer to that dated snapshot. Current implementation/testing status is in [TESTING.md](../TESTING.md) and the [CI completion report](TASK5_CI_INSTRUMENTATION_REPORT.md). The open UI/accessibility/reboot/backup checks below remain open; the current successful CI journey does not retroactively execute them.

Date: 2026-10-15  
Repository: `https://github.com/brunkonjaa/Liz-s-Shopping-App.git`  
Branch: `development/task2-task3-integration`  
Application ID: `ie.suric.lizshopping`

## A. Repository and environment

- Starting commit: `7fd402982ce358b8268f52a7d0a6c0f3bfd75bd4` (`docs: record task 4 verification evidence`).
- Final delivery commit: the documentation delivery commit reported in the Task 5 handoff.
- Working tree was clean before this report. The ignored `verification/` directory contains local evidence and remains excluded from Git.
- Host: Windows 11, JDK 17.0.20.1, Gradle 9.4.1, Android SDK compile API 37.
- Primary acceptance target: Samsung SM-A127F, Android 13 / API 33, serial `R58R93QQ3GZ`.
- APK installed for manual testing from `app/build/outputs/apk/debug/app-debug.apk` without clearing the dedicated test installation.

## B. Acceptance checklist

| Area | Result | Evidence and limits |
|---|---|---|
| Data persistence | **PASS** for normal navigation and force-stop/restart | A controlled Lidl list retained Milk, quantity and completed state after force-stop/restart; Aldi remained an independent empty list. Device reboot and in-place update were not run. |
| Backup/restore | **NOT VERIFIED** end to end | Unit/storage tests cover codec, v1 compatibility, bounded input and transactional save. The UI file-picker export/import, invalid-file UI path and image backup contract were not executed. |
| Shopping workflows | **PASS** for the exercised Lidl journey | Launched, selected Lidl, searched Milk, opened the editor, changed quantity, saved a favourite, added to list, marked bought, reopened the list and switched shops. Full multi-shop product-entry journey was not completed. |
| Catalogue reuse | **NOT VERIFIED** end to end | Stable product identity and reuse rules are covered by source/unit tests; custom-product creation and reuse across another list were not manually executed. |
| Bottom sheets | **PASS** for inspected product and change-shop sheets | Settled sheets had shared rounded styling, drag handles, scrolling and visible actions in the captured states. Keyboard insets, every Task 4 sheet, dismissal permutations and rapid repeated actions remain unverified. |
| Visual design | **PASS** for inspected light/dark states; overall matrix **NOT VERIFIED** | Fresh light landing/list/product/change-shop and dark settings/list screenshots matched the approved warm editorial direction with no observed clipping. The full ten-screenshot matrix, large fonts and landscape were not completed. |
| Accessibility | **NOT VERIFIED** for TalkBack; automated semantics **PASS** | UI-tree inspection found named controls, roles, selected/checked states and no visible clickable target below the 48dp heuristic. TalkBack, focus order, keyboard navigation, contrast measurement and text scaling were not run. |
| Security/privacy | **PASS** for static/runtime regression checks; formal audit **NOT VERIFIED** | Manifest checker found no network or runtime permissions and only the launcher exported; `allowBackup=false`, cleartext disabled, no crash-buffer entries and no network dependency were observed. Vulnerability-free status, revoked URI handling and adversarial testing were not claimed. |
| Android compatibility | **PASS** on Samsung Android 13; API 37 **NOT VERIFIED** | Connected instrumentation passed and manual acceptance ran on the Samsung. The API 37 emulator attempt was stopped without app verification; this is an environment coverage gap, not an app failure. |

## C. Automated verification

All commands were run from the repository root on the current checkout with `--no-build-cache` where shown.

| Command | Result |
|---|---|
| `./gradlew.bat testDebugUnitTest --no-build-cache --console=plain` | **PASS**, Gradle build successful. |
| `./gradlew.bat lintDebug --no-build-cache --console=plain` | **PASS**, Gradle build successful. |
| `./gradlew.bat assembleDebug --no-build-cache --console=plain` | **PASS**, debug APK assembled. |
| `./gradlew.bat connectedDebugAndroidTest --no-build-cache --console=plain` | **PASS**, 1 test on SM-A127F / Android 13, 0 failures. |
| `scripts/check_manifest.py` against the packaged debug manifest | **PASS**, no network/runtime permissions and launcher-only exported component. |
| APK installation with `adb install -r` | **PASS**, installation returned `Success`. |
| `adb logcat -d -b crash` filtered for `ie.suric.lizshopping` | **PASS**, no matching crash-buffer entries. |

The automated suite is evidence for the implemented rules, storage codec/migration and existing instrumentation journey. It does not substitute for the unexecuted UI journeys listed below.

## D. Manual evidence

Fresh ignored evidence is under `verification/task5/`, including `launch.png`, `home.png`, `populated-list.png`, `product-sheet2.png`, `product-sheet-scrolled.png`, `change-shop.png`, `settings-dark.png`, `dark-home.png` and matching UI-tree dumps.

- First launch showed the navigation drawer, four default shops, favourites, current list, all items and settings.
- Lidl browsing/search opened the Milk product sheet. The settled sheet showed configuration controls, quantity controls, favourite toggle and add-to-list action; scrolling exposed the lower actions.
- Milk appeared with package size `Whole · 1 L`, quantity `×3` and a completion control. The favourite save toast was observed for Lidl.
- After force-stop/restart, Lidl showed `1 of 1 completed` and Aldi showed `0 of 0 completed`, directly demonstrating shop separation and persistence for this controlled data.
- Dark theme was selected, inspected in settings and on the shopping screen, then Light was restored. The restored UI tree reported Light selected.

## E. Defects

No confirmed P0 or P1 defects were found in the executed scope. No production Kotlin changes were necessary.

The manual search interaction briefly appended to an existing query during coordinate-driven testing; clearing the field and entering `Milk` produced the expected result. This was a test-harness input-state issue, not a confirmed product defect. An immediately captured product-sheet frame was transiently incomplete; the settled frame after the transition was complete and usable.

## F. Outstanding verification

- Complete the UI backup/export/import journey with valid, malformed, oversized and unsupported-version files, including confirmation cancellation and preservation after failed restore.
- Execute device reboot and in-place APK update persistence checks on an isolated installation.
- Run custom product/catalogue reuse and full shop-management journeys, including cancellation and guarded destructive confirmations.
- Inspect every implemented bottom sheet with keyboard, Back, outside dismissal, repeated taps and lower-content scrolling.
- Run TalkBack or an equivalent device accessibility service, plus large-font, landscape and contrast checks.
- Verify image selection/replacement/removal and revoked-URI behavior on the device.
- Verify API 37 only if a stable emulator becomes available; no compatibility claim is made here.

## G. Release readiness

**CONDITIONALLY READY.**

There are no known P0/P1 defects in the tested scope. Unit tests, lint, debug assembly, the existing connected journey, manifest checks and a non-destructive Samsung Android 13 manual flow all passed. Mandatory acceptance gaps remain for backup/restore UI, device reboot/update persistence, TalkBack, full visual/accessibility matrices, several management/reuse journeys and API 37; therefore this is not a `READY` verdict.

## H. APK

- Location: `app/build/outputs/apk/debug/app-debug.apk`
- Variant: `debug`
- SHA-256: `016A8FE45A6948C5B138520CEBBC7A5B793FA9D028A033FD95A1D9A0B5B64192`

## I. Recommended next action

Use an isolated installation to complete the backup/restore UI matrix and reboot/update persistence check; then run the TalkBack and large-font accessibility pass. These are the smallest remaining checks that could change the conditional release decision.
