# Yield Browser v0.9.79

## Tab Switch No-Flicker Fix

- Memperbaiki efek kedip konten WebView dari tab lain saat pindah tab.
- Saat berpindah tab, WebView lama ditutup overlay singkat sebelum state tab baru di-restore/load.
- Jika `restoreState()` berhasil, overlay ditutup cepat agar tab terasa responsif.
- Jika tab harus load ulang, overlay tetap menjaga agar konten tab lama tidak terlihat sampai halaman baru siap.
- Fallback timeout ditambahkan agar overlay tidak nyangkut jika halaman tidak memanggil `onPageFinished`.
- Fitur v0.9.78 dan sebelumnya tetap dipertahankan.
