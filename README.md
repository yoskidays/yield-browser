# Yield Browser v0.3.6 Build Fix + Download Manager Stabilization

Update ini memperbaiki kemungkinan gagal build dari versi sebelumnya dan menstabilkan download manager.

## Perbaikan v0.3.6

- Mengganti icon notifikasi dari `android.R.drawable.stat_sys_download` ke icon drawable aplikasi agar aman saat compile.
- Mengganti beberapa icon item file dari `android.R.drawable` ke drawable internal Yield.
- Memperbaiki refresh progress download agar aman dipanggil dari background thread.
- Menghapus pemanggilan dialog unduhan ganda saat klik notifikasi.
- Fitur v0.3.5 tetap ada:
  - notifikasi klik langsung buka menu unduhan
  - pause/jeda
  - lanjutkan
  - reload dari awal
  - 1 koneksi / 2 koneksi paralel
  - menu Open, Bagikan, Ganti nama, Hapus riwayat, Hapus file + riwayat

## Catatan

Gunakan versi ini untuk menggantikan v0.3.5 kalau GitHub Actions gagal build.
