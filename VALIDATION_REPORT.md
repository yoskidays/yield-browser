# Validation Report — Yield Browser v0.10.05

## Version

- `versionCode 79`
- `versionName 0.10.05`
- GitHub Actions APK artifact names updated to v0.10.05.

## Persistent element-filter checks

- Picker script compiles as Java 17 and the generated JavaScript passes `node --check`.
- Picker bar contains a dedicated `__yield_picker_close` control and a persistent `__yieldPickerContinue` path.
- Blocking an element no longer commits or closes the picker; Android calls the continue hook after saving and applying the selector.
- The Android bridge supports selector preview, match count, and selected tag metadata.
- `UserElementFilterPolicy` rejects critical targets and CSS-injection characters while accepting normal generated selectors.
- Critical tags protected in both JavaScript and Java: `html`, `body`, `head`, `script`, `style`, `link`, `meta`, `title`, `video`, `audio`, and `source`.
- Pointer events are preferred over simultaneous pointer/touch/mouse listeners; fallback events are debounced.
- User filter application no longer checks the AdBlock master flag.
- User filter application no longer excludes compatibility mode.
- Saved filters are applied at page commit, page finish, live-tab activation, and after Shield/AdBlock settings change.
- A mutation observer restores the user stylesheet if a dynamic page removes it.
- CSS rules are emitted separately per selector, preventing one stale selector from invalidating the entire stylesheet.
- Picker cleanup hooks cover navigation, tab switching, Home, active WebView destruction, and Activity pause.

## Regression checks retained

- Reader-safe Next/Previous Chapter navigation remains enabled under Click Hijack Protection.
- Same-origin advertising relay paths remain blocked.
- Android Back destroys the active WebView when returning to Home with no earlier history.
- Long-press links open in a newly activated tab and reject non-web schemes.

## Test inventory

- 18 JVM test classes.
- 56 `@Test` cases in the project source.
- New coverage: `ElementPickerScriptTest` and `UserElementFilterPolicyTest`.

## Packaging checks

- Complete project contains 150 files.
- Part 1 contains exactly 100 files.
- Part 2 contains exactly 50 files.
- Archive paths do not overlap.
- Reconstructed archive contents must match the source project byte-for-byte before delivery.

## Environment limitation

A full Android SDK/Gradle build was not executed in the local container because Gradle and Android SDK are not installed. The included GitHub Actions workflow performs JVM tests, Android lint, debug APK build, and unsigned release APK build on push.
