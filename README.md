# Yield Browser v0.9.80

Source revisi Yield Browser v0.9.80.

Fokus update: **Isolated WebView Per Tab** untuk memperbaiki tab compatibility yang saling menimpa URL dan mengurangi reload saat berpindah tab.

Ringkasan:
- Setiap tab memiliki WebView sendiri.
- Tab Lordborg/Invest-Tracing/compatibility tidak lagi menimpa tab Komiknesia atau tab lain.
- Saat pindah tab, browser menampilkan WebView tab tersebut, bukan memuat ulang URL dari awal.
- Callback WebView dari tab yang sedang tidak aktif tidak lagi mengubah UI/tab aktif.
- Auto-close tab iklan tetap ada dan hanya menutup tab yang `adTab = true`.
- Fitur stabil dari versi sebelumnya tetap dipertahankan.
