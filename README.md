# Yield Browser 0.10.18

Browser Android native dengan download manager, Shield Engine, mode privat, reader mode, dan pemutar video progresif berbasis Media3.

## Perbaikan 0.10.18

- Shield memakai click destination lock pada situs berisiko iklan dan popunder.
- Klik tautan asli pada situs seperti DramaEncode tidak lagi mudah disertai popup atau pengalihan ke domain iklan lain.
- `window.open`, klik sintetis, `location.assign`, dan `location.replace` diperiksa selama jendela gestur pengguna.
- Domain yang memiliki sinyal iklan kuat tetap diblokir walaupun navigasi bermula dari sentuhan pengguna.
- Workflow mendukung APK release dengan tanda tangan tetap agar versi berikutnya dapat dipasang sebagai update tanpa menghapus data aplikasi.
- Debug APK tetap dibuat untuk pengujian. Gunakan artifact `YieldBrowser-stable-release-apk` untuk penggunaan harian setelah signing secrets dikonfigurasi.
- Perbaikan video progresif, kontrol lengkap, kompatibilitas Realme Android 11, dan Media3 1.6.1 tetap dipertahankan.

## Versi

- versionCode: 92
- versionName: 0.10.18
- minSdk: 23
- targetSdk: 35
- compileSdk: 35

## Pembaruan APK dan bookmark

Petunjuk kunci signing tetap dan migrasi dari APK debug lama tersedia di [`docs/STABLE_APK_UPDATES.md`](docs/STABLE_APK_UPDATES.md).
