# CodeFile — native Android

`com.TechHurts.CodeFile` · `v26.0912.003`

CodeFile uses native Android Views and the Sora `CodeEditor` widget. There is **no WebView, browser UI, JavaScript runtime, HTML, or CSS** in the app. TextMate JSON files are grammar data interpreted by a Java highlighting engine.

## Features

- Responsive phone, tablet, split-screen and foldable layouts. Expanded windows use a side control pane; separating vertical and horizontal hinges place the reader in an uninterrupted pane. Folding posture comes from Jetpack WindowManager. Native pinch zoom (8–40sp), zoom buttons, inertial scrolling, horizontal panning when wrapping is off, and fit-width reflow preserve document text.
- Read-only, selectable code with native scrolling, line numbers, wrapping, bracket matching, block guides, current-line highlighting, adjustable font/spacing, and whitespace display.
- 68 bundled language definitions / 78 TextMate grammars, including Java, Kotlin, JS/TS/JSX/TSX, Python, C/C++, C#, Rust, Go, Swift, PHP, HTML/XML, CSS, SQL, shell, Markdown, JSON/YAML/TOML and more. Manual language selection; unknown formats use plain text.
- System document picker, multiple open files, paste, VIEW/EDIT, SEND/SEND_MULTIPLE, and PROCESS_TEXT intents. Original documents are never overwritten. Export creates a UTF-8 copy.
- Native source-reference gutter for formatted previews. `~` labels are approximate. Built-in Java routines reindent brace-based languages while preserving existing lines, strings and comments; JSON formatting changes only whitespace. A connected language server can supply full language-specific formatting. This is not Prettier and does not rewrite quotes or semicolons. Python and other unsupported local formats require a server formatter.
- Native unified / side-by-side diff lists, line numbers, changed-text highlights, collapsed unchanged regions, previous/next change, patch export, sharing, and analysis. Both panes share row positions and vertical scrolling; long lines wrap.
- Native settings, search, line navigation, selection-based snippet ranges, Markdown fencing, clipboard, share chooser, and AI responses. All app action buttons use embedded Nerd Font glyphs.
- Six palettes plus system matching. Embedded JetBrains Mono and Fira Code Nerd Fonts. User-supplied adaptive logo, legacy fallback, and monochrome themed icon.
- Native gzip diagnostic-log sharing with `ACTION_SEND`, `application/gzip`, a read-only content provider, and temporary URI permission. Reports omit source, prompts, responses, endpoint URLs, and credentials.

## Navigation and LSP

**Enable navigation** activates a native lexical symbol index immediately if no language server is configured. Symbols, declaration candidates and word occurrences work in the current document. This local index is not a semantic compiler or a language server; it does not resolve imports or infer types.

Set a `wss://` JSON-RPC language-server gateway once in Settings to use the same button for semantic navigation. Configure workspace root, optional actual document URI, and optional language ID. The server must already be running and exchange one JSON-RPC message per WebSocket message. Native OkHttp handles the connection, initialization, document open/close, symbols, definitions, references, hover, and formatting. UTF-16 positions are required. Enabling sends the original document to that server. Definitions outside the current document show a URI and line for manual opening through Android's document picker.

## OpenRouter and local APIs

Settings accepts any compatible full chat completions endpoint and model ID. OpenRouter, Ollama, and LM Studio presets are included. Examples:

- `https://openrouter.ai/api/v1/chat/completions`
- `http://127.0.0.1:11434/v1/chat/completions`
- `http://192.168.1.20:1234/v1/chat/completions`

Localhost means this Android device. For a computer use its private LAN IP. Local HTTP is accepted for localhost and private/loopback/link-local IP addresses; public endpoints require HTTPS. Redirects are refused. API keys are optional for local servers, encrypted with Android Keystore, and stored separately for each exact endpoint URL. Existing endpoint keys from earlier builds remain usable. Viewer settings now use native SharedPreferences.

Only the visible share preview is sent when **Send preview to API** is tapped. Responses are native selectable text. Select a model actually available on your endpoint; presets are examples.

## Build

Requires Python 3 and Docker/Podman. No Node/npm or browser tooling is required. AndroidBase supplies JDK 17, Gradle 8.7, SDK 34 and build-tools 34.0.0. Native library versions are pinned in `apps/codefile/app/build.gradle.kts`.

```sh
./build.sh codefile
# Reuse an existing AndroidBase image:
SKIP_IMAGE_BUILD=1 BUILD_TASK='assembleDebug assembleRelease lint testDebugUnitTest' ./build.sh codefile
./scripts/test-java.sh
python3 scripts/audit-native-apk.py output/codefile/codefile-$(cat VERSION)-release.apk
```

APKs: `output/codefile/`. Signing uses the existing private `keystore/`, excluded from source control. Back it up for future updates. CI builds/tests and uploads artifacts; it does not publish releases.

`VERSION` uses `vYY.MMDD.###`; increment the suffix for another release that day. Android versionCode is days since 2000-01-01 × 1000 + suffix. `VERSION_NAME` can override it. Native preparation rejects any browser-runtime asset accidentally added to the APK.

## Verification and limits

Native Robolectric tests cover the Android activity and read-only editor, absence of browser Views, incoming text intents, native settings, gzip reports, touch pinch zoom, visible word wrapping, and simulated tablet/hinge layouts. Release auditing rejects WebView DEX references and browser runtime assets. Pure Java tests exercise source references, formatting, snippet fences, symbol lookup, diff patches, and API endpoint rules. A physical device is needed to verify installed document/share providers and your live API/LSP servers.

Limits: 8 MiB text per file; 20 tabs; 1 MiB for formatting/navigation; 1 MiB / 20,000 combined lines per diff; 180,000 characters per text snippet and 120,000 per analysis. UTF-8 and BOM-marked UTF-16 are accepted. Documents remain in memory and are not restored after process death. Long-running requests have timeouts.

## Open source

Bundled notices and font licenses are available in Settings. Sora is unmodified LGPL-2.1-or-later; its corresponding source is included in `third-party/source/sora-editor-0.23.4.tar.gz`. To relink a modified editor, build the source modules as AARs, replace the two Sora dependencies in the app's Gradle file with local AAR dependencies (retaining their transitive dependencies), and rebuild using this source workspace. Reverse engineering for debugging modifications of the LGPL component is permitted.

Grammar provenance: VS Code 1.95.3 extensions (MIT and upstream notices), plus Kotlin/Lua grammars from Sora's sample. Sources and licenses are listed in `apps/codefile/app/src/main/assets/NOTICES.txt`. The original supplied SVG remains in `assets/codefile-logo.svg`.
