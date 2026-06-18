# Yield Browser v0.9.80

## Isolated WebView Per Tab

- Menambahkan isolasi WebView per tab.
- Memperbaiki bug ketika tab compatibility seperti Lordborg / Invest-Tracing membuat tab lain ikut membuka URL yang sama.
- Memperbaiki perpindahan tab agar tab lama tidak selalu reload ulang saat dibuka kembali.
- WebView tab yang tidak aktif disembunyikan dan callback-nya tidak lagi mengubah UI/address/tab aktif.
- Hard reload Desktop/Mobile sekarang hanya mengganti WebView tab aktif, bukan mengganggu tab lain.
- Auto-close tab iklan tetap dipertahankan dan hanya menutup tab yang benar-benar `adTab = true`.
- Tab normal/manual tidak ditutup otomatis meskipun URL-nya mirip popup/ad host.

## Dipertahankan

- Tab switch no-flicker fix v0.9.79.
- Multi-tab compatibility host list v0.9.78.
- Code cleanup v0.9.77.
- History Panel Back Fix.
- Bigger Video Controls.
- YouTube Auto Resume After Ad.
- YouTube native skip click.
- Edge Gesture Guard.
- Universal Blank Compatibility.
- Desktop/Mobile universal fix.
- Search Engine fix.
- Lordborg / Instant Monitor / Invest-Tracing compatibility.
- Night Mode.
- Translate.
- Download Manager.
