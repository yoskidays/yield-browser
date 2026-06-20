package com.yieldbrowser.app;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Loopback-only HTTP range server used by the internal video player.
 *
 * <p>The download engine may pre-allocate a multipart file. This server therefore never assumes
 * that file.length() means every byte is ready. It exposes only byte ranges that DownloadItem marks
 * as completed and waits for later ranges while the download continues.</p>
 */
final class ProgressiveDownloadServer {
    private static final long SESSION_IDLE_TTL_MS = 30L * 60L * 1000L;
    private static final long CLOSED_SESSION_TTL_MS = 90L * 1000L;
    private static final long WAIT_FOR_TOTAL_MS = 15_000L;
    private static final long WAIT_FOR_BYTES_MS = 120_000L;
    private static final int STREAM_BUFFER = 64 * 1024;

    private static final Object START_LOCK = new Object();
    private static final ConcurrentHashMap<String, SessionState> SESSIONS = new ConcurrentHashMap<>();
    private static final ExecutorService CLIENT_POOL = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "yield-progressive-client");
        thread.setDaemon(true);
        return thread;
    });

    private static volatile ServerSocket serverSocket;
    private static volatile int serverPort;

    private ProgressiveDownloadServer() {
    }

    static PlaybackSession open(Context context, DownloadItem item) throws IOException {
        if (context == null || item == null) throw new IOException("Sumber video tidak tersedia");
        ensureStarted();
        cleanupExpiredSessions();

        String token = UUID.randomUUID().toString().replace("-", "");
        SessionState state = new SessionState(context.getApplicationContext(), item, token);
        SESSIONS.put(token, state);
        String base = "http://127.0.0.1:" + serverPort;
        return new PlaybackSession(
                token,
                base + "/media/" + token,
                base + "/status/" + token,
                base + "/close/" + token
        );
    }

    private static void ensureStarted() throws IOException {
        if (serverSocket != null && !serverSocket.isClosed()) return;
        synchronized (START_LOCK) {
            if (serverSocket != null && !serverSocket.isClosed()) return;
            ServerSocket socket = new ServerSocket(0, 24, InetAddress.getByName("127.0.0.1"));
            socket.setReuseAddress(true);
            serverSocket = socket;
            serverPort = socket.getLocalPort();
            Thread acceptThread = new Thread(ProgressiveDownloadServer::acceptLoop,
                    "yield-progressive-server");
            acceptThread.setDaemon(true);
            acceptThread.start();
        }
    }

    private static void acceptLoop() {
        while (true) {
            ServerSocket local = serverSocket;
            if (local == null || local.isClosed()) return;
            try {
                Socket socket = local.accept();
                socket.setTcpNoDelay(true);
                socket.setSoTimeout(130_000);
                CLIENT_POOL.execute(() -> handleClient(socket));
            } catch (SocketException e) {
                return;
            } catch (Exception ignored) {
            }
        }
    }

    private static void handleClient(Socket socket) {
        try (Socket client = socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(
                     client.getInputStream(), StandardCharsets.ISO_8859_1));
             OutputStream output = client.getOutputStream()) {

            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.trim().isEmpty()) return;
            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 2) {
                sendText(output, 400, "Bad Request", "Permintaan tidak valid", "text/plain");
                return;
            }

            String method = requestParts[0].toUpperCase(Locale.US);
            String path = URLDecoder.decode(requestParts[1].split("\\?", 2)[0], "UTF-8");
            Map<String, String> headers = readHeaders(reader);

            if (path.startsWith("/status/")) {
                serveStatus(output, tokenAfter(path, "/status/"));
            } else if (path.startsWith("/close/")) {
                closeSession(output, tokenAfter(path, "/close/"));
            } else if (path.startsWith("/media/")) {
                serveMedia(method, headers, output, tokenAfter(path, "/media/"));
            } else {
                sendText(output, 404, "Not Found", "Sesi tidak ditemukan", "text/plain");
            }
        } catch (Exception ignored) {
        }
    }

    private static Map<String, String> readHeaders(BufferedReader reader) throws IOException {
        HashMap<String, String> headers = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            int colon = line.indexOf(':');
            if (colon <= 0) continue;
            headers.put(line.substring(0, colon).trim().toLowerCase(Locale.US),
                    line.substring(colon + 1).trim());
        }
        return headers;
    }

    private static String tokenAfter(String path, String prefix) {
        String token = path.substring(prefix.length());
        int slash = token.indexOf('/');
        return slash >= 0 ? token.substring(0, slash) : token;
    }

    private static void serveStatus(OutputStream output, String token) throws IOException {
        SessionState state = SESSIONS.get(token);
        if (state == null) {
            sendText(output, 404, "Not Found", "{\"available\":false}", "application/json");
            return;
        }
        state.touch();
        sendText(output, 200, "OK", state.statusJson(), "application/json");
    }

    private static void closeSession(OutputStream output, String token) throws IOException {
        SessionState state = SESSIONS.get(token);
        if (state != null) state.markClosed();
        sendText(output, 200, "OK", "{\"closed\":true}", "application/json");
    }

    private static void serveMedia(String method, Map<String, String> headers,
                                   OutputStream output, String token) throws IOException {
        SessionState state = SESSIONS.get(token);
        if (state == null) {
            sendText(output, 404, "Not Found", "Sesi video sudah berakhir", "text/plain");
            return;
        }
        state.touch();

        long total = state.waitForTotalLength(WAIT_FOR_TOTAL_MS);
        if (total <= 0) {
            sendText(output, 503, "Service Unavailable",
                    "Video masih menunggu informasi ukuran file", "text/plain", "Retry-After: 1\r\n");
            return;
        }

        Range range;
        try {
            range = parseRange(headers.get("range"), total);
        } catch (IllegalArgumentException e) {
            sendRangeNotSatisfiable(output, total);
            return;
        }

        boolean partial = headers.containsKey("range");
        String mime = state.mimeType();
        long contentLength = range.end - range.start + 1L;
        StringBuilder response = new StringBuilder();
        response.append("HTTP/1.1 ").append(partial ? "206 Partial Content" : "200 OK").append("\r\n");
        response.append("Content-Type: ").append(mime).append("\r\n");
        response.append("Accept-Ranges: bytes\r\n");
        response.append("Content-Length: ").append(contentLength).append("\r\n");
        if (partial) {
            response.append("Content-Range: bytes ").append(range.start).append('-')
                    .append(range.end).append('/').append(total).append("\r\n");
        }
        response.append("Cache-Control: no-store\r\n");
        response.append("Connection: close\r\n\r\n");
        output.write(response.toString().getBytes(StandardCharsets.ISO_8859_1));
        output.flush();
        if ("HEAD".equals(method)) return;
        if (!"GET".equals(method)) return;

        streamRange(state, output, range.start, range.end);
    }

    private static void streamRange(SessionState state, OutputStream output,
                                    long start, long end) throws IOException {
        byte[] buffer = new byte[STREAM_BUFFER];
        long position = start;
        long waitStarted = 0L;
        int sourceGeneration = state.currentGeneration();
        SeekableReader reader = null;
        try {
            while (position <= end) {
                if (state.currentGeneration() != sourceGeneration) return;
                state.touch();
                long availableEnd = state.availableEndExclusive(position);
                if (availableEnd <= position) {
                    if (state.isTerminalWithoutMoreBytes(position)) return;
                    if (waitStarted == 0L) waitStarted = System.currentTimeMillis();
                    if (System.currentTimeMillis() - waitStarted > WAIT_FOR_BYTES_MS) return;
                    sleepQuietly(140L);
                    continue;
                }
                waitStarted = 0L;
                if (reader == null) {
                    reader = state.openReader();
                    if (reader == null) {
                        sleepQuietly(120L);
                        continue;
                    }
                }

                int wanted = (int) Math.min(buffer.length,
                        Math.min(availableEnd - position, end - position + 1L));
                int read;
                try {
                    read = reader.read(position, buffer, wanted);
                } catch (IOException staleReader) {
                    closeQuietly(reader);
                    reader = state.openReader();
                    if (reader == null) throw staleReader;
                    read = reader.read(position, buffer, wanted);
                }
                if (read <= 0) {
                    closeQuietly(reader);
                    reader = null;
                    sleepQuietly(100L);
                    continue;
                }
                output.write(buffer, 0, read);
                output.flush();
                position += read;
            }
        } finally {
            closeQuietly(reader);
        }
    }

    private static Range parseRange(String value, long total) {
        if (value == null || value.trim().isEmpty()) return new Range(0, total - 1);
        String normalized = value.trim().toLowerCase(Locale.US);
        if (!normalized.startsWith("bytes=")) throw new IllegalArgumentException("Range tidak valid");
        String first = normalized.substring(6).split(",", 2)[0].trim();
        int dash = first.indexOf('-');
        if (dash < 0) throw new IllegalArgumentException("Range tidak valid");
        String startText = first.substring(0, dash).trim();
        String endText = first.substring(dash + 1).trim();
        long start;
        long end;
        if (startText.isEmpty()) {
            long suffix = Long.parseLong(endText);
            if (suffix <= 0) throw new IllegalArgumentException("Range tidak valid");
            start = Math.max(0L, total - suffix);
            end = total - 1L;
        } else {
            start = Long.parseLong(startText);
            end = endText.isEmpty() ? total - 1L : Long.parseLong(endText);
        }
        if (start < 0 || start >= total || end < start) throw new IllegalArgumentException("Range tidak valid");
        end = Math.min(end, total - 1L);
        return new Range(start, end);
    }

    private static void sendRangeNotSatisfiable(OutputStream output, long total) throws IOException {
        String response = "HTTP/1.1 416 Range Not Satisfiable\r\n"
                + "Content-Range: bytes */" + total + "\r\n"
                + "Content-Length: 0\r\nConnection: close\r\n\r\n";
        output.write(response.getBytes(StandardCharsets.ISO_8859_1));
        output.flush();
    }

    private static void sendText(OutputStream output, int code, String reason,
                                 String body, String contentType) throws IOException {
        sendText(output, code, reason, body, contentType, "");
    }

    private static void sendText(OutputStream output, int code, String reason,
                                 String body, String contentType, String extraHeaders) throws IOException {
        byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.ISO_8859_1));
        writer.write("HTTP/1.1 " + code + " " + reason + "\r\n");
        writer.write("Content-Type: " + contentType + "; charset=utf-8\r\n");
        writer.write("Content-Length: " + bytes.length + "\r\n");
        writer.write("Cache-Control: no-store\r\n");
        if (extraHeaders != null && !extraHeaders.isEmpty()) writer.write(extraHeaders);
        writer.write("Connection: close\r\n\r\n");
        writer.flush();
        output.write(bytes);
        output.flush();
    }

    private static void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, SessionState> entry : SESSIONS.entrySet()) {
            SessionState state = entry.getValue();
            long ttl = state.closedAtMs > 0 ? CLOSED_SESSION_TTL_MS : SESSION_IDLE_TTL_MS;
            long base = state.closedAtMs > 0 ? state.closedAtMs : state.lastAccessMs;
            if (now - base > ttl) SESSIONS.remove(entry.getKey(), state);
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) closeable.close();
        } catch (Exception ignored) {
        }
    }

    static final class PlaybackSession {
        final String token;
        final String mediaUrl;
        final String statusUrl;
        final String closeUrl;

        PlaybackSession(String token, String mediaUrl, String statusUrl, String closeUrl) {
            this.token = token;
            this.mediaUrl = mediaUrl;
            this.statusUrl = statusUrl;
            this.closeUrl = closeUrl;
        }
    }

    private static final class SessionState {
        final Context context;
        final DownloadItem item;
        final String token;
        volatile long lastAccessMs = System.currentTimeMillis();
        volatile long closedAtMs;

        SessionState(Context context, DownloadItem item, String token) {
            this.context = context;
            this.item = item;
            this.token = token;
        }

        void touch() {
            lastAccessMs = System.currentTimeMillis();
        }

        void markClosed() {
            closedAtMs = System.currentTimeMillis();
            touch();
        }

        int currentGeneration() {
            return item.runGeneration;
        }

        long waitForTotalLength(long timeoutMs) {
            long started = System.currentTimeMillis();
            long value;
            while ((value = totalLength()) <= 0L && System.currentTimeMillis() - started < timeoutMs) {
                if (isTerminalStatus()) break;
                sleepQuietly(100L);
            }
            return Math.max(0L, value);
        }

        long totalLength() {
            long configured = item.totalBytes;
            if (configured > 0L) return configured;
            File file = localFile();
            if (file != null && file.exists() && isTerminalStatus()) return file.length();
            long uriLength = publicUriLength();
            if (uriLength > 0L) return uriLength;
            return 0L;
        }

        long availableEndExclusive(long position) {
            long total = totalLength();
            if (total <= 0L || position < 0L || position >= total) return position;
            String status = safe(item.status);
            if ("completed".equals(status) || "saving".equals(status) || "verifying".equals(status)) {
                if (item.downloadedBytes >= total || item.hlsOutputBytes >= total || hasPublicUri()) return total;
            }

            int connections = Math.max(1, item.connectionCount);
            long fileLength = existingLocalLength();
            long sequentialAvailable = Math.max(item.downloadedBytes, item.hlsOutputBytes);
            if (connections <= 1 && fileLength > 0L) {
                sequentialAvailable = Math.max(sequentialAvailable, fileLength);
            }
            long[] starts = {item.part1Start, item.part2Start, item.part3Start, item.part4Start};
            long[] ends = {item.part1End, item.part2End, item.part3End, item.part4End};
            long[] done = {item.part1Done, item.part2Done, item.part3Done, item.part4Done};
            return ProgressivePlaybackPolicy.availableEndExclusive(
                    position, total, connections, sequentialAvailable, false,
                    starts, ends, done);
        }

        long availablePrefix() {
            return availableEndExclusive(0L);
        }

        boolean isTerminalWithoutMoreBytes(long position) {
            String status = safe(item.status);
            if ("removed".equals(status)) return true;
            if ("failed".equals(status) || "completed".equals(status)) {
                return availableEndExclusive(position) <= position;
            }
            return false;
        }

        boolean isTerminalStatus() {
            String status = safe(item.status);
            return "completed".equals(status) || "failed".equals(status) || "removed".equals(status);
        }

        SeekableReader openReader() throws IOException {
            File file = localFile();
            if (file != null && file.exists()) return new FileSeekableReader(file);
            if (hasPublicUri()) return new UriSeekableReader(context, Uri.parse(item.publicUri));
            return null;
        }

        String mimeType() {
            String extension = MimeTypeMap.getFileExtensionFromUrl(safe(item.fileName));
            String mime = extension == null ? null
                    : MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase(Locale.US));
            if (mime != null && mime.startsWith("video/")) return mime;
            return "video/mp4";
        }

        String statusJson() {
            long total = totalLength();
            long downloaded = Math.max(item.downloadedBytes, item.hlsOutputBytes);
            int progress = total > 0L
                    ? (int) Math.min(100L, downloaded * 100L / Math.max(1L, total))
                    : Math.max(0, Math.min(100, item.progress));
            long prefix = availablePrefix();
            boolean playable = total > 0L && (prefix >= 256L * 1024L || "completed".equals(item.status));
            return "{"
                    + "\"available\":true,"
                    + "\"status\":\"" + jsonEscape(safe(item.status)) + "\","
                    + "\"progress\":" + progress + ","
                    + "\"downloadedBytes\":" + Math.max(0L, downloaded) + ","
                    + "\"totalBytes\":" + Math.max(0L, total) + ","
                    + "\"availablePrefix\":" + Math.max(0L, prefix) + ","
                    + "\"speedBytesPerSecond\":" + Math.max(0L,
                            (long) Math.max(item.smoothedSpeedBytesPerSecond, item.speedBytesPerSecond)) + ","
                    + "\"playable\":" + playable + ","
                    + "\"fileName\":\"" + jsonEscape(safe(item.fileName)) + "\""
                    + "}";
        }

        private File localFile() {
            String path = safe(item.path);
            return path.isEmpty() ? null : new File(path);
        }

        private long existingLocalLength() {
            try {
                File file = localFile();
                return file != null && file.exists() ? file.length() : 0L;
            } catch (Exception ignored) {
                return 0L;
            }
        }

        private boolean hasPublicUri() {
            return item.publicUri != null && !item.publicUri.trim().isEmpty();
        }

        private long publicUriLength() {
            if (!hasPublicUri()) return 0L;
            try (AssetFileDescriptor descriptor = context.getContentResolver()
                    .openAssetFileDescriptor(Uri.parse(item.publicUri), "r")) {
                if (descriptor == null) return 0L;
                long length = descriptor.getLength();
                return length > 0L ? length : 0L;
            } catch (Exception ignored) {
                return 0L;
            }
        }
    }

    private interface SeekableReader extends Closeable {
        int read(long position, byte[] buffer, int length) throws IOException;
    }

    private static final class FileSeekableReader implements SeekableReader {
        private final RandomAccessFile file;

        FileSeekableReader(File source) throws IOException {
            file = new RandomAccessFile(source, "r");
        }

        @Override
        public int read(long position, byte[] buffer, int length) throws IOException {
            file.seek(position);
            return file.read(buffer, 0, length);
        }

        @Override
        public void close() throws IOException {
            file.close();
        }
    }

    private static final class UriSeekableReader implements SeekableReader {
        private final AssetFileDescriptor descriptor;
        private final FileInputStream input;
        private final FileChannel channel;
        private final long startOffset;

        UriSeekableReader(Context context, Uri uri) throws IOException {
            descriptor = context.getContentResolver().openAssetFileDescriptor(uri, "r");
            if (descriptor == null) throw new IOException("File video tidak tersedia");
            input = new FileInputStream(descriptor.getFileDescriptor());
            channel = input.getChannel();
            startOffset = Math.max(0L, descriptor.getStartOffset());
        }

        @Override
        public int read(long position, byte[] buffer, int length) throws IOException {
            channel.position(startOffset + position);
            return input.read(buffer, 0, length);
        }

        @Override
        public void close() throws IOException {
            try {
                input.close();
            } finally {
                descriptor.close();
            }
        }
    }

    private static final class Range {
        final long start;
        final long end;

        Range(long start, long end) {
            this.start = start;
            this.end = end;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String jsonEscape(String value) {
        if (value == null) return "";
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': escaped.append("\\\""); break;
                case '\\': escaped.append("\\\\"); break;
                case '\b': escaped.append("\\b"); break;
                case '\f': escaped.append("\\f"); break;
                case '\n': escaped.append("\\n"); break;
                case '\r': escaped.append("\\r"); break;
                case '\t': escaped.append("\\t"); break;
                default:
                    if (c < 0x20) escaped.append(String.format(Locale.US, "\\u%04x", (int) c));
                    else escaped.append(c);
            }
        }
        return escaped.toString();
    }
}
