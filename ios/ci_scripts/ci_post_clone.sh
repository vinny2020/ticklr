#!/bin/zsh
# ci_scripts/ci_post_clone.sh

# Derive the project path relative to this script's location
# Script is at ios/ci_scripts/ → project is at ios/SIT.xcodeproj/
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PBXPROJ="$SCRIPT_DIR/../SIT.xcodeproj/project.pbxproj"

# Xcode Cloud shallow clones may not include tags — fetch them
git fetch --tags --quiet 2>/dev/null

# Extract version from ios/v* tag (e.g. ios/v1.5.7 → 1.5.7)
GIT_TAG=$(git describe --tags --match "ios/v*" --abbrev=0 2>/dev/null)
if [[ $GIT_TAG == ios/v* ]]; then
  VERSION=${GIT_TAG#ios/v}
  echo "Setting MARKETING_VERSION to $VERSION from tag $GIT_TAG"

  # Patch MARKETING_VERSION directly in the pbxproj
  sed -i '' "s/MARKETING_VERSION = .*;/MARKETING_VERSION = $VERSION;/g" "$PBXPROJ"
else
  echo "No ios/v* tag found (tag: $GIT_TAG), using version from project.pbxproj"
fi
