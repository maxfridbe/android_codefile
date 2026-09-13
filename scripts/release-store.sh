#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
REQUIRE_EXISTING_KEYSTORE=1 BUILD_TASK='assembleDebug assembleRelease bundleRelease lint testDebugUnitTest checkNativeStyle' ./build.sh codefile
release_version="$(cat VERSION)"
python3 scripts/audit-native-apk.py "output/codefile/codefile-${release_version}-release.apk"
python3 scripts/audit-store.py "output/codefile/codefile-${release_version}-release.apk" "output/codefile/codefile-${release_version}-release.aab"
python3 scripts/verify-release.py
./scripts/test-java.sh
