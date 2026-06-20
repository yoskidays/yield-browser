package com.yieldbrowser.app;

import android.content.Context;
import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.webkit.WebViewDatabase;

/** Clears all persistent state belonging to the dedicated incognito WebView process/profile. */
final class PrivateProfileCleaner {
    private PrivateProfileCleaner() {
    }

    static void clear(Context context) {
        if (context == null || !YieldBrowserApplication.isIncognitoProcess(context)) return;

        try {
            CookieManager cookies = CookieManager.getInstance();
            cookies.removeAllCookies(value -> {
                try {
                    cookies.flush();
                } catch (Exception ignored) {
                }
            });
            cookies.removeSessionCookies(null);
        } catch (Exception ignored) {
        }

        try {
            WebStorage.getInstance().deleteAllData();
        } catch (Exception ignored) {
        }

        try {
            WebViewDatabase database = WebViewDatabase.getInstance(context.getApplicationContext());
            database.clearFormData();
            database.clearHttpAuthUsernamePassword();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                // Kept only for old WebView implementations where this store still exists.
                database.clearUsernamePassword();
            }
        } catch (Exception ignored) {
        }
    }
}
