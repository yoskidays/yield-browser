# Yield Browser v0.9.37

## Strict Mobile Restore Fix

- Memaksa Desktop Mode default OFF satu kali setelah update agar sisa preferensi desktop lama tidak nyangkut.
- Semua navigasi halaman utama sekarang lewat `loadBrowserUrl()` supaya User-Agent sesuai mode saat ini.
- Mobile Mode memakai User-Agent Chrome Mobile murni tanpa marker WebView/desktop.
- Saat Desktop Mode dimatikan, WebView di-reset lewat `about:blank`, cache ringan dibersihkan, lalu URL dimuat ulang dengan header mobile.
- Tidak mengubah fitur download, translate, history, bookmark, pintasan, adblock, atau kontrol video selain cara load URL agar mode mobile/desktop konsisten.
