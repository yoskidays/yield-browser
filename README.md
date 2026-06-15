# Yield Browser v0.4.7 Pause Resume Fix + Home Bookmark Clear

Update ini memperbaiki dua masalah utama.

## Perubahan v0.4.7

### Pause / Resume download
- Tombol **Ⅱ** tetap untuk jeda.
- Tombol **▶** sekarang mencoba melanjutkan dari file partial yang sudah ada.
- Tombol **↻** tetap khusus untuk reload/download ulang dari awal.
- Untuk download **2 koneksi**, aplikasi menyimpan progres part 1 dan part 2.
- Untuk fallback **1 koneksi**, aplikasi mencoba resume memakai HTTP Range dari byte terakhir.
- Kalau server menolak resume, baru fallback mulai ulang sesuai respons server.

### Home dan bookmark
- Saat klik tombol **Home**, address bar dikosongkan.
- Star bookmark tidak lagi ikut menyala/nempel dari situs sebelumnya.
- Tab aktif dianggap kembali ke halaman awal.

## Catatan
Resume download bergantung pada dukungan server terhadap HTTP Range. Jika server/file host menolak resume, reload dari awal tetap tersedia lewat tombol **↻**.


## v0.5.0
- Kontrol video tidak muncul otomatis saat halaman hanya memiliki video.
- Kontrol video baru muncul saat video benar-benar diputar.
- Saat video pause/selesai, kontrol otomatis sembunyi.
- Tombol X pada kontrol video hanya menyembunyikan kontrol sesi itu, bukan mematikan fitur permanen.
- Panel Bookmark/Histori tetap memakai status bar gelap penuh.


## v0.5.1
- Mode Malam dibuat lengkap:
  - OFF
  - ON
  - Auto ikut sistem
  - Pengecualian per situs
- Mode malam sekarang juga menginjeksi CSS ke halaman web agar background gelap dan teks tetap terang/terbaca.
- Gambar, video, canvas, dan SVG tidak ikut dibuat aneh/terbalik.
- Pengaturan pengecualian situs memakai host/domain halaman aktif.


## v0.5.2
- Fix compile error: duplicate inner class `VideoBridge`.
- `VideoBridge` sekarang hanya ada 1 kali di `MainActivity.java`.
- Pemanggilan `webView.addJavascriptInterface(...)` dibersihkan agar tidak dobel.
- Fitur v0.5.1 tetap dipertahankan:
  - Mode Malam OFF / ON / Auto ikut sistem
  - pengecualian per situs
  - kontrol video hanya muncul saat video benar-benar diputar


## v0.5.3
- Ikon mode privat diganti menjadi ikon topi + kacamata (incognito style).


## v0.5.4
- Fix compile error `class, interface, or enum expected` di `onCreate`.
- Penyebabnya adalah extra closing brace `}` sebelum `VideoBridge`.
- Struktur `MainActivity` dibetulkan.
- Icon Privat topi+kacamata dari v0.5.3 tetap dipertahankan.


## v0.5.4 final fix
- Fix compile error `class, interface, or enum expected` karena extra closing brace `}`.
- Struktur `MainActivity` sudah kembali valid.
- Icon privat topi+kacamata tetap dipertahankan.


## v0.5.5
- Fix compile error `';' expected` pada `configureWebView()` dan `isSystemDarkMode()`.
- Penyebabnya: method `loadTranslatedPage()` belum ditutup dengan `}`.
- Struktur `MainActivity.java` dicek ulang: `VideoBridge` tetap 1 dan JavaScript interface tetap 1.


## v0.5.6
- Mode Malam web dibuat lebih aman agar teks tidak nyatu/ketumpuk di atas gambar.
- CSS mode malam tidak lagi memaksa semua `div`, `article`, dan card menjadi gelap/transparan.
- Bookmark dan Histori dibuat true fullscreen agar area atas tidak putih/abu-abu.
- Status bar panel Bookmark/Histori disembunyikan/digelapkan penuh.
