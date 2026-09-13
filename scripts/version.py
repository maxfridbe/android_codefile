#!/usr/bin/env python3
"""Validate vYY.MMDD.### and derive an increasing Android versionCode."""
import datetime, os, re
version = os.environ.get('VERSION_NAME', open('VERSION').read().strip())
m = re.fullmatch(r'v(\d{2})\.(\d{2})(\d{2})\.(\d{3})', version)
if not m:
    raise SystemExit('Version must match vYY.MMDD.###')
y, month, day, serial = map(int, m.groups())
date = datetime.date(2000 + y, month, day)
code = (date - datetime.date(2000, 1, 1)).days * 1000 + serial
print(version, code)
