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


## v0.7.5
- Fix fullscreen video yang balik ke Home saat berubah landscape.
- MainActivity diberi `configChanges` agar tidak recreate saat orientasi berubah.
- Menambahkan `onConfigurationChanged` untuk menjaga state WebView.
- Menghapus bagian `Tetap tampil` di panel `Sesuaikan menu`.
- Menghapus row `Engine download` di Pengaturan Unduhan karena belum ada fungsi pengaturan aktif.


## v0.7.6
- Revisi tampilan Tentang Yield menjadi gaya halaman profesional seperti referensi.
- Menambah kartu Versi aplikasi, Sistem operasi, dan Developer.
- Teks developer diganti menjadi: develop by yoski days.


## v0.7.7
- Revisi judul home "Yield Browser" agar lebih profesional dan elegan.
- Warna nama dibedakan: "Yield" terang netral, "Browser" memakai warna identitas brand.


## v0.7.8
- Semua AlertDialog default dibuat dark melalui theme.
- Dialog `Folder unduhan` dibuat custom dark, tidak putih lagi.
- Default hasil download sekarang disalin ke folder publik `Download/Yield Browser`.
- Folder app tetap dipakai sebagai staging agar download 2 koneksi tetap stabil di Android 11.
- Menambahkan opsi `Pilih folder HP`; jika dipilih, file hasil download akan disalin ke folder HP tersebut setelah selesai.
- Menambahkan opsi reset ke default `Download/Yield Browser`.
- Riwayat download menyimpan URI hasil ekspor agar file bisa dibuka/dibagikan dari lokasi publik/SAF.
- Membersihkan sisa menu `Tetap tampil` dan `Engine download`.


## v0.7.9
- Fix compile error: `onConfigurationChanged(Configuration)` dibuat `public`.
- Menambahkan drawable `ic_close.xml` yang dipakai di halaman Tentang Yield.
- Fitur v0.7.8 tetap dipertahankan:
  - dialog folder unduhan dark
  - default hasil download ke Download/Yield Browser
  - opsi pilih folder HP


## v0.8.0
- Menambahkan fitur UC-style download yang belum ada/kurang maksimal.
- Auto detect video file: video dan HLS/m3u8 otomatis diberi kategori Video dan nama file dirapikan.
- Dynamic 2/4 koneksi: file besar otomatis mencoba 4 koneksi jika server support Range; file normal tetap 2 koneksi stabil.
- Smart resume dipertahankan dan diperkuat dengan retry otomatis dari progres terakhir.
- Retry otomatis sampai 3 kali jika koneksi terputus.
- Download HLS/m3u8: playlist dideteksi dan segmen digabung ke file TS.
- Speed limiter: OFF, 256 KB/s, 512 KB/s, 1024 KB/s, 2048 KB/s.
- Real-time speed graph: item download berjalan menampilkan MB/s + mini grafik speed.
- Auto rename file diperkuat untuk video/HLS dan nama duplikat.


## v0.8.1
- Menghapus mini grafik speed di bawah progress bar orange agar item download lebih rapi.
- Speed tetap tampil sebagai teks, tetapi graph blok tidak ditampilkan.
- Fullscreen video diperbaiki:
  - kontrol video tidak hilang permanen saat fullscreen.
  - kontrol video dipindahkan sebagai overlay di atas custom fullscreen player.
  - fallback fullscreen tetap memunculkan kontrol video.
- AdBlock dibuat lebih aman untuk video:
  - URL media seperti mp4, m3u8, mpd, webm, ts, m4s, dan googlevideo/videoplayback tidak diblok.
  - mengurangi risiko video ikut keblok saat AdBlock aktif.


## v0.8.2
- Dialog "Fitur UC Download" dirapikan.
- Deskripsi tambahan pada item Smart resume, Auto detect video file, Auto rename file, dan Real-time speed graph dihapus.
- Sekarang bagian itu hanya menampilkan nama fungsi agar tampilan lebih bersih dan rapi.


## v0.8.3
- Merapikan dialog `Fitur UC Download`.
- Dialog dibuat scrollable agar tidak penuh di layar kecil.
- Card toggle dibuat lebih compact dan premium.
- Item fungsi aktif dibuat lebih bersih dengan ikon centang.
- Jarak antar item dan tinggi card diperbaiki.
- Tombol ON/OFF dibuat lebih rapi.


## v0.8.4
- Fix compile error pada dynamic 2/4 koneksi.
- Variabel `total`, `connections`, dan output file dibuat final copy sebelum dipakai di lambda thread.
- Fitur v0.8.3 tetap dipertahankan.


## v0.8.5
- Merapikan dialog Folder unduhan:
  - tombol reset dibuat 2 baris agar teks `DOWNLOAD/YIELD BROWSER` tidak kepotong.
  - button dark dialog dibuat lebih fleksibel untuk teks panjang.
- AdBlock YouTube diperbaiki:
  - endpoint internal YouTube/GoogleVideo tidak diblokir agar video awal tidak blank.
  - cosmetic adblock YouTube dibuat lebih aman dan tidak menyentuh class `.ad-showing`.
  - skip/hide iklan YouTube tetap dicoba lewat JavaScript ringan.
- Cleanup source:
  - menghapus method `speedGraph()` dan field `speedHistory` yang sudah tidak dipakai di UI.
  - label `Real-time speed graph` diganti menjadi `Real-time speed`.


## v0.8.6
- AdBlock diperkuat untuk popup redirect/click hijack.
- Memblokir navigasi main-frame ke domain iklan acak seperti `hotterydiseur.shop` dan `sewarsremeets.cfd`.
- Menambahkan blokir `window.open`, link `target=_blank`, iframe/script popup, dan domain popup ad network.
- YouTube/GoogleVideo tetap masuk allowlist agar video tidak ikut terblokir.
- WebView disetel tidak mendukung popup window otomatis saat AdBlock aktif.


## v0.8.7
- Menambahkan menu detail `AdBlock Premium`.
- Setiap proteksi AdBlock punya ON/OFF sendiri:
  - AdBlock aktif
  - Blokir popup iklan
  - Blokir redirect iklan
  - Blokir script/iframe iklan
  - Proteksi click hijack
- Tidak menambahkan toggle video karena video playback selalu di-allow otomatis agar tidak ikut terblokir.
- WebView popup setting mengikuti toggle `Blokir popup iklan`.
- Blokir navigasi redirect mengikuti toggle `Blokir redirect iklan`.
- Blokir resource script/iframe mengikuti toggle `Blokir script/iframe iklan`.
- JavaScript click hijack mengikuti toggle `Proteksi click hijack`.


## v0.8.8
- Menambahkan Ad Redirect Isolation.
- Jika iklan memaksa redirect/popup, URL iklan dialihkan menjadi tab sementara agar tab utama tetap aman.
- Menambahkan auto close tab iklan agar tab tidak menumpuk.
- Menambahkan toggle AdBlock:
  - Alihkan iklan ke tab sementara
  - Auto close tab iklan
- JavaScript AdBlock sekarang mengirim URL popup/click hijack ke bridge `YieldAdBlockBridge`.
- Tab panel menandai tab iklan dengan badge `Ad`.
- Video playback tetap selalu di-allow dan tidak ikut diblokir.


## v0.8.9
- Memperkuat Desktop mode agar benar-benar terasa seperti tampilan PC/laptop.
- Desktop mode sekarang memakai User-Agent Windows Chrome, bukan Linux mobile-like.
- Menambahkan desktop viewport paksa `width=1280` lewat meta viewport injection.
- Menambahkan initial scale desktop agar halaman lebih lebar dan terlihat seperti desktop.
- Menambahkan min-width 1280px pada document/body agar situs responsive tidak tetap mode HP.
- Desktop mode tetap reload halaman saat ON/OFF agar perubahan langsung diterapkan.


## v0.9.0
- Menambahkan menu `Optimasi video online`.
- Menambahkan Video preload / buffer booster ringan:
  - preload video dibuat auto.
  - cache WebView lebih agresif saat booster aktif.
- Menambahkan HLS segment prefetch ringan:
  - m3u8/resource HLS dideteksi di halaman.
  - beberapa segmen awal download HLS di-prefetch ringan.
- Auto detect kualitas video diperkuat:
  - watcher mencoba membaca kualitas dari source, JWPlayer, dan Video.js.
  - tombol kualitas tetap memakai 240p, 360p, 480p, 720p.
- Floating player ringan:
  - mencoba Picture-in-Picture Android saat app ditinggalkan jika tersedia.
- Background play ringan:
  - media playback tidak dipaksa gesture jika fitur aktif.
  - video tidak dipaksa pause oleh Yield saat app masuk background.
- Download kualitas 240p–720p dibuat lebih aman lewat helper/detector, tanpa downloader agresif yang rawan crash.
- Rapikan kode video watcher dan helper supaya fitur video lebih terpusat.


## v0.9.1
- Kontrol video Yield saat fullscreen dipindah menjadi floating pill di atas-tengah.
- Perubahan ini mencegah kontrol Yield menimpa timeline/durasi/progress bar player web.
- AdBlock YouTube diperkuat tanpa memblokir video playback:
  - auto click tombol skip YouTube ads.
  - iklan pre-roll dicoba mute + speed up + lompat ke akhir jika terdeteksi.
  - overlay iklan YouTube disembunyikan tanpa memblokir `googlevideo/videoplayback`.
  - injeksi AdBlock diulang beberapa kali setelah page load agar iklan awal yang telat muncul ikut tertangani.


## v0.9.2
- AdBlock video ad bypass diperluas, bukan hanya YouTube.
- Menambahkan deteksi iklan video umum untuk player:
  - JWPlayer ad classes
  - Video.js / IMA ads
  - Plyr ads
  - VAST/VPAID/preroll/midroll overlay
  - tombol skip umum (`skip`, `lewati`, `skip-ad`, dll.)
- Saat iklan video terdeteksi:
  - coba klik skip
  - mute sementara
  - percepat playback iklan
  - lompat ke akhir jika durasi iklan pendek
  - hide overlay iklan
- Video utama tetap di-allow; `mp4/m3u8/mpd/webm/ts/m4s/googlevideo/videoplayback` tetap tidak diblok.
- Injeksi AdBlock diulang beberapa kali setelah page load agar iklan yang telat muncul ikut tertangani.


## v0.9.3
- Kontrol video saat fullscreen dikembalikan ke posisi bawah-tengah.
- Kontrol dibuat floating pill dengan margin bawah agar tetap nyaman dan tidak terlalu menutupi timeline/durasi bawaan player.
- Fitur AdBlock video bypass v0.9.2 tetap dipertahankan.


## v0.9.4
- Posisi kontrol video saat fullscreen diturunkan lagi agar lebih pas dan natural.
- Kontrol tetap berada di bawah-tengah.
- Ukuran tombol/menu kontrol video tidak diperkecil, jadi tetap mudah diklik.
- Fitur AdBlock video bypass tetap dipertahankan.


## v0.9.5
- AdBlock Premium sekarang default ON.
- Fresh install langsung memakai AdBlock aktif.
- Untuk update dari versi lama, AdBlock dipaksa ON satu kali pada v0.9.5.
- Setelah user mematikan AdBlock secara manual, pilihan user tetap dihormati dan tidak dinyalakan paksa lagi.


## v0.9.6
- Perbaikan AdBlock YouTube agar tidak membuat player hitam/stuck:
  - tidak memblokir resource video YouTube/GoogleVideo secara mentah.
  - deteksi mode iklan YouTube (`ad-showing` dan elemen iklan).
  - mute otomatis saat iklan.
  - speed iklan hingga 16x.
  - lompat ke akhir iklan jika durasi iklan pendek/terbaca.
  - klik tombol Skip Ad otomatis.
  - setelah iklan selesai, speed/mute video dikembalikan ke kondisi normal.
- AdBlock video umum tetap aman untuk JWPlayer/Video.js/Plyr/VAST/IMA tanpa memblokir file video utama.
- Menambahkan Download Queue Manager:
  - batas maksimal download aktif 1–4.
  - antrian otomatis.
  - pause/resume semua.
  - prioritas file.
  - naik/turun urutan antrian.
  - lanjut otomatis saat file sebelumnya selesai/gagal/dijeda.
- Tidak bentrok dengan kontrol video fullscreen bawah dan AdBlock Premium default ON.


## v0.9.7
- Perbaikan lanjutan AdBlock YouTube agar lebih mendekati Brave:
  - hide iklan sponsor YouTube mobile di bawah video (`Bersponsor`, `Buy now`, companion slot, promoted renderer).
  - YouTube pre-roll tidak di-hard-block; video resource `googlevideo/videoplayback` tetap di-allow.
  - skip iklan lebih natural: mute + speed 16x + seek bertahap 8 detik, bukan lompat paksa ke akhir yang bisa bikin player hitam.
  - restore mute/speed setelah mode iklan selesai.
  - block ringan hanya metadata/tracking iklan YouTube non-media seperti `/api/stats/ads`, `pagead`, `ptracking`, bukan file video.
- Download Queue Manager v0.9.6 tetap dipertahankan.


## v0.9.8
- Judul dialog download lanjutan diganti dari “Fitur UC Download” menjadi “Yield Fast Download”.
- Pilihan batas maksimal download aktif sekarang hanya 2, 3, dan 4.
- Setting lama yang masih bernilai 1 otomatis dinaikkan menjadi minimal 2.
- Fitur Download Queue Manager dan AdBlock YouTube v0.9.7 tetap dipertahankan.


## v0.9.9
- Perbaikan klik iklan/deeplink yang menyebabkan halaman error `ERR_UNKNOWN_URL_SCHEME`.
- Link eksternal seperti `shopeeid://`, `intent://`, `market://`, Lazada/Tokopedia deeplink, dan deeplink affiliate iklan sekarang diblokir sebelum dimuat sebagai halaman.
- Saat redirect iklan diblokir, Yield mencoba tetap di halaman asli / memulihkan URL web terakhir yang aman.
- Ditambahkan recovery `onReceivedError` untuk main-frame unknown scheme agar halaman tidak stuck di error WebView.
- Parameter klik iklan/affiliate seperti `reactPath`, `navigate_url`, `deep_and_deferred`, `utm_medium=affiliates`, `click_id`, dan `adurl` lebih cepat ditahan.
- MainActivity dirapikan secara aman:
  - section comment untuk area utama.
  - helper URL safety dibuat terpisah.
  - naming UC pada dialog diganti menjadi branding Yield.
  - cleanup teks/komentar lama yang tidak perlu tanpa memecah struktur besar agar risiko build error tetap rendah.
- Fitur Yield Fast Download, Download Queue Manager, dan AdBlock YouTube v0.9.8 tetap dipertahankan.


## v0.9.10
- Perbaikan minimize aplikasi:
  - saat tombol Home/Recent Android ditekan, Yield tidak lagi masuk floating/Picture-in-Picture.
  - app kembali ke recent apps normal seperti aplikasi belum dibuka.
  - ketika dibuka lagi dari recent apps, halaman/menu terakhir tetap dipertahankan.
  - menu Home di dalam Yield tetap berfungsi untuk kembali ke beranda Yield.
- `supportsPictureInPicture` di manifest dimatikan agar tidak muncul jendela melayang di launcher.
- Setting video diubah menjadi “Minimize normal / tanpa floating”.
- Fitur v0.9.9 tetap dipertahankan.


## v0.9.11
- Perbaikan bug dialog Download Queue yang kadang kedip/keluar saat memilih batas maksimal aktif.
- Pilihan batas maksimal aktif sekarang tampil sebagai chip stabil di dialog: 2, 3, dan 4 file aktif.
- Klik ON/OFF Download Queue tidak menutup/reopen menu lagi.
- Klik 2/3/4 langsung update setting di tempat, dialog tetap terbuka.
- Pause semua, Resume semua, dan Urutkan Antrian tetap di menu yang sama.
- Fallback dialog lama juga dibuat tidak menutup parent menu.


## v0.9.12
- Perbaikan redirect iklan saat baca komik/website bergambar:
  - link/direct iklan sekarang dipisah ke tab baru iklan.
  - tab utama tidak dipaksa reload agar gambar komik tidak terputus.
  - recovery ke URL terakhir hanya dilakukan kalau tab utama benar-benar sudah berubah ke halaman iklan/error.
  - duplicate tab iklan dicegah agar tidak spam.
  - auto close tab iklan dibuat lebih halus agar tidak terasa flicker.
- Toast/label redirect diubah menjadi “Iklan dibuka di tab baru”.
- Fitur v0.9.11 tetap dipertahankan.


## v0.9.13
- Perbaikan direct image redirect saat baca komik:
  - jika klik iklan/direct link mengubah tab utama ke URL gambar `.jpg/.jpeg/.png/.webp`, Yield menahannya.
  - direct image main-frame dibuka sebagai tab baru/direct link, bukan mengganti tab komik utama.
  - tab utama tidak dipaksa reload, sehingga gambar komik yang sedang load tidak mudah terputus.
- Perbaikan minimize di Realme/Oppo:
  - `resizeableActivity` dimatikan agar tidak masuk floating/freeform window.
  - PiP tetap OFF dan recent apps tetap normal.
- Riwayat browsing dibuat lebih aman:
  - tidak auto-clear saat data pref kosong/parse gagal.
  - direct image/ad click tidak masuk histori.
  - batas histori dinaikkan sampai 500 item.
  - hapus histori tetap manual lewat menu “Kelola / hapus riwayat...” atau tombol `×` per item.
