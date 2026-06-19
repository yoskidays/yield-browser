# Release Notes — YieldBrowser v0.9.85

## Download integrity

- Memvalidasi `206 Partial Content`, `Content-Range`, total file, dan panjang respons untuk setiap worker Range.
- File multipart hanya selesai jika seluruh part dan ukuran fisik file sesuai.
- Memperbaiki pembagian dua koneksi menjadi rentang tanpa overlap.
- Memperbaiki resume Balanced 3 dan Turbo 4.
- Menambahkan `ETag`, `Last-Modified`, dan `If-Range` untuk mencegah file lama dan baru tergabung.
- Respons Range yang tidak valid otomatis turun ke mode aman tanpa mempertahankan file sparse yang berisiko korup.

## Queue, pause, dan concurrency

- Download baru tidak lagi melewati antrean.
- Remove langsung menutup stream dan koneksi aktif.
- Generation token membatalkan worker lama setelah pause, reload, remove, atau retry.
- State lintas thread dibuat visible dan update kritis dilindungi lock/atomic counter.
- Retry penalty tidak lagi dihitung dua kali.
- Urutan antrean dipersistenkan secara deterministik.

## Google Drive

- Normalisasi link file Google Drive ke endpoint download.
- Final redirect URL disimpan dan digunakan ulang oleh semua part.
- URL bertoken yang expired otomatis di-resolve ulang.
- File besar Google Drive memakai 3 koneksi adaptif; fallback tetap tersedia bila Range/throttling tidak stabil.
- Header browser palsu yang tidak relevan dihapus dari request download.

## HLS

- Resume per segmen dan rollback partial segment.
- Variant master dipilih berdasarkan bandwidth tertinggi.
- Dukungan init map, byte range, fMP4, serta AES-128 CBC.
- Key rotation dasar, explicit IV, dan media-sequence IV didukung.
- Metode enkripsi yang tidak didukung ditolak agar tidak menghasilkan file palsu atau korup.

## Background dan platform

- Foreground data-sync service dan partial wake lock untuk download aktif.
- Permission notifikasi Android 13+ diminta sekali saat download pertama.
- targetSdk diperbarui ke 35.

## Tests

- Codec state download dan migrasi running-to-paused.
- Parser/validator HTTP Range.
- Parser master/media HLS, fMP4 map, byte range, dan encryption metadata.
- AES-128 decrypt menggunakan media sequence IV.
