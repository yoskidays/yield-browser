# Blokir Elemen V2 — mode berkelanjutan dan filter permanen

Yield Browser v0.10.05 menyempurnakan fitur **Blokir elemen** menjadi cosmetic filter manual yang berdiri sendiri, tidak bergantung pada status AdBlock utama.

## Perilaku baru

1. Buka halaman web lalu pilih **⋮ → Blokir elemen**.
2. Bar mode pemilih tetap tampil setelah satu elemen berhasil diblokir.
3. Pengguna dapat langsung mengetuk elemen berikutnya tanpa membuka menu ulang.
4. Tombol **X** di sisi kanan bar menutup mode pemilih.
5. Dialog konfirmasi menyediakan:
   - **Blokir & lanjut** — menyimpan filter dan tetap berada dalam mode pemilih.
   - **Naik 1 induk** — memperluas pilihan ke container induk yang masih aman.
   - **Batal pilihan** — membatalkan pilihan saat ini tetapi tidak menutup mode.
6. Dialog menampilkan jumlah elemen yang akan terkena selector agar pengguna tidak memblokir area terlalu luas tanpa sadar.

## Persistensi dan AdBlock OFF

- Filter disimpan permanen di `SharedPreferences` berdasarkan host yang dinormalisasi.
- Awalan `www.` diabaikan, sehingga `www.example.com` dan `example.com` memakai kumpulan filter yang sama.
- Filter tetap diterapkan saat AdBlock utama **OFF**.
- Filter tetap diterapkan pada compatibility mode dan halaman reader.
- Membuka situs dari bookmark, riwayat, tab yang dipulihkan, atau mengetik URL akan memasang kembali filter host yang sama.
- Filter hanya berhenti setelah dihapus melalui **Filter situs ini**.

## Ketahanan halaman dinamis

Setiap host memakai satu stylesheet bernama `yield-user-filters`. Stylesheet diterapkan pada `onPageCommitVisible`, `onPageFinished`, saat kembali ke tab hidup, dan setelah pengaturan AdBlock berubah. Mutation observer ringan memasang ulang stylesheet jika situs mengganti `<head>` atau menghapus style pada navigasi SPA.

CSS dibuat sebagai aturan terpisah per selector. Satu selector lama yang tidak valid tidak membatalkan seluruh kumpulan filter.

## Keamanan pemilih

- Selector diverifikasi kembali di sisi Java sebelum disimpan.
- Selector list, at-rule, kurung CSS, titik koma, karakter kontrol, dan payload injeksi CSS ditolak.
- Elemen penting tidak dapat diblokir: `html`, `body`, `head`, `script`, `style`, `link`, `meta`, `title`, `video`, `audio`, dan `source`.
- Tombol **Naik 1 induk** berhenti sebelum mencapai elemen penting.
- Event sentuhan memakai `PointerEvent` bila tersedia, dengan fallback touch/mouse dan debounce untuk mencegah satu tap memunculkan dialog berulang.
- Mode dibersihkan saat pindah tab, navigasi baru, kembali ke Home, WebView dihancurkan, atau Activity masuk background.

## Selector yang lebih stabil

Urutan pembentukan selector:

1. ID unik yang terlihat stabil.
2. Tag dengan maksimal tiga class stabil jika sudah unik.
3. Jalur induk hingga tujuh tingkat.
4. `:nth-of-type` hanya ditambahkan ketika sibling dengan pola sama memang lebih dari satu.

ID/class yang sangat panjang, berisi digit acak panjang, atau pola CSS-in-JS umum dihindari.

## Berkas utama

- `MainActivity.java` — lifecycle, penyimpanan, penerapan filter, bridge, dan dialog.
- `ElementPickerScript.java` — UI picker berkelanjutan, tombol X, highlight, selector, dan event handling.
- `UserElementFilterPolicy.java` — validasi selector dan proteksi elemen penting.
- `ElementPickerScriptTest.java`
- `UserElementFilterPolicyTest.java`
