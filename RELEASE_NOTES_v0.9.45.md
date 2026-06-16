# Yield Browser v0.9.45

## Direct Image / Komik Page Guard

- Mengembalikan perilaku lama untuk direct image/link mentah seperti `.jpg`, `.jpeg`, `.png`, `.webp`.
- Direct image main-frame sekarang dicegat sebelum normal user navigation dan sebelum compatibility mode.
- Jika situs komik/website mengarahkan tab utama ke file gambar mentah, Yield membuka direct link itu sebagai tab sementara lalu menutup otomatis, sehingga halaman komik utama tidak berubah menjadi web JPEG.
- Direct image guard tetap bekerja walaupun AdBlock dimatikan.
- Desktop/Mobile Mode tidak diubah.
- Lordborg/site compatibility guard tidak dibongkar, hanya dibuat tidak mengorbankan direct image guard.
