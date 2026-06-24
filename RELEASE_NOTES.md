# Yield Browser v0.10.07

- Fixed search results becoming completely unclickable after the v0.10.06 reader direct-link hardening.
- Removed `search`, `article`, `post`, and `category` from the strict reader-path classifier so ordinary content and search pages do not inherit the reader cross-domain boundary.
- Added a universal search-results detector shared by the native navigation policy and Shield Engine V2 page script.
- Supports regional Google/Yahoo/Yandex domains and popular engines including Bing, DuckDuckGo, Baidu, Brave Search, Startpage, Ecosia, Qwant, Mojeek, Kagi, Naver, AOL, Ask, Swisscows, and MetaGer.
- Supports self-hosted SearX/SearXNG-style engines through search-path plus query-parameter detection.
- Search result links may navigate to clean external sites, while known ad hosts, dangerous schemes, and hard advertising tokens remain blocked.
- Search pages are excluded from universal blank-page compatibility recovery.
- Added regression tests for eleven search-engine URL shapes and retained reader takeover blocking checks.
- Added an executable JavaScript behavior test confirming seven search engines remain clickable while Komiku reader direct-links are still blocked.
- Corrected the multipart progressive-playback test expectation to match the contiguous written range (`500 + 350 = 850`).
- `versionCode 81`, `versionName 0.10.07`.

# Yield Browser v0.10.06

- Hardened reader pages against unknown cross-site direct-link takeovers, including destinations that do not match an existing ad-domain list.
- Stopped treating an inherited Android WebView `hasGesture` flag as consent to leave a reader page.
- Preserved same-site chapter navigation, direct content assets, explicitly trusted search/address-bar flows, and trusted downloads.
- Added capture-phase guards for `touchstart`, `touchend`, `pointerdown`, `pointerup`, `mousedown`, and `mouseup`.
- Added non-passive touch listeners and normalized coordinates for touch-based overlay recovery.
- Classified `about:blank`, `about:srcdoc`, and empty HTML data documents as transient popup staging pages.
- Changed reader recovery to load the tab-owned last safe URL directly instead of using `goBack()` through polluted ad history.
- Prevented the temporary tab domain-switch allowance from whitelisting a stolen reader click.
- Added regression coverage for unknown clean cross-site destinations, trusted reader exits, CDN image assets, generated touch guards, and transient blank URLs.
- Updated APK artifact names.
- `versionCode 80`, `versionName 0.10.06`.

# Yield Browser v0.10.05

- Changed **Blokir elemen** into a continuous picker that remains active after each block.
- Added an explicit **X** button on the picker bar to exit without reopening the browser menu.
- Changed the confirmation action to **Blokir & lanjut** and added **Batal pilihan** so cancelling one target does not close the picker.
- Made manual cosmetic filters independent from the AdBlock master switch.
- Manual filters now remain active when AdBlock is OFF and on compatibility/reader pages.
- Persisted filters are reapplied when the same host is opened from bookmarks, history, restored tabs, or a fresh navigation.
- Added resilient stylesheet reinjection for dynamic/SPA pages that replace the document head or remove injected styles.
- Added matched-element counts to the confirmation dialog.
- Improved selector stability by preferring stable unique IDs/classes and using `:nth-of-type` only when necessary.
- Added Android-side selector validation and protection for critical document/media elements.
- Added PointerEvent/touch/mouse deduplication to prevent duplicate dialogs from one tap.
- Picker state is now cleared safely on navigation, tab changes, Home, WebView destruction, and Activity backgrounding.
- Added `ElementPickerScript` and `UserElementFilterPolicy` regression tests.
- Updated APK artifact names.
- `versionCode 79`, `versionName 0.10.05`.

# Yield Browser v0.10.04

- Fixed Next/Previous Chapter controls becoming unresponsive when **Proteksi Click Hijack** was enabled.
- Reader/content pages now use Shield Engine V2 as the single click-hijack guard, avoiding duplicate event interception by the legacy premium listener.
- Added click-through recovery for suspicious transparent overlays covering legitimate reader navigation controls.
- A blocked overlay click can now resolve and open the safe same-site chapter link underneath the touch point.
- Added support for reader controls using anchors, buttons, common `data-*` URL attributes, and simple JavaScript navigation handlers.
- Retained blocking for known advertising hosts, dangerous external schemes, opaque cross-site destinations, and same-origin relay URLs.
- Added regression coverage for the current Komiku chapter URL pattern, duplicate-guard policy, and generated Shield script recovery hooks.
- Updated APK artifact names.
- `versionCode 78`, `versionName 0.10.04`.

# Yield Browser v0.10.03

- Fixed a page continuing to load or run behind the Home screen after Android Back.
- Android Back now resets the current tab and destroys its WebView when no earlier page history remains.
- Stops JavaScript, media, redirects, network requests, and late callbacks from the page being left.
- Clears stale browser progress state whenever Home is shown.
- Preserves normal `goBack()` behavior while WebView history is still available.
- Updated APK artifact names.
- `versionCode 77`, `versionName 0.10.03`.

# Yield Browser v0.10.02

- Added press-and-hold detection for normal links inside WebView pages.
- Added **Buka link di tab baru** context action.
- The selected destination opens immediately in a newly created active tab.
- Supports text links and images wrapped by anchors.
- Resolves absolute, relative, root-relative, and protocol-relative HTTP/HTTPS URLs.
- Rejects non-web schemes before creating a tab.
- Added regression tests for long-press link URL resolution.
- Updated APK artifact names.
- `versionCode 76`, `versionName 0.10.02`.

# Yield Browser v0.10.01

- Fixed Previous/Next chapter buttons being blocked by Shield Engine V2.
- Added a safe same-site reader-navigation lane for chapter and episode URLs.
- Expanded reader path recognition to embedded slug forms such as `-chapter-`, `-episode-`, and `-read-online-`.
- Protected legitimate reader navigation in the document-start click guard and programmatic anchor/form hooks.
- Kept same-origin ad relays such as `/r/`, `/go/`, `/out/`, and `/redirect/` blocked.
- Added regression tests for chapter-to-chapter navigation and relay blocking.
- Updated APK artifact names.
- `versionCode 75`, `versionName 0.10.01`.

# YieldBrowser v0.10.00

- Shield Engine V2 tetap aktif sebagai mesin internal AdBlock.
- Tidak ada menu Shield terpisah.
- Tombol menu utama kembali menjadi `AdBlock ON/OFF`.
- Pengaturan lanjutan dibuka dari baris AdBlock yang sama.
- Tidak ada statistik blokir, badge, atau toast rutin.
- `versionCode 74`, `versionName 0.10.00`.
