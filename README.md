# Yield Browser v0.10.09

Yield Browser adalah proyek browser Android native berbasis Java 17 dengan `minSdk 23` dan `targetSdk 35`.

## Perbaikan terbaru

Versi 0.10.09 memperbaiki layar hitam ketika video dibuka saat proses unduhan masih berjalan.

- Server progresif mempertahankan ukuran total dan rentang media yang benar.
- Pemutaran menunggu data awal yang cukup sebelum player dimulai.
- URL CDN hasil redirect serta header autentikasi digunakan kembali.
- Video MP4 dengan metadata di bagian akhir dapat beralih ke streaming sumber.
- Player otomatis memakai jalur cadangan saat persiapan atau frame pertama macet.
- Download utama tetap berjalan ketika player berpindah ke jalur streaming cadangan.
- Kesalahan codec atau render ditampilkan dengan jelas dan tidak dibiarkan menjadi layar hitam.
- Finalisasi video berukuran besar tetap berjalan di background agar tidak memicu ANR.

## Versi aplikasi

- `versionCode 83`
- `versionName 0.10.09`
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

Workflow GitHub Actions di `.github/workflows/build-apk.yml` dapat digunakan untuk membangun APK debug. Unggah kedua paket ke root repository yang sama, kemudian jalankan workflow dari menu Actions.
