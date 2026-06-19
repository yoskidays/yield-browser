# Yield Browser v0.9.83

Source revisi Yield Browser v0.9.83.

Fokus update: build fix untuk Persistent Tabs Session v0.9.82.

Perbaikan:
- Memperbaiki error compile Java pada writer session tab.
- Literal newline/tab pada `saveTabsSession()` dibuat aman untuk Java source 8.
- Persistent tabs tetap dipertahankan: tab terbuka tersimpan saat aplikasi ditutup dan dipulihkan saat dibuka lagi.
- Baseline stabil v0.9.81 tetap dipertahankan.


## v0.9.84 YouTube auto assistant only

Revisi kecil khusus UI AdBlock dan YouTube:
- Menghapus aksi manual `AdBlock OFF 2 menit` dari UI.
- Tidak menambah toggle baru.
- YouTube auto assistant tetap otomatis: deteksi tombol Skip/Lewati, klik otomatis, lalu resume video utama satu kali bila kepause setelah iklan dilewati.
- Bagian stabil seperti tab session, restore tab, adTab guard, lastSafeUrl, dan ad guard session tidak diubah.

## v0.9.84 YouTube icon skip refinement

Revisi kecil khusus YouTube Assistant:
- Menambah deteksi tombol skip model baru/icon-only seperti tombol anak panah di pojok kanan player.
- Tetap menghindari tombol iklan seperti `Kunjungi Situs`, `Kunjungi Pengiklan`, sponsor, menu, settings, dan kontrol player lain.
- Tidak mengubah tab session, restore tab, adTab guard, lastSafeUrl, atau sistem stable lain.

## v0.9.84 Smart Tab Isolation Guard

Build ini menambahkan pengaman multi-tab berbasis domain per tab. Tujuannya agar ketika tab YouTube/video/website lain ditutup, URL dari tab tersebut tidak bisa tersimpan ke tab lain saat aplikasi keluar-masuk, switch tab, restore session, atau recover dari direct-link iklan.


## v0.9.84 Universal Download AdBlock Allowlist

Revisi ini menjaga tombol download asli tetap bisa diklik saat AdBlock aktif, termasuk halaman Google Drive/Drive UserContent seperti tombol `Tetap download`.

Fokus revisi:
- Membuat allowlist universal untuk URL download/file.
- Menghindari false-positive AdBlock pada link download cloud storage.
- Tidak mengubah sistem tab yang sudah stabil.

### Smart Turbo Download Engine v2

Yield Browser sekarang memakai engine download adaptif:

- Safe 1 koneksi untuk file kecil/server tanpa Range.
- Stable 2 koneksi untuk host sensitif seperti Google Drive, Googleusercontent, OneDrive, Mega.
- Turbo 4 koneksi untuk file besar/video/CDN yang cocok.
- Hard pause menutup koneksi aktif agar pause tidak terasa macet.
- Resume menyimpan state part download agar progress tidak mudah reset.

Perubahan ini hanya menyentuh area download manager.
