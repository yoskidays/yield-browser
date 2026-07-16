package com.yieldbrowser.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bounded favicon network and memory-cache pipeline shared by shortcuts, history, and bookmarks.
 */
final class HistoryFaviconLoader {
    private final Handler mainHandler;
    private final ExecutorService executor;
    private final LruCache<String, Bitmap> memoryCache;

    HistoryFaviconLoader(Handler mainHandler) {
        this.mainHandler = mainHandler;
        this.executor = Executors.newFixedThreadPool(HistoryFaviconPolicy.WORKER_COUNT);
        this.memoryCache = new LruCache<>(HistoryFaviconPolicy.MEMORY_CACHE_ENTRIES);
    }

    void load(String pageUrl, ImageView target, TextView fallback) {
        if (target == null || fallback == null) return;

        final String requestKey = HistoryFaviconPolicy.requestKey(pageUrl);
        target.setTag(requestKey);
        target.setVisibility(View.GONE);
        fallback.setVisibility(View.VISIBLE);
        if (requestKey.isEmpty()) return;

        Bitmap cached = memoryCache.get(requestKey);
        if (cached != null) {
            applyBitmap(requestKey, cached, target, fallback);
            return;
        }

        final String requestUrl = HistoryFaviconPolicy.requestUrl(pageUrl);
        if (requestUrl.isEmpty()) return;

        try {
            executor.execute(() -> fetchAndApply(requestKey, requestUrl, target, fallback));
        } catch (RuntimeException ignored) {
            // The Activity may already be shutting down and the executor may reject new work.
        }
    }

    void shutdown() {
        try {
            executor.shutdownNow();
        } catch (Exception ignored) {
        }
        try {
            memoryCache.evictAll();
        } catch (Exception ignored) {
        }
    }

    private void fetchAndApply(String requestKey,
                               String requestUrl,
                               ImageView target,
                               TextView fallback) {
        HttpURLConnection connection = null;
        InputStream input = null;
        try {
            connection = (HttpURLConnection) new URL(requestUrl).openConnection();
            connection.setConnectTimeout(HistoryFaviconPolicy.CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(HistoryFaviconPolicy.READ_TIMEOUT_MS);
            connection.setUseCaches(true);
            input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            if (bitmap == null) return;
            memoryCache.put(requestKey, bitmap);
            postBitmap(requestKey, bitmap, target, fallback);
        } catch (Exception ignored) {
        } finally {
            try {
                if (input != null) input.close();
            } catch (Exception ignored) {
            }
            try {
                if (connection != null) connection.disconnect();
            } catch (Exception ignored) {
            }
        }
    }

    private void postBitmap(String requestKey,
                            Bitmap bitmap,
                            ImageView target,
                            TextView fallback) {
        if (mainHandler == null) return;
        try {
            mainHandler.post(() -> applyBitmap(requestKey, bitmap, target, fallback));
        } catch (Exception ignored) {
        }
    }

    private void applyBitmap(String requestKey,
                             Bitmap bitmap,
                             ImageView target,
                             TextView fallback) {
        if (bitmap == null || target == null || fallback == null) return;
        if (!HistoryFaviconPolicy.matchesTarget(requestKey, target.getTag())) return;
        target.setImageBitmap(bitmap);
        target.setVisibility(View.VISIBLE);
        fallback.setVisibility(View.GONE);
    }
}
