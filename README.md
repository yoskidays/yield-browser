# Yield Browser v0.4.7 Pause Resume Fix + Home Bookmark Clear

Update ini memperbaiki dua masalah utama.

## Perubahan v0.4.7

### Pause / Resume download
- Tombol **Ⅱ** tetap untuk jeda.
- Tombol **▶** sekarang mencoba melanjutkan dari file partial yang sudah ada.
- Tombol **↻** tetap khusus untuk reload/download ulang dari awal.
- Untuk download **2 koneksi**, aplikasi menyimpan progres part 1 dan part 2.
- Untuk fallback **1 koneksi**, aplikasi mencoba resume memakai HTTP Range dari byte terakhir.
- Kalau server menolak resume, baru fallback mulai ulang sesuai respons server.

### Home dan bookmark
- Saat klik tombol **Home**, address bar dikosongkan.
- Star bookmark tidak lagi ikut menyala/nempel dari situs sebelumnya.
- Tab aktif dianggap kembali ke halaman awal.

## Catatan
Resume download bergantung pada dukungan server terhadap HTTP Range. Jika server/file host menolak resume, reload dari awal tetap tersedia lewat tombol **↻**.


## v0.5.0
- Kontrol video tidak muncul otomatis saat halaman hanya memiliki video.
- Kontrol video baru muncul saat video benar-benar diputar.
- Saat video pause/selesai, kontrol otomatis sembunyi.
- Tombol X pada kontrol video hanya menyembunyikan kontrol sesi itu, bukan mematikan fitur permanen.
- Panel Bookmark/Histori tetap memakai status bar gelap penuh.


## v0.5.1
- Mode Malam dibuat lengkap:
  - OFF
  - ON
  - Auto ikut sistem
  - Pengecualian per situs
- Mode malam sekarang juga menginjeksi CSS ke halaman web agar background gelap dan teks tetap terang/terbaca.
- Gambar, video, canvas, dan SVG tidak ikut dibuat aneh/terbalik.
- Pengaturan pengecualian situs memakai host/domain halaman aktif.


## v0.5.2
- Fix compile error: duplicate inner class `VideoBridge`.
- `VideoBridge` sekarang hanya ada 1 kali di `MainActivity.java`.
- Pemanggilan `webView.addJavascriptInterface(...)` dibersihkan agar tidak dobel.
- Fitur v0.5.1 tetap dipertahankan:
  - Mode Malam OFF / ON / Auto ikut sistem
  - pengecualian per situs
  - kontrol video hanya muncul saat video benar-benar diputar


## v0.5.3
- Ikon mode privat diganti menjadi ikon topi + kacamata (incognito style).


## v0.5.4
- Fix compile error `class, interface, or enum expected` di `onCreate`.
- Penyebabnya adalah extra closing brace `}` sebelum `VideoBridge`.
- Struktur `MainActivity` dibetulkan.
- Icon Privat topi+kacamata dari v0.5.3 tetap dipertahankan.


## v0.5.4 final fix
- Fix compile error `class, interface, or enum expected` karena extra closing brace `}`.
- Struktur `MainActivity` sudah kembali valid.
- Icon privat topi+kacamata tetap dipertahankan.


## v0.5.5
- Fix compile error `';' expected` pada `configureWebView()` dan `isSystemDarkMode()`.
- Penyebabnya: method `loadTranslatedPage()` belum ditutup dengan `}`.
- Struktur `MainActivity.java` dicek ulang: `VideoBridge` tetap 1 dan JavaScript interface tetap 1.


## v0.5.6
- Mode Malam web dibuat lebih aman agar teks tidak nyatu/ketumpuk di atas gambar.
- CSS mode malam tidak lagi memaksa semua `div`, `article`, dan card menjadi gelap/transparan.
- Bookmark dan Histori dibuat true fullscreen agar area atas tidak putih/abu-abu.
- Status bar panel Bookmark/Histori disembunyikan/digelapkan penuh.


## v0.5.7
- Histori dan Bookmark tidak lagi memakai fullscreen hide-status-bar.
- Status bar tetap terlihat, tetapi background dipaksa hitam.
- Dialog panel dibersihkan dari dim/gray overlay.
- Mode Malam web dibuat lebih aman untuk Google Search/AI:
  - tidak lagi memaksa semua `div`, `span`, `p`, dan card menjadi warna tertentu
  - hanya memakai `color-scheme: dark` + background dasar
  - gambar/video/canvas tetap normal


## v0.5.8
- Translate dibuat memakai menu pilihan:
  - Terjemahkan halaman ke Indonesia
  - Sembunyikan/Tampilkan bar Google Translate
  - Terjemahkan teks halaman saja
  - Reload website
  - Matikan translate/buka halaman asli
- Bar Google Translate bisa di-hide supaya tampilan web lebih bersih.
- Translate teks halaman ditambahkan sebagai fallback untuk situs yang menolak Google Translate proxy.
- Fitur Reload website ditambahkan ke quick menu.


## v0.5.9
- Jika download/file/riwayat download dihapus, notifikasi download ikut dibatalkan.
- Thread download yang itemnya dihapus diberi status `removed` dan `pauseRequested=true`.
- Hide bar Google Translate diperkuat:
  - overlay/iframe Google Translate tidak lagi menangkap klik
  - `pointer-events` untuk elemen website dikembalikan aktif
  - hide toolbar dijalankan beberapa kali karena Google sering inject toolbar terlambat
- Menu website tetap bisa diklik ketika translate aktif.


## v0.6.0
- Tombol Home sekarang hanya menyembunyikan halaman web, bukan menghapus state halaman/tab.
- Kalau tidak sengaja kepencet Home, halaman terakhir bisa dibuka lagi dengan gesture.
- Swipe kiri: kembali ke aktivitas/halaman sebelumnya.
- Swipe kanan: maju ke aktivitas/halaman berikutnya jika tersedia.
- Saat berada di Home, gesture bisa mengembalikan halaman web terakhir yang masih tersimpan di WebView/tab.


## v0.6.1
- Fix compile error: invalid method reference `this::hideGoogleTranslateToolbar`.
- Method reference diganti ke lambda `() -> hideGoogleTranslateToolbar()`.
- Jika method hide toolbar tidak ada, ditambahkan ulang implementasi aman.
- Fitur v0.6.0 gesture maju-mundur tetap dipertahankan.


## v0.6.2
- Fix translate aktif tetapi menu/link website tidak bisa diklik.
- Hide Google Translate bar diperkuat:
  - overlay Google Translate dibuat `pointer-events:none`
  - elemen website dipaksa `pointer-events:auto`
  - MutationObserver dipakai untuk membersihkan layer translate yang muncul terlambat
- Menu Translate ditambah opsi manual: `Aktifkan klik menu website`.


## v0.6.3
- Fix Mode Malam OFF masih menyisakan background gelap.
- Saat OFF:
  - CSS `yield-night-style` dihapus
  - `color-scheme` dikembalikan ke light
  - WebView background dikembalikan putih
  - `ForceDark` dimatikan
  - halaman aktif direload ringan agar style situs kembali normal


## v0.6.4
- Item lama `Night mode` di Setelan diganti menjadi `Mode Malam: OFF/ON/AUTO`.
- Mengubah Mode Malam dari Setelan tidak menutup panel Setelan lagi.
- Mode Malam OFF tidak langsung reload/pindah Home; hanya membersihkan CSS gelap di halaman aktif.
- Ditambahkan opsi `Bersihkan style gelap halaman ini` untuk membersihkan sisa style gelap tanpa keluar dari menu.


## v0.6.5
- Translate hide bar dibuat soft supaya tidak merusak layout web hasil translate.
- Opsi `Aktifkan klik menu website` tetap ada untuk situs yang kliknya masih tertahan.
- Toggle ON/OFF di Setelan tidak lagi menutup dan membuka ulang panel.
- Status ON/OFF di Setelan berubah langsung di baris yang sama.
- Dialog Mode Malam dibuat dark, bukan putih.
- Warna hitam UI diganti menjadi dark gray seperti style Brave, tidak hitam pekat.


## v0.6.6
- Menghapus tulisan info di Home:
  `Download langsung berjalan saat tombol unduh ditekan. Detail ada di Menu > Unduhan Yield.`
- Tampilan Home dibuat lebih bersih.


## v0.6.7
- Translate dibuat lebih kompatibel untuk website yang menolak Google Translate proxy.
- Opsi utama `Terjemahkan halaman ke Indonesia` sekarang memakai Mode Translate Kompatibel:
  - tetap membuka website asli
  - menerjemahkan teks langsung di halaman
  - tidak memakai domain `.translate.goog`, sehingga lebih kecil kemungkinan kena anti-bot.
- Opsi `Google Translate proxy (lama)` tetap tersedia.
- Jika halaman proxy menampilkan captcha/`Aku bukan robot`, browser mencoba kembali ke website asli dan menjalankan Mode Translate Kompatibel.


## v0.6.8
- Menambahkan ikon Reload website di address bar, posisinya sebelum Bookmark.
- Tiga ikon atas bisa diatur ON/OFF:
  - Reload
  - Bookmark
  - Translate
- Pengaturan ikon atas ada di `Sesuaikan menu` bagian `Icon atas`.
- Toggle di panel `Sesuaikan menu` tidak lagi menutup dan membuka ulang panel.


## v0.6.9
- Fix compile error `fixedMenuRow(int,String)` belum ada.
- Menambahkan import `BufferedReader` dan `InputStreamReader` untuk Mode Translate Kompatibel.
- Mengganti method reference `this::translatePageCompatible` menjadi lambda `() -> translatePageCompatible()`.
- Memastikan JavaScript interface `VideoBridge` dan `TranslateBridge` berada di `configureWebView()`.


## v0.7.0
- Icon Translate langsung berubah warna saat translate aktif, tanpa perlu reload.
- Target bahasa translate tidak dikunci Indonesia lagi.
- Menu Translate ditambah `Pilih bahasa target`.
- Bahasa target disimpan di pengaturan.
- Mode Translate Kompatibel memakai bahasa target internal Yield, bukan bar Google Translate.
- Translate Kompatibel diperluas:
  - node teks maksimum dinaikkan
  - teks yang belum diterjemahkan bisa dilanjutkan dengan opsi `Lanjutkan translate bagian belum diterjemahkan`
  - halaman dinamis dicoba translate ulang otomatis.


## v0.7.1
- Kontrol video ditambah tombol kualitas.
- Pilihan kualitas video:
  - Auto
  - 240p
  - 360p
  - 480p
  - 720p
- Browser mencoba mengganti kualitas melalui:
  - `<source>` HTML5 video
  - JWPlayer quality levels
  - Video.js quality levels
  - tombol kualitas di player web jika ditemukan
- Jika player tidak menyediakan pilihan kualitas yang bisa diakses WebView, akan muncul notifikasi bahwa kualitas tidak tersedia.


## v0.7.2
- Menambahkan menu `Tentang Yield` di bagian paling bawah Setelan.
- Isi `Tentang Yield`:
  - Yield Browser
  - Versi aplikasi
  - Develop by Yield Yoski Days
- Menambahkan ikon info untuk menu Tentang Yield.


## v0.7.3
- Menghapus tulisan `Video` di samping tombol `-10s` agar bar kontrol lebih lega.
- Tombol X kontrol video sekarang hanya hide sementara:
  - bisa muncul lagi saat video diketuk
  - otomatis mencoba muncul lagi setelah beberapa detik jika video masih berjalan
- Menambahkan tombol `Full` di kontrol video.
- Menambahkan dukungan fullscreen video:
  - request fullscreen HTML5
  - klik tombol fullscreen player jika ditemukan
  - fallback fullscreen aplikasi dengan orientasi landscape
  - tombol Back keluar dari fullscreen.


## v0.7.4
- Kontrol video dipusatkan.
- Tombol `-10s`, `Play`, `Pause`, dan `+10s` dibuat fixed width agar posisinya berada di center.
- Tombol `Video` tetap dihapus agar bar kontrol lebih lega.
- Fitur v0.7.3 tetap ada:
  - kontrol video bisa muncul lagi setelah X
  - tombol Full
  - fullscreen video fallback.
