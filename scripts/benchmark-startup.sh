#!/usr/bin/env bash
# Process-cold starts: OS filesystem caches are intentionally retained.
set -euo pipefail
cd "$(dirname "$0")/.."
runs="${1:-10}"
[[ "$runs" =~ ^[1-9][0-9]*$ ]] || { echo 'Usage: benchmark-startup.sh [positive-run-count]' >&2; exit 1; }
adb get-state >/dev/null
benchmark_dir="output/benchmarks/$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$benchmark_dir"
adb shell getprop ro.product.model > "$benchmark_dir/device.txt"
adb shell getprop ro.build.version.sdk >> "$benchmark_dir/device.txt"
adb shell dumpsys package com.TechHurts.CodeFile | sed -n '/versionCode=/p; /versionName=/p; /flags=/p' >> "$benchmark_dir/device.txt"
printf 'run,total_ms,wait_ms\n' > "$benchmark_dir/startup.csv"
for ((run=1; run<=runs; run++)); do
    adb shell am force-stop com.TechHurts.CodeFile
    adb shell am start -W -n com.TechHurts.CodeFile/.MainActivity > "$benchmark_dir/run-$run.txt"
    python3 - "$benchmark_dir/run-$run.txt" "$run" >> "$benchmark_dir/startup.csv" <<'PY'
import pathlib, re, sys
text = pathlib.Path(sys.argv[1]).read_text()
if not re.search(r'^Status: ok\s*$', text, re.M) or 'Warning:' in text or 'Error:' in text:
    raise SystemExit('Launch failed or reused an activity; inspect ' + sys.argv[1])
values = [re.search(r'^' + key + r': (\d+)\s*$', text, re.M) for key in ('TotalTime', 'WaitTime')]
if not all(values):
    raise SystemExit('Missing launch timing; inspect ' + sys.argv[1])
print(sys.argv[2] + ',' + ','.join(value[1] for value in values))
PY
    sleep 1
done
python3 - "$benchmark_dir/startup.csv" <<'PY' | tee "$benchmark_dir/summary.txt"
import csv, statistics, sys
with open(sys.argv[1]) as source:
    times = [int(row['total_ms']) for row in csv.DictReader(source)]
print(f'Process-cold launch: n={len(times)}, median={statistics.median(times):g} ms, min={min(times)} ms, max={max(times)} ms')
print('Measures Android am start TotalTime, not syntax readiness or filesystem-cold startup.')
print('Raw timings: ' + sys.argv[1])
PY
