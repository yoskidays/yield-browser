# Universal Reader Compatibility Repair — v0.9.93

## Tujuan
Mengganti perbaikan reader yang sebelumnya khusus `komiku.org` menjadi pemulihan universal berbasis struktur DOM dan perilaku lazy-load. Mesin ini dapat bekerja pada situs manga, manhwa, manhua, webtoon, komik, dan image-reader lain tanpa menambahkan daftar domain satu per satu.

## Deteksi aman
Perbaikan hanya berjalan pada halaman HTTP/HTTPS, bukan URL gambar langsung, video, PDF, arsip, `blob:`, `data:`, atau skema internal. Java melakukan penyaringan URL awal, kemudian JavaScript mengklasifikasikan halaman dari:

- pola URL chapter, episode, reader, reading, atau baca;
- jumlah gambar lazy-load dalam kontainer yang sama;
- susunan gambar lebar/vertikal yang menyerupai reader;
- nama kelas atau ID seperti chapter, manga, webtoon, reader, pages, dan content;
- atribut lazy-load yang umum digunakan.

Halaman daftar komik dengan thumbnail kecil tidak dipaksa menjadi reader karena tidak memenuhi skor kontainer vertikal.

## Format lazy-load yang dipulihkan

- `data-src`
- `data-lazy-src`
- `data-original`
- `data-cfsrc`
- `data-url`
- `data-echo`
- `data-lazy`
- `data-image`
- `data-original-src`
- `data-src-large`
- `data-srcset`
- `data-lazy-srcset`
- `data-original-srcset`
- elemen `<picture><source>`
- background lazy melalui `data-bg`, `data-background`, `data-background-image`, dan `data-lazy-background`

## Perlindungan iklan
Elemen tidak dipulihkan jika URL, ID, class, role, aria-label, parent, atau link induknya mengandung pola iklan seperti ad, banner, sponsor, affiliate, popup, popunder, DoubleClick, Taboola, Outbrain, dan jaringan iklan umum. Dengan demikian pemulihan reader tidak menyalakan kembali iklan yang telah diblokir.

## Ketahanan runtime

- Pemulihan dijalankan bertahap setelah commit/finish.
- Mode kompatibel memperoleh retry lebih panjang sampai 6 detik.
- Halaman reader biasa memperoleh retry ringan.
- `MutationObserver` menangani gambar yang ditambahkan secara dinamis.
- Observer tidak memantau perubahan style buatan Yield agar tidak membentuk loop injeksi.
- Task tertunda terikat pada tab dan generasi WebView yang sama sehingga tidak diterapkan ke tab lain setelah pengguna berpindah.

## Batasan
Perbaikan tidak mengekstrak gambar dari DRM, canvas terenkripsi, WebGL, atau reader yang membangun halaman sepenuhnya dari binary stream tertutup. Untuk situs tersebut dibutuhkan adapter khusus dan harus mempertimbangkan hak akses konten.
