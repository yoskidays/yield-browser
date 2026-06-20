package com.yieldbrowser.app;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;

/** Mutable runtime and persisted state for one download. */
final class DownloadItem {
    final Object stateLock = new Object();

    volatile int id;
    volatile String url;
    volatile String resolvedUrl = "";
    volatile String fileName;
    volatile String path;
    volatile String status;
    volatile int progress;
    volatile long totalBytes;
    volatile long downloadedBytes;

    volatile int connectionCount;
    volatile int activeConnectionLimit;
    volatile boolean pauseRequested;
    volatile String engineInfo = "Menunggu koneksi";

    volatile String userAgent = "";
    volatile String referer = "";
    /** Runtime-only cookie snapshot; intentionally excluded from persistent download history. */
    volatile String cookieHeader = "";
    volatile String failReason = "";
    volatile String categoryHint = "";
    volatile String publicUri = "";
    volatile String etag = "";
    volatile String lastModified = "";
    volatile int retryCount;
    volatile int runGeneration;
    volatile boolean hlsDownload;

    volatile long part1Start;
    volatile long part1End;
    volatile long part1Done;
    volatile long part2Start;
    volatile long part2End;
    volatile long part2Done;
    volatile long part3Start;
    volatile long part3End;
    volatile long part3Done;
    volatile long part4Start;
    volatile long part4End;
    volatile long part4Done;

    volatile int hlsCompletedSegments;
    volatile long hlsOutputBytes;
    volatile String hlsPlaylistFingerprint = "";
    volatile boolean hlsInitMapWritten;

    volatile double speedBytesPerSecond;
    volatile double smoothedSpeedBytesPerSecond;
    volatile long etaSeconds = -1L;
    volatile int finalizeProgress;
    volatile long finalizeBytes;
    volatile long finalizeTotalBytes;
    volatile long lastSpeedTimeMs;
    volatile long lastSpeedBytes;
    volatile long lastActionClickMs;

    volatile String turboProfile = "";
    volatile int turboTargetConnections;
    volatile double turboAvgSpeedBytesPerSecond;
    volatile double turboStabilityScore = 100;
    volatile long turboLastSampleBytes;
    volatile long turboLastSampleTimeMs;
    volatile int turboSlowSamples;
    volatile int turboHealthySamples;
    volatile int turboRetryPenalty;
    volatile double turboJitterScore;
    volatile double turboPeakSpeedBytesPerSecond;
    volatile long turboLastPersistMs;
    volatile long lastProgressPersistMs;

    final DownloadRateLimiter rateLimiter = new DownloadRateLimiter();
    final ArrayList<HttpURLConnection> activeConnections = new ArrayList<>();
    final ArrayList<InputStream> activeStreams = new ArrayList<>();

    DownloadItem(int id, String url, String fileName, String path, String status, int progress) {
        this.id = id;
        this.url = url;
        this.fileName = fileName;
        this.path = path;
        this.status = status;
        this.progress = progress;
    }
}
