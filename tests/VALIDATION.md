# Native release validation — v26.0913.013

Package: `com.TechHurts.CodeFile`; versionCode: `9752013`; minimum Android API 24, target/compile API 36.

- `assembleDebug assembleRelease bundleRelease lint testDebugUnitTest checkNativeStyle`: passed.
- 54 JUnit/Robolectric tests: passed, no failures or skips.
- 11 additional API endpoint checks: passed.
- google-java-format 1.22.0 dry-run and Git whitespace checks: passed.
- Checkstyle rejects excessive nested `if`, `for`, and `try` blocks; CI runs the check.
- Whitespace markers explicitly use the editor NON_PRINTABLE_CHAR color: neutral #777777 on light themes and #858585 on dark themes, reapplied with appearance changes.
- Startup tests verify one eagerly loaded theme, cached fonts, no extra welcome tab for incoming text, and preserved cursor when syntax arrives. Diagnostics write asynchronously and flush before gzip sharing. Device first-frame/cold-start timing remains unmeasured because no device is attached.
- Store packaging targets API 36 with AGP 8.10.1/Gradle 8.11.1, native edge-to-edge/IME insets, signed AAB output, release resource shrinking and bundled privacy disclosure.
- Theme selection immediately repaints the existing editor without replacing its document. All 14 palettes load native syntax themes; foreground contrast is at least 4.5:1 and accent contrast at least 3:1 against the editor background.
- Slow upward gutter dragging selects complete lines, excluding the following line; selection actions include Share and Ask LLM.
- Manual collapse by the toggle or selected tab blocks drag/fling expansion; explicit expansion releases the lock and restores scroll tracking.
- Active-tab clicks collapse/reopen the ribbon without hiding the filename. Finger movement changes ribbon height proportionally; editor scroll notifications cannot feed layout changes back into ribbon height. Pinch and selection-handle gestures do not drive ribbon movement.
- Embedded Java service resolves a shadowed local variable to its same-file declaration, initializes without a network endpoint, and provides a labelled lexical fallback for other languages.
- Office-style ribbon groups use a stable 112dp height, shared caption baselines, outlined primary tiles and two stacked secondary commands. Tabs stay left-aligned and no wider than 88dp on tablets; the active tab is visibly selected. Phone/tablet native bitmaps inspected.
- MD/JSON ribbon visibility follows file switches. Markdown tests inspect rendered native spans including code and extensions; JSON tests cover nested types, exact large numeric values, strict malformed-input rejection and expand/collapse. Markwon/CommonMark license texts are bundled.
- App icon opens Settings; Settings is in Support, and Go to line is in Search rather than View/Navigation. Symbol regression filters names case-insensitively, inspects code preview and jumps to the highlighted destination without changing source text.
- Navigation opens inline with directly visible Symbols, Definition, References, Info and Table actions; opening the tab creates no dialog. Service info is absent from Code and remains in Support.
- Ribbon actions remain within their native layout at 320/360/411/600/840dp widths. Touch drags collapse and reopen the ribbon. Command content wider than the viewport is reachable through horizontal scrolling. Logs are in Support.
- Simulated horizontal hinge: code above, full controls pane below. Vertical hinge and tablet layouts also pass; normal tablets now place the ribbon across the top.
- Wrap uses an accent-filled selected state while enabled and exposes On/Off to accessibility. Fit width adjusts text size within 8–40sp and preserves wrapping; regression checks cover both states and configuration refresh.
- Native pinch zoom and visible wrapping preserve source text and source line numbers.
- Settings category tabs switch independently scrolling pages; native viewer and Settings bitmaps visually inspected. CodeFile’s icon is 40dp (twice its previous size), and the top bar has no surrounding outline. Individual tabs keep accent top and side outlines, with no bottom border or underline. The ribbon upper border joins the active tab sides, leaving a gap underneath that tab; the gap follows horizontal tab scrolling. The icon sits left of File without the CodeFile text label; the Settings gear is in Support and the app icon opens Settings. Settings Save remains outside its scrolling content.
- Selection swatch opens a PopupWindow containing native SeekBars and a hex EditText with no preset colors; it no longer creates a dialog. Clear colors is directly enabled for intersecting colored selections. Tests apply and remove a multi-line highlight, preserve source text and retain annotations through configuration refresh. Range tests cover overlapping colors and partial removal; native highlight bitmap inspected.
- Incoming ClipData text is accepted with its suggested filename. File → Save launches ACTION_CREATE_DOCUMENT and writes exact UTF-8 text through the content resolver.
- Pan regression covers horizontal gestures starting in the source/number gutter at 8, 14, 26 and 40sp, fixed editor width, no accidental selection and no ribbon drag. Source references share the native renderer rather than resizing a sibling view.
- PDF syntax tests capture exact selected keyword colors, bold/italic emphasis, theme background, spacing, source references and manual annotations; the styled native page bitmap is inspected.
- PDF tests clip annotation spans to exact selected text, paginate the native text layout, check rendered highlight color pixels, and verify application/pdf provider MIME. The same page drawing code feeds Android PdfDocument canvases. Android PdfDocument’s file writer is not implemented by this Robolectric version; actual PDF serialization and installed share targets remain device checks.
- Standard native selection toolbar is enabled and its Copy action writes the selected text to Android’s clipboard.
- Search has a single swatch opening the shared native color popup. Tests apply a search color to exact case-sensitive matches, verify annotations remain after changing and clearing the query, verify color persistence after configuration refresh, verify the matching ribbon surface, and check that labeled Clear search removes the query/match selection without changing manual highlight annotations. Native Search screenshot inspected.
- Search is inline in a separate Search ribbon, with tests asserting that opening it creates no dialog, File has no search panel, and there are no duplicate search actions or exposed preset-color rows. Search tests cover case-insensitive/case-sensitive results, next-match selection, malformed regex handling, clearing.
- YAML tests cover comments, quoted scalars, block content, indentation, aliases, custom tags, multiple documents, invalid input and idempotence. Inline-commented flow collections retain flow layout to preserve their comments.
- CSV/TSV tests cover escaped quotes, multiline fields, empty fields, leading zeros, significant spaces and malformed quotes. JSON Lines preserves large numeric tokens; Markdown preserves hard-break spaces. XML preserves mixed content and rejects DTDs/external entities.
- All bundled grammar catalog entries load. Data-table records are parsed off the UI thread.
- Release ZIP/DEX audit: no WebView references or HTML/CSS/JavaScript/WASM assets; bundled fonts and grammar catalogs present.
- Official bundletool 1.18.3 validates the AAB. JAR signature verification passes; JDK reports the expected self-signed certificate and AGP ZIP manifest-order streaming warnings. APK/AAB audits find no native .so files or embedded signing keys, and include the privacy policy. AAB SHA-256: `8643cf2e2d74dacad240baec0dc446e360a2c7e8446724ea3d72352f39324348`.
- APK signature verification: passed, Signature Scheme v2, one signer. Package/version metadata matches this release.

Release SHA-256:

```
0f79d2198181b65bfecfb2b9e5bfe96b5a6f5716f3e4a7d08d171c30683fbb89
```

The installed `android-dev` skill's Java guidance informed the review. Ribbon geometry, fold geometry, table rendering, asynchronous search bindings, and data formatting are separate classes. Background formatter/table operations use named helpers to avoid nested callback chains.

No physical Android device was attached. Actual Galaxy Z Fold posture changes, installed document/share providers, live API services, and TalkBack remain device integration checks. Robolectric tests run against debug classes; release validation covers packaging, shrinking and signing rather than execution on a device.

Reproduce with the README build/audit commands. Generated reports/APKs and private signing material are excluded from Git.
