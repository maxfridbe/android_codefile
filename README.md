# CodeFile — native Android

`com.TechHurts.CodeFile` · `v26.0913.007`

CodeFile uses native Android Views and the Sora `CodeEditor` widget. There is **no WebView, browser UI, JavaScript runtime, HTML, or CSS** in the app. TextMate JSON files are grammar data interpreted by a Java highlighting engine.

## Features

- Responsive phone, tablet, split-screen and foldable layouts. The ribbon spans the top of normal phone/tablet windows; separating vertical and horizontal hinges place the reader in an uninterrupted pane. Folding posture comes from Jetpack WindowManager. Wrap shows an accent fill and border while enabled, with an accessible On/Off state; Fit width adjusts text size within 8–40sp without changing Wrap. Native pinch zoom (8–40sp), zoom buttons, inertial scrolling, horizontal panning when wrapping is off, and fit-width reflow preserve document text.
- Office-style File / Search / View / Code / Navigation / Support ribbon with rounded tabs with accent top/side outlines and open bottom edges; the surrounding top bar stays unframed, 112dp command groups, outlined primary tiles, two stacked secondary actions, separators and aligned group captions. Tabs and command groups scroll horizontally when they exceed the available width. Reading scroll collapses the ribbon; scrolling back or tapping a tab expands it. Resting folds put the entire controls pane below a horizontal hinge and the code above it. CodeFile’s 40dp icon sits left of File; Settings sits right of Code. The filename row stays visible. Tapping the selected tab collapses the expanded ribbon; finger displacement controls its animation, avoiding feedback from editor layout changes.
- Read-only, selectable code with native scrolling, line numbers, wrapping, bracket matching, block guides, current-line highlighting, adjustable font/spacing, and whitespace display.
- 68 bundled language definitions / 78 TextMate grammars, including Java, Kotlin, JS/TS/JSX/TSX, Python, C/C++, C#, Rust, Go, Swift, PHP, HTML/XML, CSS, SQL, shell, Markdown, JSON/YAML/TOML and more. Manual language selection; unknown formats use plain text.
- System document picker, multiple open files, paste, VIEW/EDIT, SEND/SEND_MULTIPLE, and PROCESS_TEXT intents. Incoming text from EXTRA_TEXT, PROCESS_TEXT, or ClipData can be saved through File → Save; EXTRA_TITLE supplies the suggested filename. Original documents are never overwritten. Save creates a UTF-8 copy.
- Native source-reference gutter for formatted previews. `~` labels are approximate. Built-in Java routines reindent brace-based languages while preserving existing lines, strings and comments; JSON formatting changes only whitespace.  This is not Prettier and does not rewrite quotes or semicolons. Python and other unsupported local formats retain their meaningful whitespace.
- Native YAML node formatting retains comments, scalar styles, aliases, custom tags and multiple documents. Inline-commented flow collections retain flow style. CSV/TSV formatting preserves field values; the Navigation → Table action supports comma, tab, semicolon and pipe delimiters with quoted/multiline fields. Table parsing is off the UI thread. XML pretty printing preserves mixed content and rejects DTDs/external entities. JSON Lines compaction preserves numeric tokens. TXT/log/INI/properties/TOML/Markdown keep meaningful whitespace and normalize line endings only.
- Native unified / side-by-side diff lists, line numbers, changed-text highlights, collapsed unchanged regions, previous/next change, patch export, sharing, and analysis. Both panes share row positions and vertical scrolling; long lines wrap.
- Native settings, inline search, line navigation, standard Copy selection toolbar, selection-based snippet ranges, Markdown fencing, clipboard, share chooser, and AI responses. All app action buttons use embedded Nerd Font glyphs.
- Inline search in its own Search ribbon with live native match highlighting, match counts, next/previous, case matching, regex validation, and ordinary match highlighting without redundant search actions. A single color swatch opens the native popup; Apply highlight stores current matches as document annotations that remain when the query changes or clears. Color presets are removed. Clear search is a labeled button that removes the query and its match selection while preserving manually highlighted text. The search panel follows the ribbon background.
- Tabbed Settings pages with compact value rows and a font preview with immediate theme application; changing a theme keeps the same editor and document. Fourteen light/dark palettes plus system matching: Carbon, Midnight, Dracula, Forest, Paper, High contrast, Nord dark/light, Catppuccin Mocha/Latte, Solarized dark/light, and Rosé Pine/Dawn. Theme accents color ribbon borders and code guides. Embedded JetBrains Mono and Fira Code Nerd Fonts. User-supplied adaptive logo, legacy fallback, and monochrome themed icon.
- Document/language/navigation status is consolidated into the horizontally scrollable bottom status bar. The filename remains above the editor.
- Support ribbon includes Log .gz, service details, licenses and diagnostics. Native gzip diagnostic-log sharing with `ACTION_SEND`, `application/gzip`, a read-only content provider, and temporary URI permission. Reports omit source, prompts, responses, endpoint URLs, and credentials.

## Navigation and LSP

The Navigation ribbon exposes Symbols, Definition, References, Info, Table and Line directly. Service info is only in Support. Navigation starts automatically for each document; there is no language-server setup in Settings. Java uses an embedded JavaParser AST and symbol resolver for same-file declarations, references, symbols and hover. Other languages use a local lexical index. This does not provide project-wide semantic resolution or a full compiler for every language. Documents stay on the device during navigation.

Press a visible line number and slowly drag to select complete lines in either direction; dragging near the edge scrolls the document. Releasing opens a selection ribbon with Share, Copy, Export and Ask LLM. The analysis dialog previews the selected snippet before sending it.

Both the standard text-selection toolbar and the line-selection ribbon offer a colored highlight swatch. Tap it for a native popup containing Android RGB sliders, a hex field, Apply highlight and Clear colors. Highlight annotations follow exact selected ranges across wrapping, panning and zoom; overlapping colors replace only the overlapping range. Clear colors is also directly available when the selection intersects a highlighted range. They remain with each open document during the session and do not alter exported source. Original and formatted previews keep separate annotations; regenerating a changed preview clears its old annotations. The native line-number gutter pans with unwrapped text and returns when panning back.

File’s Sharing group and the selection ribbon include **Share PDF**. PDF export uses Android’s native PDF canvas, keeps exact selected text and highlight colors, wraps long lines, and includes displayed line numbers. With no text selected it exports the full displayed document. A read-only content URI is shared through Android’s chooser. PDF export supports up to 1 MiB of text and 500 pages.

## OpenRouter and local APIs

Settings accepts any compatible full chat completions endpoint and model ID. OpenRouter, Ollama, and LM Studio presets are included. Examples:

- `https://openrouter.ai/api/v1/chat/completions`
- `http://127.0.0.1:11434/v1/chat/completions`
- `http://192.168.1.20:1234/v1/chat/completions`

Localhost means this Android device. For a computer use its private LAN IP. Local HTTP is accepted for localhost and private/loopback/link-local IP addresses; public endpoints require HTTPS. Redirects are refused. API keys are optional for local servers, encrypted with Android Keystore, and stored separately for each exact endpoint URL. Existing endpoint keys from earlier builds remain usable. Viewer settings now use native SharedPreferences.

Only the previewed snippet is sent when **Send preview to API** or the selection dialog’s **Ask LLM** is tapped. Responses are native selectable text. Select a model actually available on your endpoint; presets are examples.

## Build

Requires Python 3 and Docker/Podman. No Node/npm or browser tooling is required. AndroidBase supplies JDK 17, Gradle 8.7, SDK 34 and build-tools 34.0.0. Native library versions are pinned in `apps/codefile/app/build.gradle.kts`.

```sh
./build.sh codefile
# Reuse an existing AndroidBase image:
SKIP_IMAGE_BUILD=1 BUILD_TASK='assembleDebug assembleRelease lint testDebugUnitTest checkNativeStyle' ./build.sh codefile
./scripts/test-java.sh
python3 scripts/audit-native-apk.py output/codefile/codefile-$(cat VERSION)-release.apk
```

APKs: `output/codefile/`. Signing uses the existing private `keystore/`, excluded from source control. Back it up for future updates. CI builds/tests and uploads artifacts; it does not publish releases.

`VERSION` uses `vYY.MMDD.###`; increment the suffix for another release that day. Android versionCode is days since 2000-01-01 × 1000 + suffix. `VERSION_NAME` can override it. Native preparation rejects any browser-runtime asset accidentally added to the APK.

## Verification and limits

Native Robolectric tests cover the Android activity and read-only editor, absence of browser Views, incoming text intents, native settings, gzip reports, touch pinch zoom, visible word wrapping, and simulated tablet/hinge layouts. Release auditing rejects WebView DEX references and browser runtime assets. Pure Java tests exercise source references, formatting, snippet fences, symbol lookup, diff patches, and API endpoint rules. A physical device is needed to verify installed document/share providers and your live API servers.

Limits: 8 MiB text per file; 20 tabs; 1 MiB for formatting/navigation; 1 MiB / 20,000 combined lines per diff; 180,000 characters per text snippet and 120,000 per analysis. UTF-8 and BOM-marked UTF-16 are accepted. Documents remain in memory and are not restored after process death. Long-running requests have timeouts.

## Open source

Bundled notices and font licenses are available in Settings. Sora is unmodified LGPL-2.1-or-later; its corresponding source is included in `third-party/source/sora-editor-0.23.4.tar.gz`. To relink a modified editor, build the source modules as AARs, replace the two Sora dependencies in the app's Gradle file with local AAR dependencies (retaining their transitive dependencies), and rebuild using this source workspace. Reverse engineering for debugging modifications of the LGPL component is permitted.

Grammar provenance: VS Code 1.95.3 extensions (MIT and upstream notices), plus Kotlin/Lua grammars from Sora's sample. Sources and licenses are listed in `apps/codefile/app/src/main/assets/NOTICES.txt`. The original supplied SVG remains in `assets/codefile-logo.svg`.

## Code quality

Native Java source is formatted with google-java-format 1.22.0. `checkNativeStyle` enforces maximum nesting for `if`, `for`, and `try` blocks in CI. Ribbon layout, fold layout, table rendering and data formatting live in separate classes; formatting and table background work use named methods to avoid nested callback chains. The installed `android-dev` skill includes a Java-specific reference used during this review.

YAML dependency: SnakeYAML Engine 2.9, Apache-2.0. The app composes data nodes and emits them without constructing arbitrary Java objects. Its license is bundled with Settings → About & diagnostics → Open-source licenses.

Ribbon design references: [Microsoft command sizing and group collapse](https://learn.microsoft.com/en-us/office/dev/add-ins/design/add-in-commands) and the classic Word ribbon's large-primary/stacked-secondary hierarchy. Implemented with native Android Views; no desktop or browser control is embedded.
