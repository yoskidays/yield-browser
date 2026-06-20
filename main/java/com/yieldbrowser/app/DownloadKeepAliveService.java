package com.yieldbrowser.app;

import static com.yieldbrowser.app.BrowserConstants.ACTION_OPEN_DOWNLOADS;
import static com.yieldbrowser.app.BrowserConstants.CHANNEL_DOWNLOADS;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Foreground owner for active downloads.
 *
 * Normal and incognito browser processes report their active downloads to this service. The
 * service exposes one real download-progress notification and never adds a second
 * "download engine/background protection" notification.
 */
public class DownloadKeepAliveService extends Service {
    private static final String ACTION_UPDATE = "com.yieldbrowser.app.DOWNLOAD_FOREGROUND_UPDATE";
    private static final String ACTION_CLEAR_CLIENT = "com.yieldbrowser.app.DOWNLOAD_FOREGROUND_CLEAR";
    private static final int NORMAL_FOREGROUND_NOTIFICATION_ID = 91001;
    private static final int PRIVATE_FOREGROUND_NOTIFICATION_ID = 91002;
    private static final long CLIENT_STALE_AFTER_MS = 120_000L;

    private static final String EXTRA_CLIENT_ID = "client_id";
    private static final String EXTRA_FILE_NAME = "file_name";
    private static final String EXTRA_TEXT = "text";
    private static final String EXTRA_PROGRESS = "progress";
    private static final String EXTRA_ACTIVE_COUNT = "active_count";

    /** This flag is process-local and only prevents repeated foreground-start requests. */
    private static final AtomicBoolean START_REQUESTED = new AtomicBoolean(false);

    private final Map<String, ClientState> clients = new HashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private PowerManager.WakeLock wakeLock;

    private final Runnable staleClientSweep = new Runnable() {
        @Override
        public void run() {
            pruneStaleClients();
            if (totalActiveDownloads() <= 0) {
                stopForegroundAndSelf();
                return;
            }
            publishCombinedNotification();
            handler.postDelayed(this, 60_000L);
        }
    };

    static void startOrUpdate(Context context, DownloadItem item, int activeCount, String text) {
        if (context == null || item == null || activeCount <= 0) return;
        Intent intent = new Intent(context, serviceClass(context));
        intent.setAction(ACTION_UPDATE);
        intent.putExtra(EXTRA_CLIENT_ID, clientId(context));
        intent.putExtra(EXTRA_FILE_NAME, item.fileName == null ? "Unduhan" : item.fileName);
        intent.putExtra(EXTRA_TEXT, text == null || text.trim().isEmpty()
                ? "Mengunduh" : text.trim());
        int visibleProgress = ("saving".equals(item.status) || "verifying".equals(item.status))
                ? item.finalizeProgress : item.progress;
        intent.putExtra(EXTRA_PROGRESS, Math.max(0, Math.min(100, visibleProgress)));
        intent.putExtra(EXTRA_ACTIVE_COUNT, activeCount);

        boolean firstStart = START_REQUESTED.compareAndSet(false, true);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && firstStart) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception firstError) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent);
                else context.startService(intent);
            } catch (Exception ignored) {
                START_REQUESTED.set(false);
            }
        }
    }

    static void stop(Context context) {
        START_REQUESTED.set(false);
        if (context == null) return;
        Intent intent = new Intent(context, serviceClass(context));
        intent.setAction(ACTION_CLEAR_CLIENT);
        intent.putExtra(EXTRA_CLIENT_ID, clientId(context));
        try {
            context.startService(intent);
        } catch (Exception ignored) {
            // The service is already absent, so there is nothing left to clear.
        }
    }

    private static String clientId(Context context) {
        return YieldBrowserApplication.isIncognitoProcess(context) ? "incognito" : "normal";
    }

    private static Class<? extends Service> serviceClass(Context context) {
        return YieldBrowserApplication.isIncognitoProcess(context)
                ? PrivateDownloadKeepAliveService.class : DownloadKeepAliveService.class;
    }

    private int foregroundNotificationId() {
        return YieldBrowserApplication.isIncognitoProcess(this)
                ? PRIVATE_FOREGROUND_NOTIFICATION_ID : NORMAL_FOREGROUND_NOTIFICATION_ID;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        createDownloadChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        String clientId = intent == null ? "normal" : intent.getStringExtra(EXTRA_CLIENT_ID);
        if (clientId == null || clientId.trim().isEmpty()) clientId = "normal";

        if (ACTION_CLEAR_CLIENT.equals(action)) {
            clients.remove(clientId);
            pruneStaleClients();
            if (totalActiveDownloads() <= 0) {
                stopForegroundAndSelf();
            } else {
                publishCombinedNotification();
            }
            return START_NOT_STICKY;
        }

        if (ACTION_UPDATE.equals(action)) {
            ClientState state = new ClientState();
            state.clientId = clientId;
            state.fileName = safeText(intent.getStringExtra(EXTRA_FILE_NAME), "Unduhan");
            state.text = safeText(intent.getStringExtra(EXTRA_TEXT), "Mengunduh");
            state.progress = Math.max(0, Math.min(100, intent.getIntExtra(EXTRA_PROGRESS, 0)));
            state.activeCount = Math.max(1, intent.getIntExtra(EXTRA_ACTIVE_COUNT, 1));
            state.updatedAtMs = System.currentTimeMillis();
            clients.put(clientId, state);
        }

        pruneStaleClients();
        if (totalActiveDownloads() <= 0) {
            stopForegroundAndSelf();
            return START_NOT_STICKY;
        }

        acquireWakeLock();
        publishCombinedNotification();
        handler.removeCallbacks(staleClientSweep);
        handler.postDelayed(staleClientSweep, 60_000L);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(staleClientSweep);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            //noinspection deprecation
            stopForeground(true);
        }
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        wakeLock = null;
        clients.clear();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void publishCombinedNotification() {
        ClientState lead = mostRecentlyUpdatedClient();
        int activeCount = totalActiveDownloads();
        if (lead == null || activeCount <= 0) return;
        startForeground(foregroundNotificationId(),
                buildDownloadNotification(lead.fileName, lead.text, lead.progress, activeCount,
                        "incognito".equals(lead.clientId)));
    }

    private ClientState mostRecentlyUpdatedClient() {
        ClientState selected = null;
        for (ClientState state : clients.values()) {
            if (state == null || state.activeCount <= 0) continue;
            if (selected == null || state.updatedAtMs > selected.updatedAtMs) selected = state;
        }
        return selected;
    }

    private int totalActiveDownloads() {
        int total = 0;
        for (ClientState state : clients.values()) {
            if (state != null && state.activeCount > 0) total += state.activeCount;
        }
        return total;
    }

    private void pruneStaleClients() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, ClientState>> iterator = clients.entrySet().iterator();
        while (iterator.hasNext()) {
            ClientState state = iterator.next().getValue();
            if (state == null || state.activeCount <= 0
                    || now - state.updatedAtMs > CLIENT_STALE_AFTER_MS) {
                iterator.remove();
            }
        }
    }

    private void stopForegroundAndSelf() {
        handler.removeCallbacks(staleClientSweep);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            //noinspection deprecation
            stopForeground(true);
        }
        stopSelf();
    }

    private void acquireWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) return;
        PowerManager manager = (PowerManager) getSystemService(POWER_SERVICE);
        if (manager == null) return;
        wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "YieldBrowser:ActiveDownload");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire();
    }

    private void createDownloadChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_DOWNLOADS,
                "Yield Downloads", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Progress unduhan Yield Browser");
        channel.setSound(null, null);
        channel.enableVibration(false);
        manager.createNotificationChannel(channel);
        manager.deleteNotificationChannel("yield_download_engine");
    }

    private Notification buildDownloadNotification(String fileName, String text,
                                                     int progress, int activeCount,
                                                     boolean privateClient) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pending;
        if (privateClient) {
            Intent openPrivate = new Intent(this, PrivateBrowserActivity.class);
            openPrivate.putExtra("open_downloads", true);
            openPrivate.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            pending = PendingIntent.getActivity(this, foregroundNotificationId(),
                    openPrivate, flags);
        } else {
            Intent open = new Intent(this, DownloadOpenReceiver.class);
            open.setAction(ACTION_OPEN_DOWNLOADS);
            open.putExtra("open_downloads", true);
            pending = PendingIntent.getBroadcast(this, foregroundNotificationId(), open, flags);
        }

        String title = activeCount > 1
                ? activeCount + " unduhan aktif"
                : safeText(fileName, "Mengunduh file");
        String line = activeCount > 1
                ? safeText(fileName, "Unduhan") + " • " + safeText(text, "Mengunduh")
                : safeText(text, "Mengunduh");

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_DOWNLOADS)
                : new Notification.Builder(this);
        builder.setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(title)
                .setContentText(line)
                .setContentIntent(pending)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setAutoCancel(false)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setProgress(100, Math.max(0, Math.min(100, progress)), false);
        return builder.build();
    }

    private static String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static final class ClientState {
        String clientId;
        String fileName;
        String text;
        int progress;
        int activeCount;
        long updatedAtMs;
    }
}
