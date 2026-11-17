#!/bin/sh
# Bygger et Release-arkiv og laster det opp til App Store Connect / TestFlight.
# Bruker Apple-kontoen som er logget inn i Xcode (Innstillinger → Kontoer) og automatisk signering.
#
# Bruk:  scripts/testflight.sh          # bygg og last opp
#        scripts/testflight.sh --export # bare lag .ipa i build/export (ingen opplasting)
#
# Build-nummeret settes til tidspunktet som ÅÅÅÅ.MDD.TMM (f.eks. 2026.1117.1445), slik at hver opplasting får et
# høyere nummer. Tre korte tall fordi App Store Connect ikke godtar tall større enn 2^31 i build-nummeret.
# Versjonsnummeret (MARKETING_VERSION) endres i prosjektet når en ny versjon skal ut.
set -e
cd "$(dirname "$0")/.."

DESTINATION=upload
[ "$1" = "--export" ] && DESTINATION=export
BUILD_NUMBER="$(date +%Y).$(date +%-m%d).$(date +%-H%M)"
ARCHIVE=build/Kjorelista.xcarchive

rm -rf "$ARCHIVE" build/export
xcodebuild -project Kjorelista.xcodeproj -scheme Kjorelista -configuration Release \
  -destination 'generic/platform=iOS' -archivePath "$ARCHIVE" \
  CURRENT_PROJECT_VERSION="$BUILD_NUMBER" -allowProvisioningUpdates -quiet archive

cat > build/ExportOptions.plist <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>method</key><string>app-store-connect</string>
	<key>destination</key><string>$DESTINATION</string>
	<key>teamID</key><string>XE253SCCM3</string>
	<key>signingStyle</key><string>automatic</string>
	<key>uploadSymbols</key><true/>
	<key>manageAppVersionAndBuildNumber</key><false/>
</dict>
</plist>
EOF

xcodebuild -exportArchive -archivePath "$ARCHIVE" -exportPath build/export \
  -exportOptionsPlist build/ExportOptions.plist -allowProvisioningUpdates

echo "Ferdig: build $BUILD_NUMBER ($DESTINATION)"
