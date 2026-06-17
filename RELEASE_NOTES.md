# Yield Browser v0.9.67

## Build Fix + Universal Desktop Mode + YouTube Delayed Skip-Only

### Build Fix
- Memastikan regex JavaScript pada Universal Blank Compatibility aman untuk Java source 8.

### Universal Desktop/Mobile Mode
- Desktop/Mobile toggle sekarang memperlakukan halaman aktif sebagai navigasi yang dipercaya.
- Situs seperti `invest-tracing.com` tidak lagi gagal reload hanya karena host-nya masuk daftar popup/ad.
- Hard reload tetap memakai profile mode yang benar.
- Saat kembali ke Mobile Mode, viewport desktop lama dibersihkan agar layout mobile tidak sempit/terpotong.
- YouTube mobile diarahkan kembali ke `m.youtube.com` saat Desktop Mode OFF dan ke `www.youtube.com` saat Desktop Mode ON.

### YouTube Delayed Skip-Only
- Awal video: hanya klik tombol Skip/Lewati jika muncul.
- Setelah video utama berjalan sekitar 5 detik, script masuk cooldown/tidur agar player tidak hitam.
- Setelah video berjalan ±2 menit, monitor Skip-Only aktif lagi untuk iklan tengah video.
- Tidak ada speed, mute, force play, currentTime jump, network block, atau style manipulation pada video utama.

### Tidak Diubah
- AdBlock situs umum yang sudah stabil.
- Lordborg/Instant Monitor compatibility.
- Direct Image Guard.
- Night Mode.
- Kontrol fullscreen video.
- Translate dan Download Manager.
