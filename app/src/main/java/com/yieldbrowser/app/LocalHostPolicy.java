package com.yieldbrowser.app;

import java.util.Locale;

/** Pure local, private IPv4, and private IPv6 host classification. */
final class LocalHostPolicy {
    private LocalHostPolicy() {
    }

    static boolean isPrivateIpv4(String host) {
        try {
            String[] parts = host.split("\\.");
            if (parts.length != 4) return false;
            int[] octets = new int[4];
            for (int i = 0; i < 4; i++) {
                if (parts[i].length() == 0 || parts[i].length() > 3) return false;
                octets[i] = Integer.parseInt(parts[i]);
                if (octets[i] < 0 || octets[i] > 255) return false;
            }
            return octets[0] == 10
                    || octets[0] == 127
                    || (octets[0] == 169 && octets[1] == 254)
                    || (octets[0] == 172 && octets[1] >= 16 && octets[1] <= 31)
                    || (octets[0] == 192 && octets[1] == 168)
                    || octets[0] == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    static boolean isLocalOrPrivate(String host) {
        if (host == null || host.trim().length() == 0) return true;
        String normalized = host.trim().toLowerCase(Locale.US);
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (normalized.equals("localhost")
                || normalized.equals("::1")
                || normalized.equals("0:0:0:0:0:0:0:1")) return true;
        if (normalized.endsWith(".localhost")
                || normalized.endsWith(".local")
                || normalized.endsWith(".lan")
                || normalized.endsWith(".home")
                || normalized.endsWith(".internal")
                || normalized.endsWith(".test")
                || normalized.endsWith(".invalid")
                || normalized.endsWith(".onion")) return true;
        if (!normalized.contains(".") && !normalized.contains(":")) return true;
        if (isPrivateIpv4(normalized)) return true;
        if (normalized.contains(":")) {
            return normalized.startsWith("fc")
                    || normalized.startsWith("fd")
                    || normalized.startsWith("fe8")
                    || normalized.startsWith("fe9")
                    || normalized.startsWith("fea")
                    || normalized.startsWith("feb");
        }
        return false;
    }
}
