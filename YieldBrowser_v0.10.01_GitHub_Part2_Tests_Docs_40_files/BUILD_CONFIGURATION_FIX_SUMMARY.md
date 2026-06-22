# YieldBrowser v0.9.89 — AndroidX Build Configuration Fix

## Penyebab build gagal

Download Manager v0.9.88 menambahkan `androidx.recyclerview:recyclerview:1.3.2` dan `androidx.annotation:annotation:1.8.2`, tetapi root project belum memiliki `gradle.properties` yang mengaktifkan AndroidX. Akibatnya Gradle berhenti pada task `:app:checkDebugAarMetadata` sebelum kompilasi Java dimulai.

## Perbaikan

Root project sekarang mempunyai `gradle.properties` dengan konfigurasi:

```properties
android.useAndroidX=true
android.enableJetifier=false
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
```

- `android.useAndroidX=true` mengaktifkan dukungan AndroidX yang diwajibkan RecyclerView.
- `android.enableJetifier=false` dipertahankan karena source tidak memakai library lama `android.support.*`.
- JVM memory, parallel execution, dan build cache ditetapkan agar build lokal/CI lebih konsisten.

## Versi

- versionCode: 63
- versionName: 0.9.89
- GitHub Actions artifact: v0.9.89

## Validasi statis

- `gradle.properties` berada pada root project, sejajar dengan `settings.gradle`.
- Tidak ditemukan import atau dependency `android.support.*`.
- Dependency AndroidX tetap eksplisit pada `app/build.gradle`.
- Nama artifact workflow dan metadata aplikasi telah diselaraskan.

Build APK penuh perlu dijalankan melalui GitHub Actions atau mesin yang memiliki Android SDK dan Gradle 8.10.2.
