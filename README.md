# Yield Browser 0.10.14

Browser Android native dengan download manager, pemutaran video saat download berlangsung, mode privat, Shield Engine, riwayat, bookmark, dan reader mode.

## Perbaikan 0.10.14

- Panel kontrol video lengkap kini tersedia pada player download: mundur 10 detik, play/pause, maju 10 detik, kecepatan, kualitas Auto, dan Fullscreen.
- Panel dibuat responsif agar keenam tombol tetap muat dan dapat digunakan pada mode portrait Realme 5 Pro Android 11.
- Kontrol lengkap muncul saat area video diketuk dan otomatis tersembunyi setelah enam detik ketika video sedang berjalan.
- Tombol kecepatan menyediakan 0.5x, 0.75x, 1x, 1.25x, 1.5x, dan 2x.
- Tombol Full mengaktifkan fullscreen landscape; tombol Exit atau tombol kembali mengembalikan player ke portrait.
- Seekbar, waktu berjalan, durasi, dan progres data yang tersedia tetap ditampilkan di bawah panel tombol.
- Perbaikan layar hitam Realme 5 Pro Android 11 dan Media3 ExoPlayer 1.6.1 tetap dipertahankan.

## Build

- compileSdk 35
- targetSdk 35
- minSdk 23
- Android Gradle Plugin 8.7.3
- Media3 ExoPlayer 1.6.1
- Java 17

Build APK melalui workflow GitHub Actions pada `.github/workflows/build-apk.yml`.
