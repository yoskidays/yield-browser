# Validation Report — YieldBrowser v0.9.99

## Completed static checks

- `MainActivity.java`, `ShieldEngineV2.java`, `ReaderCompatibilityPolicy.java`, and `QuietToast.java` parsed successfully with a Java parser.
- No duplicate method signature was found in `MainActivity` or its nested bridge classes.
- `ShieldEngineV2` and `ShieldPageScript` compiled with Java 17 in an isolated policy test.
- Policy tests passed for:
  - same-origin `/r/<token>` reader relay blocking;
  - clean chapter navigation;
  - known cross-site ad host blocking;
  - clean external link allowance;
  - reader CDN image allowance;
  - known ad script blocking.
- Generated document-start JavaScript passed `node --check`.
- 46 Android XML/Manifest files parsed successfully.
- All app drawable references were found, including `drawable-nodpi`; Android framework drawable references were excluded from the app-resource check.
- No direct `Toast.makeText` call exists outside `QuietToast`.
- No Shield counter, blocked-count badge, or popup/redirect toast was added.
- Version verified as `versionCode 73` and `versionName 0.9.99`.

## Packaging checks

- GitHub web-upload package contains exactly 100 files.
- Full source ZIP includes tests and documentation.
- Both ZIP archives pass archive integrity testing.

## Not executed in this environment

- Full Android Gradle build.
- Android Lint through the Android SDK.
- Instrumentation test on an Android 11/API 30 emulator.
- Live WebView test against current advertising scripts on third-party sites.

The included GitHub Actions workflow remains the build source of truth for APK compilation and Android unit/lint checks.
