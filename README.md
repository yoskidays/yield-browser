# Yield Browser v0.10.12

Yield Browser adalah browser Android native berbasis Java 17 dengan `minSdk 23` dan `targetSdk 35`.

## Perbaikan terbaru

Versi 0.10.12 memfokuskan kompatibilitas pemutar video internal pada Android 11, termasuk Realme 5 Pro.

- Pemutar internal menggunakan AndroidX Media3 ExoPlayer 1.6.1.
- Realme Android 11 memulai pemutaran dengan `SurfaceView`.
- Jika waktu atau suara berjalan tetapi gambar belum muncul, player otomatis mencoba ulang dengan `TextureView`.
- Jika data lokal belum dapat dirender, player mencoba sumber video asli sambil download tetap berjalan.
- Decoder fallback ExoPlayer diaktifkan.
- Header `User-Agent`, `Referer`, `Origin`, dan `Cookie` tetap diteruskan saat memakai sumber asli.
- Layar pemuatan baru ditutup setelah frame video pertama benar-benar dirender.
- Rasio video menyesuaikan mode portrait, landscape, dan Picture-in-Picture.
- Finalisasi file besar tetap dilakukan di background agar tidak memicu ANR.

## Kompatibilitas build

- Media3 dikunci ke versi `1.6.1` agar kompatibel dengan `compileSdk 35` dan Android Gradle Plugin `8.7.3`.
- Perbaikan ini mengatasi kegagalan `:app:checkDebugAarMetadata` yang sebelumnya muncul karena Media3 `1.10.1` membutuhkan `compileSdk 36`.
- `targetSdk` tetap `35` dan aplikasi tetap dapat dipasang pada Android 11 karena `minSdk` tetap `23`.

## Versi aplikasi

- `versionCode 86`
- `versionName 0.10.12`
- `minSdk 23`
- `targetSdk 35`

## Struktur utama

- `.github/workflows/build-apk.yml`
- `app/build.gradle`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/yieldbrowser/app/`
- `app/src/main/res/`
- `app/src/test/java/com/yieldbrowser/app/`

## Build APK

Upload kedua paket ke root repository yang sama. Setelah itu, jalankan workflow build dari menu **Actions** di GitHub.
