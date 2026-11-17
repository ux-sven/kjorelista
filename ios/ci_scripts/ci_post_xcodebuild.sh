#!/bin/zsh
# Xcode Cloud: kjøres etter xcodebuild (også når bygget feiler).
# Når bygget er signert for App Store/TestFlight, skrives de siste commit-meldingene til
# «Hva skal testes» i TestFlight (TestFlight/WhatToTest.<locale>.txt ved siden av Xcode-prosjektet).
# Kilde: https://developer.apple.com/documentation/xcode/including-notes-for-testers-with-a-beta-release-of-your-app

if [[ -d "$CI_APP_STORE_SIGNED_APP_PATH" ]]; then
  TESTFLIGHT_DIR_PATH=../TestFlight
  mkdir -p $TESTFLIGHT_DIR_PATH
  git fetch --deepen 5 >/dev/null 2>&1 || true
  NOTES=$(git log -5 --pretty=format:"• %s")
  print -r -- "$NOTES" >! $TESTFLIGHT_DIR_PATH/WhatToTest.no.txt
  print -r -- "$NOTES" >! $TESTFLIGHT_DIR_PATH/WhatToTest.en-US.txt
  echo "Testnotater for TestFlight:"
  echo "$NOTES"
fi
