# Validation Report — Yield Browser v0.10.04

## Version

- `versionCode 78`
- `versionName 0.10.04`
- GitHub Actions APK artifact names updated to v0.10.04.

## Reader-safe Click Hijack checks

- Current Komiku chapter URL pattern is recognized as a safe same-site reader navigation.
- A transition from chapter 304 to chapter 305 is allowed by the native Shield policy.
- Same-origin relay URLs such as `/r/<opaque-token>` remain blocked.
- Reader/content pages disable the duplicate legacy premium click listener while Shield Engine V2 remains active.
- Ordinary non-reader pages retain the legacy listener when Click Hijack Protection is enabled.
- Generated document-start JavaScript passes Java compilation and `node --check` syntax validation.
- Mocked DOM runtime validation confirms that a blocked `onclickads.net` transparent overlay is cancelled and the underlying same-site next-chapter URL is opened.
- Direct legitimate next-chapter clicks are not cancelled or reported as advertising.
- `elementsFromPoint` recovery has an `elementFromPoint` fallback for older WebView engines.

## Previous fixes retained

- Android Back destroys the active WebView when returning to Home with no earlier history.
- Long-press links can open in a newly activated tab.
- HTTP/HTTPS absolute, relative, and protocol-relative long-press destinations remain supported.
- Non-web long-press schemes remain rejected.

## Packaging checks

- Project contains 146 files.
- Part 1 contains exactly 100 files.
- Part 2 contains exactly 46 files.
- Archive file paths do not overlap.
- Reconstructed archive contents match the source project byte-for-byte.

## Environment limitation

A full Android SDK/Gradle build was not executed in the local container because Gradle and Android SDK are not installed. The included GitHub Actions workflow performs unit tests, Android lint, debug APK build, and unsigned release APK build on push.
