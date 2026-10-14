#!/bin/bash
set -uo pipefail
result=0
./gradlew --no-daemon connectedDebugAndroidTest || result=$?
mkdir -p app/build/qa-screenshots
for screen in shops milk-editor shopping-list after-test; do
  adb exec-out run-as ie.suric.lizshopping cat "files/$screen.png" > "app/build/qa-screenshots/$screen.png" || true
done
exit "$result"
