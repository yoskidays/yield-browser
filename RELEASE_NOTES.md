# Yield Browser v0.9.84 — Smart Tab Isolation Guard

Patch fokus untuk memperkuat multi-tab agar URL/state dari tab yang baru ditutup tidak menimpa tab lain.

## Perubahan utama
- Menambahkan `isolationHost` per tab.
- Menambahkan `currentPageUrlForRequest` per tab agar request/resource tidak memakai cache URL global yang bisa stale.
- Menambahkan trusted navigation window per tab untuk navigasi user yang sah.
- `saveCurrentTabState()` sekarang hanya menyimpan URL dari WebView milik tab aktif.
- Address bar tidak lagi dipakai sebagai fallback jika tab aktif masih punya live WebView, untuk mencegah URL tab tertutup tersalin ke tab lain.
- Session tab menyimpan host isolasi sebagai kolom tambahan yang tetap kompatibel dengan format lama.
- `restoreAfterBlockedNavigation()` sekarang memakai fallback URL milik tab terkait, bukan `lastSafeHttpUrl` global.

## Yang tidak disentuh
- YouTube assistant / icon skip / forward +10s.
- AdTab guard.
- Persistent tabs yang sudah stabil.
- Filter direct-link iklan yang sudah stabil.

# Yield Browser v0.9.84 — Universal Download AdBlock Allowlist

Patch fokus agar tombol download asli tetap bisa berjalan saat AdBlock aktif.

## Perubahan utama
- Menambahkan `isTrustedDownloadIntentUrl()` sebagai jalur aman khusus URL download/file.
- URL seperti `drive.usercontent.google.com/download?...&export=download&authuser=0` tidak lagi dianggap iklan/direct-link.
- Global AdBlock JS sekarang mengenali link download asli dan tidak memblokir click/window.open download.
- Network intercept tidak memblokir resource/halaman download yang valid.
- Tetap menolak URL yang mengandung token iklan keras seperti `adclick`, `adurl=`, `click_id`, `popunder`, dan host iklan berisiko.

## Yang tidak disentuh
- Smart Tab Isolation Guard.
- Tab session/restore tab.
- YouTube assistant, icon skip, dan forward +10s.
- AdTab guard dan direct-link ad guard yang sudah stabil.

## v0.9.84 Smart Turbo Download Engine v2

Fokus rilis ini hanya engine download. Bagian tab/session, YouTube assistant, dan adblock/tab guard tidak diubah.

### Download Engine
- Menambahkan Smart Turbo Download profile: Safe 1 koneksi, Stable 2 koneksi, Turbo 4 koneksi.
- Pemilihan koneksi berdasarkan host, ukuran file, tipe file, dan dukungan Range.
- Host sensitif seperti Google Drive / Googleusercontent / OneDrive / Mega diarahkan ke mode stabil agar pause/resume lebih aman.
- File kecil diarahkan ke 1 koneksi untuk mengurangi overhead.
- File video besar/CDN diarahkan ke Turbo 4 koneksi bila server mendukung.
- Menambahkan bandwidth prediction ringan: moving average speed, stability score, slow sample counter.
- Menambahkan hard pause: saat pause, koneksi HTTP dan InputStream aktif diputus supaya tombol pause lebih responsif.
- Progress multi-part disimpan lebih sering saat berjalan agar resume lebih aman.
- Resume 4-part lama dapat dijalankan dengan worker stabil 2 koneksi jika sistem mendeteksi host/sesi lebih sensitif.

### Catatan
- Turbo v2 tetap membatasi maksimal 4 koneksi karena struktur part existing hanya memiliki state part1-part4. Ini sengaja untuk menjaga kompatibilitas dan menghindari bug baru.
