# Native release validation — v26.0912.004

Package: `com.TechHurts.CodeFile`; versionCode: `9751004`; minimum Android API 24, target/compile API 34.

- `assembleDebug assembleRelease lint testDebugUnitTest checkNativeStyle`: passed.
- 28 JUnit/Robolectric tests: passed, no failures or skips.
- 11 additional API endpoint checks: passed.
- google-java-format 1.22.0 dry-run and Git whitespace checks: passed.
- Checkstyle rejects excessive nested `if`, `for`, and `try` blocks; CI runs the check.
- Theme selection immediately repaints the existing editor without replacing its document.
- Ribbon actions remain within their native layout at 320/360/411/600/840dp widths. Editor drag events collapse and reopen the ribbon.
- Simulated horizontal hinge: code above, full controls pane below. Vertical hinge and tablet layouts also pass.
- Native pinch zoom and visible wrapping preserve source text and source line numbers.
- Settings groups expand/collapse; native viewer and Settings bitmaps visually inspected. Header logo removed; Settings Save remains outside its scrolling content.
- Search tests cover case-insensitive/case-sensitive results, next-match selection, malformed regex handling, clearing, and highlight-color persistence across configuration refresh.
- YAML tests cover comments, quoted scalars, block content, indentation, aliases, custom tags, multiple documents, invalid input and idempotence. Inline-commented flow collections retain flow layout to preserve their comments.
- CSV/TSV tests cover escaped quotes, multiline fields, empty fields, leading zeros, significant spaces and malformed quotes. JSON Lines preserves large numeric tokens; Markdown preserves hard-break spaces. XML preserves mixed content and rejects DTDs/external entities.
- All bundled grammar catalog entries load. Data-table records are parsed off the UI thread.
- Release ZIP/DEX audit: no WebView references or HTML/CSS/JavaScript/WASM assets; bundled fonts and grammar catalogs present.
- APK signature verification: passed, Signature Scheme v2, one signer. Package/version metadata matches this release.

Release SHA-256:

```
f69d87aad8f7996357122770cba27c1ddb4d47b51520e460954e62e0b70c860f
```

The installed `android-dev` skill's Java guidance informed the review. Ribbon geometry, fold geometry, table rendering, asynchronous search bindings, and data formatting are separate classes. Background formatter/table operations use named helpers to avoid nested callback chains.

No physical Android device was attached. Actual Galaxy Z Fold posture changes, installed document/share providers, live API/LSP services, and TalkBack remain device integration checks. Robolectric tests run against debug classes; release validation covers packaging, shrinking and signing rather than execution on a device.

Reproduce with the README build/audit commands. Generated reports/APKs and private signing material are excluded from Git.
