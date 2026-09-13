#!/usr/bin/env python3
"""Reject browser components/assets in the distributable APK."""
import sys, zipfile, pathlib, hashlib
apk=pathlib.Path(sys.argv[1])
with zipfile.ZipFile(apk) as archive:
    names=archive.namelist()
    forbidden=[n for n in names if n.startswith('assets/') and n.endswith(('.js','.html','.css','.wasm'))]
    if forbidden:raise SystemExit('Browser assets found: '+str(forbidden))
    for name in names:
        if name.endswith('.dex') and b'Landroid/webkit/WebView;' in archive.read(name):
            raise SystemExit('WebView class reference found in '+name)
    for required in ['assets/fonts/JetBrainsMono.ttf','assets/fonts/FiraCode.ttf','assets/languages.json','assets/textmate/languages.json']:
        assert archive.getinfo(required).file_size>0,required
sha=hashlib.sha256(apk.read_bytes()).hexdigest()
apk.with_suffix('.apk.sha256').write_text(sha+'  '+apk.name+'\n')
print('PASS: no WebView class references and no HTML/CSS/JavaScript/WASM assets; fonts and native grammar data present.')
print('SHA-256: '+sha)
