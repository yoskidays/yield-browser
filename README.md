# Yield Browser v0.9.43

Patch stabilitas untuk crash WebView thread pada `shouldInterceptRequest`.

## Perubahan
- Fix crash: `A WebView method was called on thread ThreadPoolForeg`.
- `shouldInterceptRequest()` tidak lagi memanggil `WebView.getUrl()` dari background thread.
- URL halaman aktif disimpan di cache `volatile` dari thread utama (`onPageStarted`, `onPageFinished`, dan `loadBrowserUrl`).
- Desktop/Mobile mode v0.9.40 tetap tidak disentuh.
- Lordborg navigation compatibility v0.9.42 tetap dipertahankan.

## Test utama
1. Buka Google dan klik hasil `lordborg.com` dengan AdBlock ON.
2. Buka langsung `https://lordborg.com`.
3. Klik menu internal Lordborg.
4. Pastikan aplikasi tidak force close.
5. Pastikan Desktop/Mobile mode tetap stabil.

## v0.9.45
Fix direct image navigation guard: prevents comic pages from being replaced by raw .jpeg/.jpg web image pages; temporary direct-link tabs are restored.


## v0.9.47
Night Mode ON/OFF state fix: ForceDark/AlgorithmicDarkening sync, light guard for history/back-forward pages, and hard reload when toggling night mode.
