# Yield Browser v0.9.84 — Smart Tab Isolation Guard

Patch fokus untuk memperkuat multi-tab agar URL/state dari tab yang baru ditutup tidak menimpa tab lain.

## Perubahan utama
- Menambahkan `isolationHost` per tab.
- Menambahkan `currentPageUrlForRequest` per tab agar request/resource tidak memakai cache URL global yang bisa stale.
- Menambahkan trusted navigation window per tab untuk navigasi user yang sah.
- `saveCurrentTabState()` sekarang hanya menyimpan URL dari WebView milik tab aktif.
- Address bar tidak lagi dipakai sebagai fallback jika tab aktif masih punya live WebView, untuk mencegah URL tab tertutup tersalin ke tab lain.
- Session tab menyimpan host isolasi sebagai kolom tambahan yang tetap kompatibel dengan format lama.
- `restoreAfterBlockedNavigation()` sekarang memakai fallback URL milik tab terkait, bukan `lastSafeHttpUrl` global.

## Yang tidak disentuh
- YouTube assistant / icon skip / forward +10s.
- AdTab guard.
- Persistent tabs yang sudah stabil.
- Filter direct-link iklan yang sudah stabil.
