#!/usr/bin/env python3
"""Prepare only native assets; fail if browser runtime files are packaged."""
from pathlib import Path
import re, subprocess
version, code = subprocess.check_output(['python3','scripts/version.py'], text=True).split()
p=Path('apps/codefile/app/build.gradle.kts')
s=p.read_text()
s=re.sub(r'(versionCode = .*\?: )\d+',lambda m:m[1]+code,s)
s=re.sub(r'(versionName = .*\?: )"[^"]*"',lambda m:m[1]+'"'+version+'"',s)
p.write_text(s)
assets=Path('apps/codefile/app/src/main/assets')
for p in assets.rglob('*'):
    if p.suffix in {'.js','.html','.css','.wasm'}:
        raise SystemExit('Browser asset in native build: '+str(p))
print('Native CodeFile '+version)
