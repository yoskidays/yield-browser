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
