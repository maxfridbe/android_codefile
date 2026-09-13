#!/usr/bin/env python3
"""Check release containers without reading or exposing signing secrets."""
import hashlib
import pathlib
import sys
import zipfile

for supplied in sys.argv[1:]:
    path = pathlib.Path(supplied)
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        native = [name for name in names if name.endswith('.so')]
        if native:
            raise SystemExit('Native libraries require a separate 16 KB alignment audit: ' + str(native))
        prefix = 'base/' if path.suffix == '.aab' else ''
        assert prefix + 'assets/PRIVACY.txt' in names, 'Missing in-app privacy policy'
        if path.suffix == '.aab':
            assert 'BundleConfig.pb' in names
            assert 'base/manifest/AndroidManifest.xml' in names
            assert any(name.startswith('META-INF/') and name.endswith('.RSA') for name in names), 'Unsigned bundle'
        else:
            assert 'AndroidManifest.xml' in names
        bad = [name for name in names if name.endswith(('.keystore', '.jks'))]
        assert not bad, 'Signing material in release'
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    path.with_suffix(path.suffix + '.sha256').write_text(digest + '  ' + path.name + '\n')
    print('PASS:', path.name, '(no native libraries; policy bundled) SHA256', digest)
