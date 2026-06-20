# YieldBrowser v0.9.89 — Smooth Download Manager

YieldBrowser v0.9.89 memperbarui pengalaman unduhan file besar agar lebih responsif, informatif, dan stabil tanpa mengubah fondasi download engine v0.9.85–v0.9.87.

## Download Manager baru

- Menggunakan `RecyclerView`, `ListAdapter`, `DiffUtil`, dan stable item ID.
- Perubahan progres hanya memperbarui baris file yang berubah; seluruh daftar tidak lagi dibuat ulang.
- Tampilan dipisahkan menjadi **Mengunduh** dan **Selesai**.
- Setiap item menampilkan nama file, ukuran diterima/total, progres, kecepatan stabil, estimasi waktu, serta aksi pause/resume/retry.
- Informasi teknis koneksi tidak lagi mendominasi daftar utama.
- Daftar kosong dan hasil pencarian kosong mempunyai empty state yang jelas.

## Progres file besar

- Progres internal menggunakan skala 0–10.000 agar gerak progress bar lebih halus.
- UI diperbarui melalui ticker 300 ms dan progress bar dianimasikan.
- Kecepatan memakai exponential moving average agar angka tidak meloncat tajam.
- ETA dihitung dari throughput yang sudah distabilkan.
- Statistik penyimpanan dibatasi pembaruannya setiap 2 detik agar tidak membebani UI.

## Tahap akhir unduhan

File multipart tetap menggunakan staging file karena membutuhkan random-access write. Tahap akhir sekarang terlihat jelas:

1. **Memverifikasi file…**
2. **Menyimpan ke Downloads…** dengan progres tersendiri
3. **Selesai**

Setelah ekspor berhasil, staging file dihapus agar tidak menyimpan dua salinan. Jika proses berhenti saat finalisasi, item dipulihkan sebagai status yang dapat dilanjutkan.

## Interaksi unduhan

Download Manager tidak lagi dibuka paksa ketika unduhan dimulai. Sebagai gantinya, Yield menampilkan banner kecil **Unduhan dimulai** dengan tombol **Lihat**.

## Fondasi yang tetap dipertahankan

- Queue manager dan batas download aktif
- Multipart adaptif 1–4 koneksi
- Resume tervalidasi
- Google Drive resolved URL caching dan fallback adaptif
- HLS, fMP4, byte-range, dan AES-128
- Profil incognito terisolasi
- Satu notifikasi progres download tanpa notifikasi background tambahan

## Build

- Android Gradle Plugin 8.7.3
- Gradle 8.10.2
- Java 17
- compileSdk 35
- targetSdk 35
- versionCode 63
- versionName 0.9.89

```bash
gradle testDebugUnitTest
gradle assembleDebug
gradle assembleRelease
```
