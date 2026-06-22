# Validation Report — Yield Browser v0.10.01

## Verified

- `versionCode 75` and `versionName 0.10.01`.
- Shield policy Java sources compile with `javac`.
- Legitimate chapter-to-chapter navigation is allowed when the source is already a chapter page.
- Previous chapter navigation is allowed.
- Same-origin relay `/r/<opaque-token>` remains blocked.
- Generated Shield document-start JavaScript passes `node --check`.
- Main Android XML and Manifest files are well-formed.
- Full source preserves application files, resources, tests, workflow, and documentation.

## Environment limitation

A full Android Gradle build and device-level WebView test were not run locally because this environment does not include the Android SDK/Gradle runtime. The included GitHub Actions workflow performs unit tests, lint, and APK builds.
