# Yield Browser v0.9.72

## Video Controls + YouTube Skip Fix

- Menghapus toast seek video (+10s / -10s) yang menutup tombol kontrol saat portrait.
- Menggabungkan tombol Play dan Pause menjadi satu tombol toggle.
  - Saat video play, tombol berubah menjadi Pause.
  - Saat video pause, tombol berubah menjadi Play.
- Tombol Fullscreen dibuat lebih responsif dengan fallback otomatis jika request fullscreen WebView tidak langsung aktif.
- Saat fullscreen/landscape, jarak dan ukuran tombol kontrol video dibuat sedikit lebih lega.
- Saat kembali portrait, layout kontrol kembali compact seperti semula.
- YouTube Skip/Lewati diperkuat dengan fallback native tap dari Android WebView supaya tombol iklan lebih mungkin terpencet seperti tap user.
- Tetap tidak memakai playbackRate 16x, mute paksa, force play video utama, atau blokir resource YouTube inti.

## Dipertahankan

- YouTube Auto Cycle Ad Bypass v0.9.70/v0.9.71.
- Edge Gesture Guard.
- Universal Blank Compatibility.
- Desktop/Mobile universal fix.
- Search Engine fix.
- Lordborg, Instant Monitor, Invest-tracing compatibility.
- Direct Image Guard.
- Night Mode.
- Translate.
- Download Manager.
