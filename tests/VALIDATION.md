# Native release validation — v26.0912.003

Package: `com.TechHurts.CodeFile`; versionCode: `9751003`; Android API 24–34 build target.

- `assembleDebug assembleRelease lint testDebugUnitTest`: passed.
- 15 JUnit/Robolectric tests: passed, no failures or skips.
- 11 additional API endpoint checks: passed.
- All bundled grammar catalog entries load into the native TextMate editor.
- Synthetic touch pinch changes scale without editing source.
- Long lines visibly reflow while source line numbers remain stable.
- Simulated phone, tablet, vertical hinge and horizontal hinge layouts pass.
- Native portrait bitmap inspected with highlighting and wrapped lines visible.
- Release ZIP/DEX audit: no WebView references or HTML/CSS/JavaScript/WASM assets; bundled fonts and grammar catalogs present.
- `apksigner verify --verbose`: passed, APK Signature Scheme v2, one signer.
- `aapt dump badging`: expected package/version, small/normal/large/xlarge screens, any density.

Release SHA-256:

```
45dd8c502329cd1a2f5789314059feb343b64f6c48efcd9c5562e0e3291b113e
```

No physical Android device was attached. Real Galaxy Z Fold posture changes, installed document/share providers, and live API/LSP services still need device integration testing. Robolectric is not a substitute for those checks. Unit tests run against the debug classes; release verification covers packaging, shrinking and signing, not execution on a device.

Reproduce using the build and audit commands in README.md. Generated reports and APKs remain excluded from Git; the signing key is private and excluded as well.
