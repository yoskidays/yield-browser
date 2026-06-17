# Yield Browser v0.9.60

## Compatibility Desktop Mode Fix

- Memperbaiki Desktop Mode pada situs compatibility seperti Lordborg dan situs sejenis.
- Saat mode kompatibel aktif, toggle Desktop ON sekarang tetap menerapkan desktop profile/viewport.
- Request situs compatibility di Desktop Mode memakai User-Agent desktop minimal tanpa header Sec-CH agresif agar tidak memicu security/blank.
- Mobile Mode pada situs compatibility tetap kembali normal saat Desktop OFF.
- Tidak mengubah adblock situs umum yang sudah stabil.
- Tidak mengubah YouTube adblock, Night Mode, Direct Image Guard, Download Manager, Translate, dan kontrol video fullscreen.
