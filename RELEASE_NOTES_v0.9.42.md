# Yield Browser v0.9.42

## Lordborg / compatibility navigation fix

- Memperbaiki halaman seperti lordborg.com yang menjadi blank setelah reload-loop guard aktif.
- Reload-loop guard tidak lagi menghentikan semua navigasi pada host yang sama; menu internal situs tetap bisa dibuka.
- AdBlock tidak lagi memblokir navigasi utama yang jelas berasal dari klik user atau hasil pencarian Google/Bing/DuckDuckGo/Yahoo/Yandex.
- Resource first-party pada situs yang masuk compatibility guard tidak diblokir, supaya script/menu inti situs tidak membuat halaman blank.
- Desktop/Mobile mode v0.9.40/v0.9.41 tidak disentuh.
