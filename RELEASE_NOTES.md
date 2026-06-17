# Yield Browser v0.9.70

## YouTube Auto Cycle Ad Bypass

- Menambahkan siklus otomatis YouTube AdBlock:
  - iklan muncul → assist aktif;
  - klik Skip/Lewati jika tombol muncul;
  - jika Skip belum muncul, maju +10 detik hanya pada video iklan;
  - setelah iklan selesai → YouTube AdBlock tidur;
  - setelah video utama berjalan ±2 menit → monitor aktif lagi untuk iklan tengah/belakang.
- Tidak memakai speed playbackRate, mute paksa, force play, currentTime saat video utama normal, atau style override player utama.
- Tetap tidak memblokir resource YouTube core seperti googlevideo, ytimg, dan youtubei.
- Fix v0.9.69 tetap dipertahankan: universal desktop-site gesture guard, desktop/mobile fix, search engine fix, universal blank compatibility, Lordborg, Instant Monitor, dan Invest-tracing.
