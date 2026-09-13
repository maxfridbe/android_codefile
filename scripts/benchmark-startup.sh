#!/usr/bin/env bash
set -euo pipefail
runs="${1:-10}"
[[ "$runs" =~ ^[1-9][0-9]*$ ]] || { echo 'Usage: benchmark-startup.sh [positive-run-count]' >&2; exit 1; }
adb get-state >/dev/null
for ((run=1; run<=runs; run++)); do
    adb shell am force-stop com.TechHurts.CodeFile
    adb shell am start -W -n com.TechHurts.CodeFile/.MainActivity
    sleep 1
done
