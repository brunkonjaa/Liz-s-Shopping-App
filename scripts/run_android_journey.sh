#!/bin/bash
set -euo pipefail
# Gradle pulls additionalTestOutputDir before uninstalling the test application.
# Preserve its exit status; never upload adb error text as a PNG.
set +e
./gradlew --no-daemon connectedDebugAndroidTest
status=$?
set -e

evidence_dir="app/build/outputs/connected_android_test_additional_output"
mkdir -p "$evidence_dir"
temp_screenshot="$evidence_dir/shopping-journey-timeout.png.tmp"
final_screenshot="$evidence_dir/shopping-journey-timeout.png"
rm -f "$temp_screenshot" "$final_screenshot"
adb -s "${ANDROID_SERIAL:-emulator-5554}" exec-out run-as ie.suric.lizshopping cat files/shopping-journey-timeout.png > "$temp_screenshot" 2>/dev/null || true
if test -s "$temp_screenshot"; then
  mv "$temp_screenshot" "$final_screenshot"
else
  rm -f "$temp_screenshot"
fi

exit "$status"
