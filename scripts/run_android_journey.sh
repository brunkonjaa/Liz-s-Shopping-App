#!/bin/bash
set -euo pipefail
# This script runs while the emulator is alive. The action kills it before the
# subsequent upload step. Gradle pulls additionalTestOutputDir before uninstall.
evidence_dir="app/build/reports/journey-diagnostics"
mkdir -p "$evidence_dir"
serial="${ANDROID_SERIAL:-emulator-5554}"
adb_target=(adb -s "$serial")
logcat_pid=""

collect_environment() {
  "${adb_target[@]}" shell wm size
  "${adb_target[@]}" shell wm density
  for key in window_animation_scale transition_animation_scale animator_duration_scale; do
    printf '%s=' "$key"
    "${adb_target[@]}" shell settings get global "$key"
  done
  for key in ro.build.version.sdk ro.build.version.release ro.product.cpu.abi ro.product.model ro.build.fingerprint; do
    printf '%s=' "$key"
    "${adb_target[@]}" shell getprop "$key"
  done
  "${adb_target[@]}" emu avd name
}

finish() {
  status=$?
  trap - EXIT
  set +e
  collect_environment > "$evidence_dir/environment-after.txt" 2>&1
  "${adb_target[@]}" shell dumpsys input_method > "$evidence_dir/input-method-after.txt" 2>&1
  "${adb_target[@]}" shell dumpsys activity activities > "$evidence_dir/activities-after.txt" 2>&1
  if [[ -n "$logcat_pid" ]]; then
    kill "$logcat_pid" 2>/dev/null
    wait "$logcat_pid" 2>/dev/null
  fi
  printf '%s\n' "$status" > "$evidence_dir/exit-status.txt"
  exit "$status"
}
trap finish EXIT

collect_environment > "$evidence_dir/environment-before.txt" 2>&1 || true
"${adb_target[@]}" shell dumpsys input_method > "$evidence_dir/input-method-before.txt" 2>&1 || true
if [[ -n "${ANDROID_AVD_HOME:-}" ]]; then
  find "$ANDROID_AVD_HOME" -name config.ini -type f -exec cat {} \; > "$evidence_dir/avd-config.txt" 2>&1 || true
fi
# CI is a fresh disposable emulator. Bound this to relevant app/test/SQLite tags.
"${adb_target[@]}" logcat -v threadtime ShoppingJourneyEvidence:V ShoppingJourneyState:V ShoppingJourneyTimeout:V TestRunner:V AndroidRuntime:E SQLiteLog:W SQLiteDatabase:W '*:S' > "$evidence_dir/logcat.txt" 2>&1 &
logcat_pid=$!

# tee retains runner output; pipefail and PIPESTATUS preserve the Gradle result.
set +e
./gradlew --no-daemon connectedDebugAndroidTest --console=plain 2>&1 | tee "$evidence_dir/runner-output.txt"
status=${PIPESTATUS[0]}
set -e

exit "$status"
