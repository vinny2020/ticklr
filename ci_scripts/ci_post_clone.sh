#!/bin/zsh
# ci_scripts/ci_post_clone.sh

# Symlink the ios project to the repo root so Xcode Cloud can find it
ln -s "$CI_WORKSPACE/ios/SIT.xcodeproj" "$CI_WORKSPACE/SIT.xcodeproj"
