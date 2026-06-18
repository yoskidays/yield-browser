# Yield Browser v0.9.81

Source revisi Yield Browser v0.9.81.

Fokus update: WebView thread crash fix untuk isolated WebView per tab.

Crash yang diperbaiki:
- `shouldInterceptRequest()` memanggil `WebView.getUrl()` dari thread Chromium/background.
- Android WebView mewajibkan semua method WebView dipanggil dari main thread.
- Sekarang `shouldInterceptRequest()` hanya memakai cached URL (`currentPageUrlForRequest` / TabInfo.url), bukan memanggil method WebView langsung.

Fitur v0.9.80 tetap dipertahankan:
- Isolated WebView per tab,
- tab switch no-flicker,
- compatibility host list,
- code cleanup/stability pass,
- history panel fix,
- video controls,
- YouTube skip/auto resume,
- desktop/mobile universal fix,
- universal blank compatibility,
- download manager.
