# Yield Browser v0.1.5 UI Fix

Yield Browser adalah browser Android ringan berbasis WebView.

## Perubahan v0.1.5

- Address/search bar dibuat lebih panjang dan nyaman dipakai.
- Menu icon dipisah ke navigation toolbar agar tidak sempit.
- Semua tombol menu tetap memakai drawable/vector icon, bukan text glyph.
- Tambahan progress bar tipis saat halaman loading.
- Keyboard action GO/Search aktif dari address bar.
- Target test: Android 11 Realme 5 Pro.

## Build APK via GitHub Actions

1. Upload isi folder project ini ke repository GitHub.
2. Buka tab Actions.
3. Jalankan workflow `Build Yield Browser APK`.
4. Download artifact `YieldBrowser-debug-apk`.
5. Extract ZIP artifact dan install APK di Android.

## Status fitur

- Browser dasar: aktif.
- UI icon drawable: aktif.
- Download manager multi-connection: tahap berikutnya.
