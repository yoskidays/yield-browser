# Release Notes - YieldBrowser v0.9.84 Safe Brave-Class Layer v3

## Added
- Safe Brave-Class Download Layer v3.
- Balanced 3 connection mode.
- Bandwidth prediction v3 dengan rolling speed, jitter, healthy/slow sample, dan retry pressure.
- Adaptive fallback: Turbo 4 dapat turun ke Balanced 3 atau Stable 2 melalui worker limit.
- Active connection display agar UI tidak selalu menampilkan total chunk sebagai koneksi aktif.

## Improved
- Video >50 MB tetap mencoba Turbo 4 lebih dulu.
- Host sensitif tetap dijaga di Stable 2.
- Unknown file besar bisa memakai Balanced 3 sebelum Turbo 4.
- Resume dynamic 3/4 part lebih aman karena worker bisa diturunkan tanpa membuang part state.
- Hard pause tetap memutus stream dan HTTP connection aktif.

## Unchanged
- Smart Tab Isolation Guard.
- Persistent tab session.
- YouTube auto assistant, icon skip, dan forward +10s.
- AdBlock logic dan universal download allowlist.
