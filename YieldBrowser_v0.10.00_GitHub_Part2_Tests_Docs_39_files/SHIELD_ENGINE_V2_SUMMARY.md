# Yield Shield Engine V2 — v0.9.99

## Tujuan

Shield Engine V2 melindungi tab utama sebelum popup, click hijack, same-origin ad relay, atau redirect iklan mengambil alih halaman. Implementasi dibuat universal untuk reader komik dan situs lain dengan pola iklan serupa, tanpa statistik mengambang, badge hitungan, atau toast rutin.

## Lapisan perlindungan

1. **Document-start guard**
   - Dipasang melalui AndroidX WebKit bila `DOCUMENT_START_SCRIPT` tersedia.
   - Berjalan sejak awal dokumen, sebelum sebagian besar script halaman.
   - Memiliki fallback injeksi pada page commit/page finished untuk WebView lama.

2. **Navigation firewall**
   - Memeriksa main-frame navigation sebelum tab berpindah.
   - Memblokir domain iklan berkeyakinan tinggi, external app scheme berisiko, dan same-origin relay seperti `/r/<token>`, `/go/`, `/out/`, `/redirect/`, atau `/click/` ketika konteksnya reader/compatibility.
   - Chapter, manga, article, search, media, dan asset konten tetap diizinkan.

3. **Network filtering selektif**
   - Memblokir script/iframe/tracker pihak ketiga dengan sinyal iklan kuat.
   - Tidak memblokir CDN gambar reader, font, media, YouTube core, atau download intent yang dipercaya.

4. **Click-hijack dan popup guard**
   - Menjaga `window.open`, klik anchor, programmatic anchor click, dan form submit.
   - Gesture pengguna tidak otomatis dianggap izin bila targetnya relay/iklan.

5. **Cosmetic overlay cleanup**
   - Menyembunyikan overlay fullscreen transparan hanya bila memenuhi beberapa sinyal: cakupan layar, z-index, posisi, clickability, dan pola iklan/relay.
   - Tidak menghapus dialog normal hanya berdasarkan nama `overlay`.

6. **Safe URL preservation**
   - URL relay tidak boleh menggantikan `lastSafeUrl` tab.
   - Jika redirect lolos sampai `onPageStarted`, Yield memulihkan URL reader aman, bukan `about:blank` atau relay iklan.

## UI bersih

- Tidak ada penghitung iklan/tracker.
- Tidak ada panel statistik Shield.
- Tidak ada toast ketika iklan, popup, atau redirect diblokir.
- Menu hanya menampilkan status sederhana `Shield ON/OFF` dan opsi proteksi.
- Toast tetap dibatasi untuk error yang benar-benar membutuhkan tindakan pengguna.

## Catatan kompatibilitas

Shield Engine V2 memakai kebijakan high-confidence untuk mengurangi situs rusak. Ini meningkatkan perlindungan pada Android WebView, tetapi tidak mengubah Yield menjadi fork Chromium native seperti Brave. Situs yang sangat khusus tetap mungkin memerlukan penyesuaian setelah runtime test.
