#!/bin/bash
set -euo pipefail
# Gradle pulls additionalTestOutputDir before uninstalling the test application.
# Preserve its exit status; never upload adb error text as a PNG.
./gradlew --no-daemon connectedDebugAndroidTest
