# Yield Browser v0.3.7 Compile Fix + Download Notification Handler

Update ini memperbaiki error build dari v0.3.5.

## Perbaikan v0.3.7

- Menambahkan method yang hilang: `handleOpenDownloadsIntent(Intent intent)`.
- Error compile `cannot find symbol handleOpenDownloadsIntent(Intent)` sudah diperbaiki.
- Klik notifikasi download tetap diarahkan ke menu **Download / Unduhan Yield**.
- Menghapus pemanggilan lama yang bisa membuka dialog download ganda.
- Fitur v0.3.5 tetap dipertahankan:
  - Jeda / Pause
  - Lanjutkan
  - Reload dari awal
  - 1 koneksi / 2 koneksi paralel
  - Open
  - Hapus riwayat
  - Hapus file + riwayat
  - tampilan koneksi download gaya IDM

## Catatan

Build gagal sebelumnya bukan karena limit GitHub, tetapi karena method handler notifikasi belum masuk ke source Java.
