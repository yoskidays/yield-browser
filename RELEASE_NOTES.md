# Yield Browser v0.9.77

## Code Cleanup & Stability Pass

- Memperbaiki metadata build yang masih tertulis versi lama.
- Menguatkan state tab kosong agar tab baru normal/privat tidak ketempel URL dari tab sebelumnya.
- Menguatkan `saveCurrentTabState()` agar tidak menyimpan URL saat Home sedang aktif atau saat tab kosong sengaja dibuat.
- Menguatkan hapus riwayat satu item:
  - tombol hapus mengonsumsi touch sendiri,
  - mengurangi risiko row ikut terbuka,
  - panel history refresh di tempat.
- Menambah null guard kecil pada deduplikasi riwayat agar lebih aman.
- Mempertahankan seluruh fitur v0.9.76:
  - History Panel Back Fix,
  - tab/private/history fix,
  - bigger video controls,
  - YouTube auto resume after ad,
  - YouTube native skip click,
  - Edge Gesture Guard,
  - Universal Blank Compatibility,
  - Desktop/Mobile universal fix,
  - Search Engine fix,
  - Lordborg / Instant Monitor / Invest-tracing,
  - Night Mode,
  - Translate,
  - Download Manager.
