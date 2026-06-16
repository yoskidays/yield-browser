# Yield Browser v0.9.41

## Fokus
Fix situs tertentu yang reload terus, terutama situs berat iklan/anti-adblock seperti lordborg.com.

## Perubahan
- Tambah reload-loop detector di `onPageStarted()`.
- Jika URL/host yang sama reload berulang dalam ±12 detik, Yield aktifkan guard selama ±90 detik.
- Saat guard aktif, Yield menghentikan `stopLoading()` pada reload berulang dan melewati injection rutin agar situs tidak terpancing reload lagi.
- Adblock/translate/night/video injection tidak dipaksa jalan saat host sedang guarded.
- Script injection rutin diganti ke `evaluateJavascript()` supaya tidak diperlakukan sebagai navigasi halaman.
- Cookie dan third-party cookie WebView diaktifkan.

## Tidak diubah
- Desktop/Mobile Mode hard reload v0.9.40.
- Download Manager / Queue.
- Bookmark, History, Shortcut Home.
- Full Video dan Landscape Video.

## Test
1. Matikan Adblock dan Translate.
2. Buka `https://lordborg.com`.
3. Jika situs mencoba reload berulang, Yield harus menahan loop dan menampilkan toast "Reload loop dicegah untuk situs ini".
4. Coba Desktop/Mobile Mode tetap ON/OFF seperti v0.9.40.

## Version
- versionCode: 16
- versionName: 0.9.41
