package com.yieldbrowser.app;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.webkit.WebView;

import java.io.File;
import java.util.List;

/** Initializes process-specific WebView storage before any WebView instance is created. */
public final class YieldBrowserApplication extends Application {
    static final String INCOGNITO_PROCESS_SUFFIX = ":incognito";
    private static final String INCOGNITO_DATA_SUFFIX = "yield_incognito";
    private static volatile boolean incognitoProfileReady = false;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && isIncognitoProcess(base)) {
            purgeOldIncognitoDirectory(base);
            try {
                WebView.setDataDirectorySuffix(INCOGNITO_DATA_SUFFIX);
                incognitoProfileReady = true;
            } catch (IllegalStateException ignored) {
                incognitoProfileReady = false;
                // A WebView was initialized too early. PrivateBrowserActivity will fail closed
                // rather than silently sharing the normal profile.
            }
        }
    }


    private static void purgeOldIncognitoDirectory(Context context) {
        try {
            File data = context.getDataDir();
            deleteRecursively(new File(data, "app_webview_" + INCOGNITO_DATA_SUFFIX));
            deleteRecursively(new File(new File(data, "app_webview"), INCOGNITO_DATA_SUFFIX));
        } catch (Exception ignored) {
        }
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) deleteRecursively(child);
        }
        try {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        } catch (Exception ignored) {
        }
    }

    static boolean isIncognitoProfileReady() {
        return incognitoProfileReady;
    }

    static boolean isIncognitoProcess(Context context) {
        String processName = currentProcessName(context);
        return processName != null && processName.endsWith(INCOGNITO_PROCESS_SUFFIX);
    }

    private static String currentProcessName(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return Application.getProcessName();
            } catch (Exception ignored) {
            }
        }
        try {
            int pid = android.os.Process.myPid();
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager != null) {
                List<ActivityManager.RunningAppProcessInfo> processes = manager.getRunningAppProcesses();
                if (processes != null) {
                    for (ActivityManager.RunningAppProcessInfo info : processes) {
                        if (info != null && info.pid == pid) return info.processName;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
