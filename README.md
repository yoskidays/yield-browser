# Yield Browser 0.10.15

Browser Android native dengan download manager, pemutaran video saat download berlangsung, mode privat, Shield Engine, riwayat, bookmark, dan reader mode.

## Perbaikan 0.10.15

- Panel kontrol video lengkap dipindahkan keluar dari lapisan `SurfaceView`, sehingga tidak lagi tertutup renderer video pada Realme 5 Pro Android 11.
- Mode portrait menampilkan kontrol dalam dua baris: mundur 10 detik, play/pause, maju 10 detik, kecepatan, Auto, dan Full.
- Mode landscape menampilkan keenam kontrol dalam satu baris.
- Ketuk video untuk menampilkan atau menyembunyikan panel; auto-hide tetap aktif saat video berjalan.
- Seekbar, waktu berjalan, durasi, progres download, renderer fallback, dan Media3 ExoPlayer 1.6.1 tetap dipertahankan.

## Build

- compileSdk 35
- targetSdk 35
- minSdk 23
- Android Gradle Plugin 8.7.3
- Media3 ExoPlayer 1.6.1
- Java 17

Build APK melalui workflow GitHub Actions pada `.github/workflows/build-apk.yml`.
