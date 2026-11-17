#!/bin/sh
# Bygger appen, installerer den i simulatoren og starter den i demomodus.
# Bruk: scripts/run-demo.sh [simulator-UDID]   (standard: første bootede simulator)
set -e
cd "$(dirname "$0")/.."
UDID="${1:-booted}"
DEST="platform=iOS Simulator,id=$(xcrun simctl list devices booted | grep -m1 -oE '[0-9A-F-]{36}')"
[ "$UDID" != "booted" ] && DEST="platform=iOS Simulator,id=$UDID"
xcodebuild -project Kjorelista.xcodeproj -scheme Kjorelista -destination "$DEST" -derivedDataPath build build -quiet
xcrun simctl install "$UDID" build/Build/Products/Debug-iphonesimulator/Kjorelista.app
xcrun simctl launch --terminate-running-process "$UDID" com.svenway.kjorelista -demo
