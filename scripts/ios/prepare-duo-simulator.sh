#!/usr/bin/env bash
set -euo pipefail

# Print only the destination UUID on stdout so callers can capture it. Wait
# for migration and SpringBoard startup before Xcode requests an app launch.
device_id=${1:-}
if [[ -z "$device_id" ]]; then
  device_id=$(xcrun simctl list devices available --json | python3 -c '
import json, sys
devices = json.load(sys.stdin)["devices"].get("com.apple.CoreSimulator.SimRuntime.iOS-27-1", [])
matches = [d for d in devices if d.get("isAvailable") and d.get("deviceTypeIdentifier", "").endswith(".iPhone-Duo")]
if not matches:
    sys.exit("No available iOS 27.1 iPhone Duo simulator")
print(matches[0]["udid"])
')
fi

xcrun simctl bootstatus "$device_id" -b >&2
printf '%s\n' "$device_id"
