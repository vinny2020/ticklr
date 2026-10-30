#!/bin/zsh
# ci_scripts/ci_post_clone.sh

# Xcode Cloud's Xcode version is selected in its workflow UI. Fail early if
# that setting drifts below the Duo baseline, and retain evidence in build logs.
xcodebuild -version || exit 1
SDK_VERSION=$(xcrun --sdk iphoneos --show-sdk-version) || exit 1
echo "iOS archive SDK: $SDK_VERSION"
autoload -Uz is-at-least
if ! is-at-least 27.1 "$SDK_VERSION"; then
  echo "error: Select Xcode 27.1 or later in the Xcode Cloud workflow."
  exit 1
fi

# Derive the project path relative to this script's location
# Script is at ios/ci_scripts/ → project is at ios/SIT.xcodeproj/
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PBXPROJ="$SCRIPT_DIR/../SIT.xcodeproj/project.pbxproj"

# Xcode Cloud shallow clones may not include tags — fetch them
git fetch --tags --quiet 2>/dev/null

# Resolve the release version. Prefer the tag that triggered this build —
# Xcode Cloud sets CI_TAG for tag-based starts. Otherwise fall back to the
# HIGHEST ios/v* tag on the built commit via a real version sort.
#
# Do NOT use `git describe`: it selects by commit-graph distance and prefers
# annotated over lightweight tags, not by version number. With multiple tags
# on one commit that makes it return an older tag (e.g. an annotated v1.7.1
# over lightweight v1.7.3), which is the bug this replaced.
if [[ $CI_TAG == ios/v* ]]; then
  GIT_TAG=$CI_TAG
else
  GIT_TAG=$(git tag -l "ios/v*" --points-at HEAD | sort -V | tail -1)
fi
if [[ $GIT_TAG == ios/v* ]]; then
  VERSION=${GIT_TAG#ios/v}
  echo "Setting MARKETING_VERSION to $VERSION from tag $GIT_TAG"

  # Patch MARKETING_VERSION directly in the pbxproj
  sed -i '' "s/MARKETING_VERSION = .*;/MARKETING_VERSION = $VERSION;/g" "$PBXPROJ"
else
  echo "No ios/v* tag found (tag: $GIT_TAG), using version from project.pbxproj"
fi
