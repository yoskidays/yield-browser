# Yield Browser v0.9.70

Source revisi Yield Browser v0.9.70.

Fokus update: **YouTube Auto Cycle Ad Bypass**.

Alur YouTube:
- saat iklan terdeteksi, Yield mencoba klik tombol Skip/Lewati;
- jika tombol belum muncul, Yield membantu maju +10 detik hanya saat sinyal iklan kuat;
- setelah iklan selesai, engine YouTube AdBlock masuk mode tidur;
- setelah video utama berjalan sekitar 2 menit, engine aktif lagi untuk iklan tengah/belakang;
- siklus ini berulang setiap kali iklan muncul.

Tidak memakai playbackRate, mute paksa, force play, atau manipulasi style video utama.
