package com.yieldbrowser.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Opens the download manager when the user taps a download notification. */
public final class DownloadOpenReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent openDownloads = new Intent(context, MainActivity.class);
        openDownloads.setAction(BrowserConstants.ACTION_OPEN_DOWNLOADS);
        openDownloads.putExtra("open_downloads", true);
        openDownloads.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );
        context.startActivity(openDownloads);
    }
}
