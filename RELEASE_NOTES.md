# Yield Browser v0.9.81

## WebView Thread Crash Fix

- Memperbaiki crash fatal:
  - `A WebView method was called on thread 'ThreadPoolForeg'`
  - lokasi: `MainActivity.shouldInterceptRequest()`
- Penyebab: `shouldInterceptRequest()` memanggil `view.getUrl()` dari background thread Chromium.
- Fix: request interception sekarang memakai cached page URL, bukan memanggil method WebView dari background thread.
- Tetap aman untuk isolated WebView per tab karena URL aktif diperbarui dari callback main thread (`onPageStarted`, `onPageCommitVisible`, `onPageFinished`).

## Dipertahankan dari v0.9.80

- Isolated WebView per tab.
- Tab tidak reload ulang saat switch.
- Inactive WebView tidak mengubah UI/address tab aktif.
- Auto-close hanya untuk `adTab`.
- Compatibility Lordborg / Instant Monitor / Invest-Tracing.
- YouTube native skip + auto resume.
- Bigger video controls.
- History panel back fix.
- Universal blank compatibility.
- Desktop/Mobile universal fix.
- Night Mode, Translate, Download Manager.
