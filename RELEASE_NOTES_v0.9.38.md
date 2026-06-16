# Yield Browser v0.9.38

## Fokus perbaikan
- Refactor ulang Desktop/Mobile Mode agar meniru pola browser stabil.
- Saat Desktop Mode ON/OFF, WebView dibuat ulang untuk membersihkan sisa User-Agent, wide viewport, overview mode, scale, dan DOM lama.
- Mobile Mode tidak lagi memakai injeksi JavaScript viewport yang bisa bentrok dengan Google/situs lain.
- Desktop Mode dan Mobile Mode sekarang memakai profil WebSettings terpisah: Mobile Profile dan Desktop Profile.
- Default Desktop Mode dipaksa OFF sekali setelah update v0.9.38.

## File utama yang diubah
- app/src/main/java/com/yieldbrowser/app/MainActivity.java
- app/build.gradle

## Catatan pengujian
1. Buka Google dengan Desktop Mode OFF.
2. Cari `komiknesia`, pastikan tampilan mobile normal.
3. Aktifkan Desktop Mode, halaman harus reload ke desktop.
4. Matikan Desktop Mode, halaman harus reload ulang melalui WebView baru dan kembali mobile.
5. Tutup aplikasi lalu buka lagi, Desktop Mode harus tetap OFF secara default.
