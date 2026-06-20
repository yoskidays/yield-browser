# YieldBrowser v0.9.94 — Android 11 Night Mode Fix

Versi ini memperbaiki Mode Malam yang sebelumnya dapat terlihat **ON** tetapi tidak mengubah halaman pada sebagian perangkat Android 11. Universal Reader Compatibility Repair juga diperluas untuk gambar reader biasa yang rusak atau tersembunyi, bukan hanya gambar dengan atribut lazy-load.

## Mode malam Android 11

- Tetap memakai AndroidX WebKit algorithmic darkening ketika provider mendukungnya.
- Tidak lagi menganggap status “supported” sebagai bukti bahwa halaman sudah benar-benar digelapkan.
- Selalu memasang fallback DOM yang reversibel dan terlihat nyata.
- Memetakan latar putih/terang menjadi palet gelap, teks gelap menjadi teks terang, serta border dan kontrol formulir menjadi warna ramah malam.
- Memproses elemen halaman secara bertahap agar UI tidak membeku pada halaman panjang.
- Memantau konten yang ditambahkan secara dinamis.
- Tidak memberi filter warna pada gambar, video, canvas, SVG, picture, dan iframe.
- Saat Mode Malam dimatikan, style inline yang diubah dipulihkan.
- Tetap diterapkan pada Mode Kompatibel.

## Universal Reader Compatibility Repair

- Tidak memakai whitelist domain.
- Mendukung `data-src`, `data-lazy-src`, `data-original`, `data-srcset`, `<picture><source>`, lazy background, `src`, dan `srcset` biasa.
- Mencoba ulang gambar standar yang rusak atau tersembunyi pada halaman chapter/episode/reader.
- Menampilkan kembali gambar dengan `display:none`, `visibility:hidden`, `opacity:0`, atau atribut `hidden` bila terdeteksi sebagai konten reader.
- Tetap mengecualikan banner, sponsor, affiliate, popup, dan URL jaringan iklan.

## GitHub Web Upload Edition

Gunakan ZIP dengan nama `YieldBrowser_v0.9.94_GitHub_upload_98_files.zip` untuk upload melalui halaman web GitHub. Paket tersebut berisi **98 file**, tetap buildable, dan tidak menyertakan unit test serta dokumen ringkasan historis agar tidak melewati batas 100 file per upload.

## Build

Project menggunakan Java 17, compile/target SDK 35, AndroidX WebKit 1.15.0, dan GitHub Actions dengan Gradle 8.10.2.

```bash
gradle assembleDebug --stacktrace
gradle assembleRelease --stacktrace
```

Version metadata:

- `versionCode 68`
- `versionName 0.9.94`
