# Validation Report — Yield Browser v0.10.07

## Version

- `versionCode 81`
- `versionName 0.10.07`
- GitHub Actions APK artifact names updated to v0.10.07.


## Universal search navigation regression checks

- Search-result pages are explicitly excluded from the strict reader boundary in both Java and the document-start Shield script.
- Regional/non-`.com` search domains are recognized without relying on the browser's selected search-engine setting.
- Generic SearX/SearXNG-style pages are recognized from a search-like path plus query parameter.
- Clean external result links remain clickable on Google Indonesia, Bing, DuckDuckGo, Yahoo, Yandex, Brave Search, and SearX-style pages.
- Known ad hosts and hard advertising-token URLs remain subject to Shield blocking on search pages.
- Komiku chapter pages still block unknown cross-site direct-link takeovers.
- Fourteen focused JVM unit tests passed for Shield Engine V2 and its generated page script.
- Eight executable JavaScript navigation scenarios passed under Node.js.
- The generated Shield script passed `node --check`.
- All 61 project JVM test methods passed in the local Java 17 harness.

## Reader direct-link regression checks

- Unknown cross-site main-frame navigation is blocked when the source is a reader/content page, even when the request carries a user gesture.
- Same-site chapter navigation and cross-site direct image/media assets remain allowed.
- Explicitly trusted navigation can still leave a reader page.
- Generated Shield script contains touch, pointer, mouse, click, auxiliary-click, and submit guards.
- Touch listeners use `passive:false`, and touch coordinates are normalized before click-through recovery.
- `about:blank`, `about:srcdoc`, and empty HTML data documents are classified as transient blank pages.
- Reader recovery is deterministic and reloads the tab-owned last safe URL.
- A standalone Java 17 regression harness passed 14 focused Shield checks.

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
- 61 `@Test` cases in the project source.
- New coverage includes universal search-result navigation, regional search domains, self-hosted search URL shapes, reader cross-site takeover, transient blank-page recovery, and mobile touch/pointer guard assertions.

## Packaging checks

- Complete project contains 150 files.
- Part 1 contains exactly 100 files.
- Part 2 contains exactly 50 files.
- Archive paths do not overlap.
- Reconstructed archive contents must match the source project byte-for-byte before delivery.

## Environment limitation

A full Android SDK/Gradle build was not executed in the local container because Gradle and Android SDK are not installed. The included GitHub Actions workflow performs JVM tests, Android lint, debug APK build, and unsigned release APK build on push.
