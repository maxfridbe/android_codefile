# Store release handoff

Target: Google Play unless the owner selects another store. No store upload or publishing has been performed.

## Artifacts

Run `scripts/release-store.sh` using the existing private signing material. It creates a signed APK, signed Android App Bundle and R8 mapping file in output/codefile. Preserve the signing key and mappings privately. Play App Signing enrollment/upload-key configuration is an owner action; do not replace the installed-app signing key without planning the distribution consequences.

## Code and packaging

- Package remains com.TechHurts.CodeFile; version names follow vYY.MMDD.### with monotonically increasing numeric version codes.
- Android API 36 target/compile; minimum API 24; AGP 8.10.1, Gradle 8.11.1, Java 17.
- Edge-to-edge system-bar, display-cutout and IME insets handled by native views.
- Release shrinking/obfuscation and resource shrinking enabled; no WebView runtime.
- INTERNET is the sole Android platform permission. AndroidX also declares an app-scoped signature permission for non-exported receivers. Local HTTP endpoints remain supported intentionally; public API endpoints require HTTPS.
- No native .so libraries: 16 KB ELF alignment does not apply. The store audit fails if native libraries are introduced so they must then receive an explicit alignment audit.
- App-private credentials use Android Keystore encryption; backup/device transfer disabled.
- In-app privacy policy under Support. Policy text and listing copy are included here.

## Owner and device gates before submission

- Supply developer/support email and a publicly hosted privacy-policy URL; ensure the policy's contact route matches the listing.
- Review Data safety answers in data-safety.md against the chosen API providers and actual deployment. Do not declare all traffic encrypted if users can configure local HTTP.
- Complete content rating, target audience, countries/pricing, developer verification, app access and any Play testing requirements applicable to the account.
- Capture genuine release screenshots on representative phone/tablet/fold devices. Native test renders in app/build are development evidence, not a substitute for installed-release screenshots.
- Run the signed release on API 24 and API 36, including gesture navigation, keyboard, rotation/fold posture, incoming shares, file Save, PDF serialization and API analysis. No physical device was attached during preparation.
- Run scripts/benchmark-startup.sh on a representative device, record before/after cold-start timings and inspect startup timing fields in Support logs. Structural optimizations are implemented; no device speedup percentage is claimed.
- Upload the AAB to an internal test track, resolve Play pre-launch/device/policy feedback, then approve production publishing separately.

## Primary requirement references (checked September 13, 2026)

- Target API: https://support.google.com/googleplay/android-developer/answer/11926878
- 16 KB pages: https://developer.android.com/guide/practices/page-sizes
- API 36 behavior and edge-to-edge: https://developer.android.com/about/versions/16/behavior-changes-16
- User data/privacy: https://support.google.com/googleplay/android-developer/answer/10144311
- Data safety: https://support.google.com/googleplay/android-developer/answer/10787469
- AI policy scope: https://support.google.com/googleplay/android-developer/answer/14094294

CodeFile is a viewer/productivity tool with optional code analysis, not a general-purpose chatbot. Review the AI policy scope if that positioning or functionality changes; this is not a guarantee of Play approval.
