#!/bin/zsh
# ci_scripts/ci_post_clone.sh

# Symlink the ios project to the repo root so Xcode Cloud can find it
ln -sf "$CI_WORKSPACE/ios/SIT.xcodeproj" "$CI_WORKSPACE/SIT.xcodeproj"

# Xcode Cloud shallow clones may not include tags — fetch them
git fetch --tags --quiet 2>/dev/null

# Extract version from ios/v* tag (e.g. ios/v1.4.12 → 1.4.12)
GIT_TAG=$(git describe --tags --match "ios/v*" --abbrev=0 2>/dev/null)
if [[ $GIT_TAG == ios/v* ]]; then
  VERSION=${GIT_TAG#ios/v}
  echo "Setting MARKETING_VERSION to $VERSION from tag $GIT_TAG"

  # Patch MARKETING_VERSION directly in the pbxproj
  sed -i '' "s/MARKETING_VERSION = .*;/MARKETING_VERSION = $VERSION;/g" \
    "$CI_WORKSPACE/ios/SIT.xcodeproj/project.pbxproj"
else
  echo "No ios/v* tag found (tag: $GIT_TAG), using version from project.pbxproj"
fi
