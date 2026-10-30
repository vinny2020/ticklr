#!/bin/bash
# Run from macOS Terminal: bash scripts/ios/validate-duo.sh
# Builds locally without signing; does not publish or erase simulator data.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
RESULTS=$(mktemp -d "${TMPDIR:-/tmp}/ticklr-duo.XXXXXX")
LOG_DIR="$ROOT/logs/duo"
mkdir -p "$LOG_DIR"
LOG_FILE="$LOG_DIR/validation-$(date +%Y%m%d-%H%M%S)-$$.log"
# A normal pipeline also works in environments that restrict /dev/fd.
# pipefail preserves validation failures even when tee succeeds.
(
trap 'status=$?; printf "Validation finished with exit code %s. Log: %s\n" "$status" "$LOG_FILE"' EXIT
printf 'Validation log: %s\n' "$LOG_FILE"
printf 'Validation results: %s\n' "$RESULTS"
xcodebuild -version
SDK_VERSION=$(xcrun --sdk iphoneos --show-sdk-version)
printf 'Device SDK: %s\n' "$SDK_VERSION"
/usr/bin/python3 - "$SDK_VERSION" <<'PY'
import sys
if tuple(map(int, sys.argv[1].split('.'))) < (27, 1):
    raise SystemExit('Select Xcode 27.1 or later before continuing.')
PY
cd "$ROOT/ios"
xcodegen generate
xcodebuild -resolvePackageDependencies -project SIT.xcodeproj -scheme Ticklr \
  -clonedSourcePackagesDirPath "$RESULTS/SourcePackages"
xcodebuild archive -project SIT.xcodeproj -scheme Ticklr \
  -destination 'generic/platform=iOS' -archivePath "$RESULTS/Ticklr.xcarchive" \
  -derivedDataPath "$RESULTS/DerivedData" \
  -clonedSourcePackagesDirPath "$RESULTS/SourcePackages" CODE_SIGNING_ALLOWED=NO
xcrun simctl list runtimes
xcrun simctl list devices available --json > "$RESULTS/devices.json"
DUO_ID=$(/usr/bin/python3 - "$RESULTS/devices.json" <<'PY'
import json, sys
with open(sys.argv[1]) as f:
    groups = json.load(f)['devices']
for devices in groups.values():
    for d in devices:
        if d.get('isAvailable') and d.get('deviceTypeIdentifier', '').endswith('.iPhone-Duo'):
            print(d['udid'])
            sys.exit(0)
raise SystemExit('No available iPhone Duo. In Xcode, install the iOS 27.1 or later simulator runtime and add an iPhone Duo simulator, then rerun.')
PY
)
# Keep xcresult/screenshots, but avoid optional sysdiagnose collection hanging
# after simulator snapshot retries (even when all test assertions pass).
xcodebuild test -project SIT.xcodeproj -scheme Ticklr \
  -collect-test-diagnostics never \
  -destination "platform=iOS Simulator,id=$DUO_ID" \
  -derivedDataPath "$RESULTS/DerivedData" \
  -clonedSourcePackagesDirPath "$RESULTS/SourcePackages" \
  -skip-testing:TicklrUITests/TutorialUITests \
  -skip-testing:TicklrUITests/TutorialLocalizationUITests \
  -resultBundlePath "$RESULTS/DuoTests.xcresult" CODE_SIGNING_ALLOWED=NO
printf 'Archive and Duo tests passed. Results: %s\n' "$RESULTS"
) 2>&1 | tee "$LOG_FILE" "$RESULTS/validation.log"
