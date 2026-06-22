# YieldBrowser v0.9.99

Versi ini menambahkan **Shield Engine V2** untuk popup, redirect, click hijack, same-origin ad relay, script/iframe iklan, dan overlay transparan pada situs reader maupun situs umum.

## Perubahan utama

- Document-start protection melalui AndroidX WebKit dengan fallback untuk WebView lama.
- Same-origin relay protection untuk pola `/r/`, `/go/`, `/out/`, `/redirect/`, dan pola serupa.
- Navigation firewall sebelum tab utama meninggalkan halaman aman.
- Network filtering selektif agar gambar komik, media, font, dan CDN konten tidak ikut diblokir.
- Safe URL preservation agar redirect iklan tidak menghasilkan halaman putih.
- UI Shield bersih tanpa statistik, badge hitungan, atau toast rutin.
- Seluruh fitur v0.9.98 tetap dipertahankan, termasuk HTTPS-First, History Engine V2, mode malam Android 11, Universal Reader Repair, ruang Umum/Privat, dan download manager.

## Versi

- `versionCode 73`
- `versionName 0.9.99`

## Build

Workflow GitHub Actions berada di `.github/workflows/build-apk.yml` dan menjalankan unit test, lint, debug APK, serta unsigned release APK.

## v0.10.00 — AdBlock Integrated UI
Shield Engine V2 adalah mesin internal. Seluruh kontrol pengguna tetap berada pada tombol dan pengaturan AdBlock yang sudah ada; tidak dibuat menu Shield terpisah.
