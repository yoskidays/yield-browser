# Yield Browser 0.10.16

Browser Android native dengan download manager, pemutaran video saat download berlangsung, mode privat, Shield Engine, riwayat, bookmark, reader mode, dan kontrol menu yang dapat disesuaikan.

## Perbaikan 0.10.16

- Shortcut **Blokir elemen** sekarang dapat diaktifkan atau dinonaktifkan melalui **Sesuaikan menu**.
- Shortcut **Filter situs ini** sekarang dapat diaktifkan atau dinonaktifkan melalui **Sesuaikan menu**.
- Kedua shortcut tetap aktif secara default agar perilaku pengguna lama tidak berubah.
- Saat shortcut dimatikan, hanya item pada menu utama yang disembunyikan; filter elemen yang sudah tersimpan tetap aman dan tetap diterapkan pada situs.
- Panel kontrol video lengkap, stabilitas Realme 5 Pro Android 11, dan Media3 ExoPlayer 1.6.1 tetap dipertahankan.

## Build

- compileSdk 35
- targetSdk 35
- minSdk 23
- Android Gradle Plugin 8.7.3
- Media3 ExoPlayer 1.6.1
- Java 17

Build APK melalui workflow GitHub Actions pada `.github/workflows/build-apk.yml`.
