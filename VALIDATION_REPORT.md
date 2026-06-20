# Validation Report — YieldBrowser v0.9.90

## Completed checks

- Parsed all production and unit-test Java sources with `javalang`.
- Parsed all Android XML resources and the manifest.
- Checked duplicate Java method signatures.
- Verified drawable references introduced by the tab-space UI.
- Compiled and executed the pure-Java `BrowserSpacePolicy` smoke test.
- Verified version metadata and GitHub Actions artifact names.
- Verified ZIP integrity after packaging.

## Tab-space scenarios checked at source level

1. Normal tab switcher opens with **Umum** selected.
2. Selecting **Privat** brings the dedicated private task forward on Android 9+.
3. Selecting **Umum** from the private task brings the normal task forward.
4. The `+` action follows the selected space.
5. Closing the final dedicated private tab returns to the normal task.
6. Opening a bookmark in a private tab passes the URL through an intent to the private process.
7. Legacy Android 6–8 keeps the selector local because separate WebView data directories are unavailable.

## Environment limitation

A full Android Gradle build was not executed in this container because Android SDK and Gradle are not installed. The included GitHub Actions workflow remains the authoritative full build check.
