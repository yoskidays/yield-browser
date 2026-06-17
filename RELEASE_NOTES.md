# Yield Browser v0.9.71

## Edge-only Gesture Fix + YouTube Lewati Click Fix

- Memperbaiki gesture slide/back yang terlalu sensitif pada mode mobile biasa.
- Custom swipe Back/Forward sekarang hanya aktif jika sentuhan dimulai dari pinggir layar.
- Pada situs desktop-only/horizontal-scroll seperti `h-metrics.com`, area edge dibuat lebih sempit agar scroll horizontal tidak mudah terbaca sebagai Back.
- Tombol Back HP tetap normal.
- Memperkuat auto klik tombol YouTube `Lewati` / `Skip` pada mobile player.
- Klik YouTube sekarang mencoba parent clickable dan fallback klik koordinat tengah tombol.
- Mode cooldown YouTube tidak lagi tidur kalau iklan masih terlihat, sehingga tombol `Lewati` tetap bisa diklik.
- Tetap tidak memakai playbackRate 16x, mute paksa, force play, atau blokir resource inti YouTube.
- Fitur v0.9.70 tetap dipertahankan.
