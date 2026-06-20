package com.yieldbrowser.app;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** HLS parser for master playlists, byte ranges, fMP4 init maps, and AES-128 keys. */
final class HlsPlaylistParser {
    private HlsPlaylistParser() {
    }

    static Playlist parse(String playlistUrl, String text) throws Exception {
        Playlist result = new Playlist();
        String[] lines = text == null ? new String[0] : text.replace("\r", "").split("\n");
        long pendingBandwidth = 0L;
        boolean expectVariant = false;
        ByteRange pendingRange = null;
        long implicitOffset = 0L;
        long mediaSequence = 0L;
        long segmentIndex = 0L;
        KeyInfo currentKey = null;

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("#EXT-X-STREAM-INF:")) {
                pendingBandwidth = parseLongAttribute(line, "BANDWIDTH", 0L);
                expectVariant = true;
                continue;
            }
            if (line.startsWith("#EXT-X-MEDIA-SEQUENCE:")) {
                try {
                    mediaSequence = Long.parseLong(line.substring(line.indexOf(':') + 1).trim());
                } catch (NumberFormatException ignored) {
                    mediaSequence = 0L;
                }
                continue;
            }
            if (line.startsWith("#EXT-X-KEY:")) {
                String method = parseStringAttribute(line, "METHOD");
                if (method == null || method.equalsIgnoreCase("NONE")) {
                    currentKey = null;
                } else if (method.equalsIgnoreCase("AES-128")) {
                    String uri = parseStringAttribute(line, "URI");
                    if (uri == null || uri.isEmpty()) {
                        result.unsupportedEncryption = true;
                        currentKey = null;
                    } else {
                        String ivText = parseStringAttribute(line, "IV");
                        byte[] explicitIv = parseHexIv(ivText);
                        if (ivText != null && explicitIv == null) result.unsupportedEncryption = true;
                        currentKey = new KeyInfo(resolve(playlistUrl, uri), explicitIv);
                        result.encrypted = true;
                    }
                } else {
                    result.unsupportedEncryption = true;
                    currentKey = null;
                }
                continue;
            }
            if (line.startsWith("#EXT-X-MAP:")) {
                String uri = parseStringAttribute(line, "URI");
                String range = parseStringAttribute(line, "BYTERANGE");
                ByteRange byteRange = parseByteRange(range, 0L);
                if (uri != null && !uri.isEmpty()) {
                    if (currentKey != null && currentKey.explicitIv == null) {
                        result.unsupportedEncryption = true;
                    }
                    result.initMap = new Resource(resolve(playlistUrl, uri), byteRange,
                            currentKey, mediaSequence);
                }
                continue;
            }
            if (line.startsWith("#EXT-X-BYTERANGE:")) {
                pendingRange = parseByteRange(line.substring(line.indexOf(':') + 1).trim(), implicitOffset);
                if (pendingRange != null) implicitOffset = pendingRange.end + 1L;
                continue;
            }
            if (line.startsWith("#")) continue;

            String resolved = resolve(playlistUrl, line);
            if (expectVariant) {
                result.variants.add(new Variant(resolved, pendingBandwidth));
                expectVariant = false;
                pendingBandwidth = 0L;
            } else {
                result.segments.add(new Resource(resolved, pendingRange, currentKey,
                        mediaSequence + segmentIndex));
                segmentIndex++;
                pendingRange = null;
            }
        }
        result.variants.sort(Comparator.comparingLong((Variant variant) -> variant.bandwidth).reversed());
        return result;
    }

    private static String resolve(String base, String value) throws Exception {
        return new URL(new URL(base), value).toString();
    }

    private static long parseLongAttribute(String line, String key, long fallback) {
        String value = parseStringAttribute(line, key);
        if (value == null) return fallback;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String parseStringAttribute(String line, String key) {
        int colon = line.indexOf(':');
        if (colon < 0) return null;
        String body = line.substring(colon + 1);
        String target = key.toUpperCase(Locale.US) + "=";
        for (int index = 0; index <= body.length() - target.length(); index++) {
            if (!body.regionMatches(true, index, target, 0, target.length())) continue;
            int start = index + target.length();
            if (start < body.length() && body.charAt(start) == '"') {
                int end = body.indexOf('"', start + 1);
                return end < 0 ? body.substring(start + 1) : body.substring(start + 1, end);
            }
            int end = body.indexOf(',', start);
            return (end < 0 ? body.substring(start) : body.substring(start, end)).trim();
        }
        return null;
    }

    private static ByteRange parseByteRange(String value, long implicitOffset) {
        if (value == null || value.trim().isEmpty()) return null;
        String clean = value.trim().replace("\"", "");
        int at = clean.indexOf('@');
        try {
            long length = Long.parseLong(at >= 0 ? clean.substring(0, at) : clean);
            long start = at >= 0 ? Long.parseLong(clean.substring(at + 1)) : implicitOffset;
            if (length <= 0 || start < 0) return null;
            return new ByteRange(start, start + length - 1L);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static byte[] parseHexIv(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        String hex = value.trim().toLowerCase(Locale.US);
        if (hex.startsWith("0x")) hex = hex.substring(2);
        if (hex.length() > 32) return null;
        StringBuilder padded = new StringBuilder();
        for (int index = hex.length(); index < 32; index++) padded.append('0');
        padded.append(hex);
        byte[] result = new byte[16];
        try {
            for (int index = 0; index < result.length; index++) {
                result[index] = (byte) Integer.parseInt(padded.substring(index * 2, index * 2 + 2), 16);
            }
            return result;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static final class Playlist {
        final List<Variant> variants = new ArrayList<>();
        final List<Resource> segments = new ArrayList<>();
        Resource initMap;
        boolean encrypted;
        boolean unsupportedEncryption;
    }

    static final class Variant {
        final String url;
        final long bandwidth;

        Variant(String url, long bandwidth) {
            this.url = url;
            this.bandwidth = bandwidth;
        }
    }

    static final class Resource {
        final String url;
        final ByteRange range;
        final KeyInfo key;
        final long sequence;

        Resource(String url, ByteRange range, KeyInfo key, long sequence) {
            this.url = url;
            this.range = range;
            this.key = key;
            this.sequence = sequence;
        }
    }

    static final class KeyInfo {
        final String url;
        final byte[] explicitIv;

        KeyInfo(String url, byte[] explicitIv) {
            this.url = url;
            this.explicitIv = explicitIv;
        }
    }

    static final class ByteRange {
        final long start;
        final long end;

        ByteRange(long start, long end) {
            this.start = start;
            this.end = end;
        }
    }
}
