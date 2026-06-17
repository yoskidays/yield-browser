package com.yieldbrowser.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DownloadOpenReceiver extends BroadcastReceiver {
    public static final String ACTION_OPEN_DOWNLOADS = "com.yieldbrowser.app.OPEN_DOWNLOADS";

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent open = new Intent(context, MainActivity.class);
        open.setAction(ACTION_OPEN_DOWNLOADS);
        open.putExtra("open_downloads", true);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(open);
    }
}
