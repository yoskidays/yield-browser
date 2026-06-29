# Yield Browser 0.10.13

Browser Android native dengan download manager, pemutaran video saat download berlangsung, mode privat, Shield Engine, riwayat, bookmark, dan reader mode.

## Perbaikan 0.10.13

- Kontrol video dapat dimunculkan kembali dengan mengetuk area video pada SurfaceView maupun TextureView.
- Kontrol ditampilkan setelah frame pertama benar-benar muncul, sehingga tidak terlanjur tersembunyi saat video masih disiapkan.
- Waktu auto-hide kontrol diperpanjang menjadi 6 detik.
- Saat video dijeda atau buffering, kontrol tetap ditampilkan.
- Lapisan kontrol diberi prioritas di atas renderer video.
- Perbaikan pemutaran Realme 5 Pro Android 11 dan Media3 1.6.1 tetap dipertahankan.

## Build

- compileSdk 35
- targetSdk 35
- minSdk 23
- Android Gradle Plugin 8.7.3
- Media3 ExoPlayer 1.6.1
- Java 17

Build APK melalui workflow GitHub Actions pada `.github/workflows/build-apk.yml`.
