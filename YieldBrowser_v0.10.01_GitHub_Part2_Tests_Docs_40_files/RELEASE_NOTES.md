# Yield Browser v0.10.01

- Fixed Previous/Next chapter buttons being blocked by Shield Engine V2.
- Added a safe same-site reader-navigation lane for chapter and episode URLs.
- Expanded reader path recognition to embedded slug forms such as `-chapter-`, `-episode-`, and `-read-online-`.
- Protected legitimate reader navigation in the document-start click guard and programmatic anchor/form hooks.
- Kept same-origin ad relays such as `/r/`, `/go/`, `/out/`, and `/redirect/` blocked.
- Added regression tests for chapter-to-chapter navigation and relay blocking.
- Updated APK artifact names.
- `versionCode 75`, `versionName 0.10.01`.

# YieldBrowser v0.10.00

- Shield Engine V2 tetap aktif sebagai mesin internal AdBlock.
- Tidak ada menu Shield terpisah.
- Tombol menu utama kembali menjadi `AdBlock ON/OFF`.
- Pengaturan lanjutan dibuka dari baris AdBlock yang sama.
- Tidak ada statistik blokir, badge, atau toast rutin.
- `versionCode 74`, `versionName 0.10.00`.
