# Validation Report — YieldBrowser v0.9.85

## Passed

- 23 Java source/test files parsed successfully.
- 45 Android XML files parsed successfully.
- No duplicate private method signatures in `MainActivity`.
- No private methods without references in `MainActivity`.
- No rough unused imports in `MainActivity`.
- 14 standalone unit tests passed:
  - storage encoding
  - current and legacy download-history codec
  - HTTP Content-Range parser
  - HLS master/media parser
  - HLS byte ranges and init map
  - HLS encryption metadata
  - AES-128 CBC decryption
- Pure Java HTTP protocol self-test rejects HTTP 200 and mismatched Range responses.
- Version metadata and workflow artifacts aligned to v0.9.85.

## Environment limitation

A complete Android APK was not compiled locally because this runtime does not contain Android SDK/Gradle and external binary downloads are unavailable. The included GitHub workflow runs Gradle unit tests and builds debug/release APK artifacts.
