# YieldBrowser v0.9.84 - Safe Brave-Class Download Layer v3

Build ini melanjutkan basis stabil v0.9.84 dan hanya menambahkan lapisan download yang lebih adaptif.

## Fokus
- Core tab/session tetap tidak disentuh.
- YouTube Assistant tetap tidak disentuh.
- AdBlock dan universal download allowlist tetap tidak disentuh.
- Perubahan hanya pada download engine.

## Download Layer v3
- Safe 1 koneksi untuk file kecil/server rapuh.
- Stable 2 koneksi untuk Google Drive/OneDrive/Mega/SharePoint dan host sensitif.
- Balanced 3 koneksi untuk server menengah atau unknown host yang cukup kuat.
- Turbo 4 koneksi untuk video besar/CDN/host kuat.

## Proteksi
- Hard pause: stream dan HTTP connection aktif diputus saat pause.
- Resume state per part tetap dipertahankan.
- Jika Turbo 4 mulai tidak stabil, engine menurunkan worker aktif ke Balanced/Stable tanpa mengubah core browser.
- UI sekarang membedakan koneksi aktif: 1/2/3/4.

## Catatan
Versi ini adalah safe layer, bukan penggantian total network stack. Tujuannya meningkatkan stabilitas download tanpa merusak fitur browser yang sudah stabil.
