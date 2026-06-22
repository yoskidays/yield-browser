package com.yieldbrowser.app;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;

/**
 * Dedicated incognito browser window.
 *
 * This Activity runs in the :incognito process. On Android 9+ that process receives its own
 * WebView data-directory suffix, so cookies, HTTP auth, service workers, cache, IndexedDB and
 * local storage never share the normal browser profile.
 */
public final class PrivateBrowserActivity extends MainActivity {
    @Override
    protected boolean useDedicatedPrivateProfile() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || isFinishing()) return;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().setStatusBarColor(Color.parseColor("#3B176B"));
        setTitle("Yield Privat");
        PrivateProfileCleaner.clear(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PrivateProfileCleaner.clear(this);
    }
}
