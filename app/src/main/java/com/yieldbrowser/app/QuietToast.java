package com.yieldbrowser.app;

import android.content.Context;

import java.util.Locale;

/**
 * Centralized low-noise toast policy.
 *
 * Routine confirmations, toggle state messages, navigation notices, and automatic
 * browser recovery messages are intentionally suppressed. Only errors that require
 * direct user action are shown. This keeps browsing clean while preserving critical
 * feedback when an operation genuinely fails.
 */
final class QuietToast {
    static final int LENGTH_SHORT = android.widget.Toast.LENGTH_SHORT;
    static final int LENGTH_LONG = android.widget.Toast.LENGTH_LONG;

    private final Context context;
    private final CharSequence text;
    private final int duration;

    private QuietToast(Context context, CharSequence text, int duration) {
        this.context = context == null ? null : context.getApplicationContext();
        this.text = text;
        this.duration = duration;
    }

    static QuietToast makeText(Context context, CharSequence text, int duration) {
        return new QuietToast(context, text, duration);
    }

    void show() {
        if (context == null || text == null || !isCritical(text)) return;
        android.widget.Toast.makeText(context, text, duration).show();
    }

    private static boolean isCritical(CharSequence message) {
        String text = String.valueOf(message).trim().toLowerCase(Locale.US);
        if (text.length() == 0) return false;

        return text.startsWith("url tidak valid")
                || text.contains("izin kamera diperlukan")
                || text.startsWith("kamera tidak bisa dibuka")
                || text.contains("profil privat tidak dapat dibuka")
                || text.contains("tab umum tidak dapat dibuka")
                || text.startsWith("gagal membuka menu")
                || text.startsWith("gagal menyimpan halaman")
                || text.startsWith("riwayat gagal dihapus")
                || text.startsWith("pemilih folder tidak tersedia")
                || text.startsWith("aksi download gagal")
                || text.startsWith("player belum dapat dibuka")
                || text.equals("file tidak ditemukan")
                || text.startsWith("gagal membagikan file")
                || text.startsWith("gagal mengganti nama")
                || text.startsWith("tidak ada aplikasi untuk membuka file")
                || text.startsWith("gagal memulai unduhan")
                || text.startsWith("translate kompatibel gagal")
                || text.startsWith("gagal ambil teks halaman")
                || text.startsWith("gagal reload website")
                || text.startsWith("tidak bisa memilih elemen")
                || text.startsWith("tidak bisa menentukan domain");
    }
}
