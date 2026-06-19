# YieldBrowser v0.9.85 — Download Engine Fix Summary

## Akar masalah Google Drive lambat

Versi sebelumnya memaksa Google Drive ke dua koneksi, mengulang redirect untuk probe/part/retry, dan mengirim header browser yang tidak diperlukan. Queue juga dapat dilewati sehingga beberapa file berbagi bandwidth tanpa mengikuti batas aktif.

Perbaikannya:

1. URL Google Drive dikenali dan dinormalisasi.
2. URL final setelah redirect di-cache dan digunakan oleh seluruh worker.
3. File Google Drive berukuran minimal 64 MB mencoba tiga koneksi adaptif.
4. Jika signed URL mengembalikan 401/403, engine kembali ke URL sumber dan mengambil URL baru.
5. Speed limiter sekarang membatasi total file, bukan masing-masing worker.
6. Queue manager selalu digunakan.

## Proteksi integritas

- Probe dan part Range harus mengembalikan HTTP 206.
- Awal/akhir `Content-Range` harus identik dengan request.
- `Content-Length`, total server, byte per part, dan ukuran file akhir diverifikasi.
- `ETag`/`Last-Modified` disimpan; `If-Range` dikirim saat resume.
- Sparse file tidak boleh dianggap lengkap hanya karena `setLength(total)`.

## Resume dan lifecycle

- Posisi 1–4 part dipersistenkan.
- Resume 3 koneksi tidak lagi dimulai dari nol.
- Pause menutup transport aktif dan mempertahankan byte part yang valid.
- Worker lama tidak dapat menyelesaikan/menggagalkan sesi baru karena generation token.
- Proses yang mati memulihkan status running menjadi paused.

## Batasan yang disengaja

- `SAMPLE-AES` tidak didekripsi.
- AES-128 yang dikombinasikan dengan byte-range ditolak karena membutuhkan penanganan chaining khusus agar aman.
- Foreground service menjaga proses dan wake lock, tetapi network worker masih diorkestrasi oleh proses aplikasi; ini bukan implementasi WorkManager terpisah.
