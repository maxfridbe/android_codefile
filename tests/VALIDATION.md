# Native release validation — v26.0913.002

Package: `com.TechHurts.CodeFile`; versionCode: `9752002`; minimum Android API 24, target/compile API 34.

- `assembleDebug assembleRelease lint testDebugUnitTest checkNativeStyle`: passed.
- 40 JUnit/Robolectric tests: passed, no failures or skips.
- 11 additional API endpoint checks: passed.
- google-java-format 1.22.0 dry-run and Git whitespace checks: passed.
- Checkstyle rejects excessive nested `if`, `for`, and `try` blocks; CI runs the check.
- Theme selection immediately repaints the existing editor without replacing its document. All 14 palettes load native syntax themes; foreground contrast is at least 4.5:1 and accent contrast at least 3:1 against the editor background.
- Slow upward gutter dragging selects complete lines, excluding the following line; selection actions include Share and Ask LLM.
- Active-tab clicks collapse/reopen the ribbon without hiding the filename. Finger movement changes ribbon height proportionally; editor scroll notifications cannot feed layout changes back into ribbon height. Pinch and selection-handle gestures do not drive ribbon movement.
- Embedded Java service resolves a shadowed local variable to its same-file declaration, initializes without a network endpoint, and provides a labelled lexical fallback for other languages.
- Office-style ribbon groups use a stable 112dp height, shared caption baselines, outlined primary tiles and two stacked secondary commands. Tabs stay left-aligned and no wider than 88dp on tablets; the active tab is visibly selected. Phone/tablet native bitmaps inspected.
- Ribbon actions remain within their native layout at 320/360/411/600/840dp widths. Touch drags collapse and reopen the ribbon. Command content wider than the viewport is reachable through horizontal scrolling. Logs are in Support.
- Simulated horizontal hinge: code above, full controls pane below. Vertical hinge and tablet layouts also pass; normal tablets now place the ribbon across the top.
- Native pinch zoom and visible wrapping preserve source text and source line numbers.
- Settings category tabs switch independently scrolling pages; native viewer and Settings bitmaps visually inspected. CodeFile identity sits left of File; the Settings gear sits right of Code. Settings Save remains outside its scrolling content.
- Selection swatch opens a native RGB/hex color picker. Tests apply and remove a multi-line highlight, preserve source text and retain annotations through configuration refresh. Range tests cover overlapping colors and partial removal; native highlight bitmap inspected.
- Standard native selection toolbar is enabled and its Copy action writes the selected text to Android’s clipboard.
- Search is inline in a separate Search ribbon, with tests asserting that opening it creates no dialog, File has no search panel, and search color controls are absent. Search tests cover case-insensitive/case-sensitive results, next-match selection, malformed regex handling, clearing.
- YAML tests cover comments, quoted scalars, block content, indentation, aliases, custom tags, multiple documents, invalid input and idempotence. Inline-commented flow collections retain flow layout to preserve their comments.
- CSV/TSV tests cover escaped quotes, multiline fields, empty fields, leading zeros, significant spaces and malformed quotes. JSON Lines preserves large numeric tokens; Markdown preserves hard-break spaces. XML preserves mixed content and rejects DTDs/external entities.
- All bundled grammar catalog entries load. Data-table records are parsed off the UI thread.
- Release ZIP/DEX audit: no WebView references or HTML/CSS/JavaScript/WASM assets; bundled fonts and grammar catalogs present.
- APK signature verification: passed, Signature Scheme v2, one signer. Package/version metadata matches this release.

Release SHA-256:

```
568c30301e77838505138db0017c3d67196f5544b39ddb7120c2c493caab8c22
```

The installed `android-dev` skill's Java guidance informed the review. Ribbon geometry, fold geometry, table rendering, asynchronous search bindings, and data formatting are separate classes. Background formatter/table operations use named helpers to avoid nested callback chains.

No physical Android device was attached. Actual Galaxy Z Fold posture changes, installed document/share providers, live API services, and TalkBack remain device integration checks. Robolectric tests run against debug classes; release validation covers packaging, shrinking and signing rather than execution on a device.

Reproduce with the README build/audit commands. Generated reports/APKs and private signing material are excluded from Git.
