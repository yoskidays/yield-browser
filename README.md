# Yield Browser v0.2.0 UC-style Settings + Yield Downloader

Update ini memasukkan menu-menu ala UC ke dalam **Setelan** sesuai arahan.

## Perubahan v0.2.0

- Posisi menu utama Yield tetap dipertahankan.
- Fitur ala UC dimasukkan ke **Menu > Setelan**.
- Tidak memasukkan fitur cloud/sync.
- Menu Setelan berisi:
  - Mode cepat
  - Safe browsing
  - Night mode
  - Reader / novel mode
  - Ad block sederhana
  - Hemat data
  - Desktop mode
  - Ukuran teks
  - Bersihkan cache
  - Download manager Yield
- Download manager Yield memakai konsep **2 koneksi paralel** menggunakan HTTP Range.
- Jika server tidak mendukung Range, otomatis fallback ke 1 koneksi.
- Icon fitur baru dibuat sebagai drawable/vector modern.

## Catatan penting

Fitur download 2 koneksi bekerja optimal kalau server mendukung `Accept-Ranges` / HTTP Range.
File tersimpan di folder app external files:
`Android/data/com.yieldbrowser.install/files/Download`
