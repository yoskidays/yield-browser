# Yield Browser APK Testing Protocol

## Tujuan
Menjaga data pengguna seperti bookmark saat melakukan pengembangan dan pengujian APK.

## Aturan Testing APK

1. Setiap perubahan besar wajib dibuatkan APK TEST terpisah terlebih dahulu.
2. APK TEST tidak boleh menimpa APK pengguna lama.
3. APK TEST harus menggunakan applicationId/package berbeda agar dapat dipasang berdampingan.
4. Pengujian dilakukan pada APK TEST terlebih dahulu.
5. Jangan meminta pengguna uninstall APK utama sebelum backup data dipastikan aman.

## Setelah Testing Berhasil

Jika APK TEST sudah lolos:

1. Build APK utama release dengan signing key yang sama.
2. Pertahankan applicationId utama.
3. Naikkan versionCode dan versionName.
4. Update APK lama melalui instalasi update normal.
5. Pastikan bookmark, history, dan pengaturan tetap tersimpan.

## Kondisi Saat Ini

APK lama pengguna adalah APK utama yang berisi data bookmark.
APK test digunakan hanya untuk validasi fitur baru seperti Shield, adblock, dan perubahan browser engine.

Jangan mengganti APK utama sebelum APK TEST terbukti stabil.

## Alur Default Percakapan Baru

Saat melakukan perubahan Yield Browser:

1. Cek repository dan file TESTING_PROTOCOL.md.
2. Buat APK TEST terpisah.
3. Minta pengguna menguji APK TEST.
4. Setelah konfirmasi berhasil, siapkan APK update utama.
5. Prioritaskan keamanan bookmark dan data pengguna.
