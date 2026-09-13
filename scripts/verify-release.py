#!/usr/bin/env python3
"""Verify release identity and metadata using Android's official tools."""
import hashlib
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent
CERT = '0d2b5acd9d3ef02e17c14bd14122cfa5566b8b3d6d2ec427e00a88606a2dc3b2'
BUNDLETOOL_SHA = 'a099cfa1543f55593bc2ed16a70a7c67fe54b1747bb7301f37fdfd6d91028e29'
PACKAGE = 'com.TechHurts.CodeFile'
ANDROID = '{http://schemas.android.com/apk/res/android}'


def require(condition, message):
    if not condition:
        raise SystemExit(message)


def main():
    os.chdir(ROOT)
    jar = Path(os.environ.get('BUNDLETOOL_JAR', '/tmp/codefile-bundletool.jar')).resolve()
    require(jar.is_file(), 'Set BUNDLETOOL_JAR to official bundletool-all-1.18.3.jar; see store/RELEASE.md')
    require(hashlib.sha256(jar.read_bytes()).hexdigest() == BUNDLETOOL_SHA, 'Unexpected bundletool checksum')
    version, code = subprocess.check_output([sys.executable, 'scripts/version.py'], text=True).split()
    output = ROOT / 'output/codefile'
    apk = output / f'codefile-{version}-release.apk'
    aab = output / f'codefile-{version}-release.aab'
    log = output / f'codefile-{version}-verification.txt'
    engine = os.environ.get('CONTAINER_ENGINE') or shutil.which('podman') or shutil.which('docker')
    require(engine, 'Podman or Docker required')
    mount = f'{output}:/out:ro' + (',Z' if Path(engine).name == 'podman' else '')
    container = [engine, 'run', '--rm', '-v', mount, os.environ.get('IMAGE', 'codefile-builder')]
    android_tools = '/opt/android-sdk/build-tools/35.0.0/'
    with log.open('w') as report:
        def run(args):
            result = subprocess.run(args, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            report.write(result.stdout + '\n')
            report.flush()
            require(result.returncode == 0, f'Validation failed; see {log}')
            return result.stdout

        signature = run(container + [android_tools + 'apksigner', 'verify', '--verbose', '--print-certs', '/out/' + apk.name])
        require(re.findall(r'certificate SHA-256 digest: (\w+)', signature) == [CERT], 'Unexpected APK signing identity')
        badging = run(container + [android_tools + 'aapt', 'dump', 'badging', '/out/' + apk.name])
        for expected in [f"name='{PACKAGE}'", f"versionCode='{code}'", f"versionName='{version}'", "sdkVersion:'24'", "targetSdkVersion:'36'"]:
            require(expected in badging, 'APK metadata mismatch: ' + expected)
        require('application-debuggable' not in badging, 'Debuggable APK')
        bundle = ['java', '-jar', str(jar)]
        run(bundle + ['validate', '--bundle=' + str(aab)])
        manifest = ET.fromstring(run(bundle + ['dump', 'manifest', '--bundle=' + str(aab), '--module=base']))
        require(manifest.get('package') == PACKAGE, 'Wrong bundle package')
        require(manifest.get(ANDROID + 'versionCode') == code and manifest.get(ANDROID + 'versionName') == version, 'Wrong bundle version')
        sdk = manifest.find('uses-sdk')
        require(sdk is not None and sdk.get(ANDROID + 'minSdkVersion') == '24' and sdk.get(ANDROID + 'targetSdkVersion') == '36', 'Wrong bundle SDK levels')
        app = manifest.find('application')
        require(app is not None and app.get(ANDROID + 'debuggable', 'false') == 'false', 'Debuggable bundle')
        require(app.get(ANDROID + 'allowBackup') == 'false', 'Backup unexpectedly enabled')
        permissions = {node.get(ANDROID + 'name') for node in manifest.findall('uses-permission')}
        require(permissions == {'android.permission.INTERNET', PACKAGE + '.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'}, 'Unexpected permissions')
        verified = run(['jarsigner', '-J-Duser.language=en', '-verify', str(aab)])
        require('jar verified.' in verified and 'unsigned entries' not in verified, 'Bundle signature incomplete')
        certificates = run(['keytool', '-J-Duser.language=en', '-printcert', '-jarfile', str(aab)])
        fingerprints = [value.replace(':', '').lower() for value in re.findall(r'SHA256: ([A-Fa-f0-9:]+)', certificates)]
        require(fingerprints == [CERT], 'Unexpected bundle signing identity')
        report.write('PASS: release signatures, identity, SDK, permissions and bundle structure\n')
    print('PASS: official release verification; report:', log)


if __name__ == '__main__':
    main()
