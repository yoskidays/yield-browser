from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "LocalHostPolicy.isPrivateIpv4(" in text:
    print("Local host policy delegation already installed")
    raise SystemExit(0)

old_ipv4 = '''    private boolean isPrivateIpv4Host(String host) {
        try {
            String[] parts = host.split("\\\\.");
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
'''
new_ipv4 = '''    private boolean isPrivateIpv4Host(String host) {
        return LocalHostPolicy.isPrivateIpv4(host);
    }
'''

old_local = '''    private boolean isLocalOrPrivateHost(String host) {
        if (host == null || host.trim().length() == 0) return true;
        String h = host.trim().toLowerCase(Locale.US);
        if (h.startsWith("[") && h.endsWith("]")) h = h.substring(1, h.length() - 1);
        if (h.equals("localhost") || h.equals("::1") || h.equals("0:0:0:0:0:0:0:1")) return true;
        if (h.endsWith(".localhost") || h.endsWith(".local") || h.endsWith(".lan")
                || h.endsWith(".home") || h.endsWith(".internal") || h.endsWith(".test")
                || h.endsWith(".invalid") || h.endsWith(".onion")) return true;
        if (!h.contains(".") && !h.contains(":")) return true;
        if (isPrivateIpv4Host(h)) return true;
        if (h.contains(":")) {
            return h.startsWith("fc") || h.startsWith("fd") || h.startsWith("fe8")
                    || h.startsWith("fe9") || h.startsWith("fea") || h.startsWith("feb");
        }
        return false;
    }
'''
new_local = '''    private boolean isLocalOrPrivateHost(String host) {
        return LocalHostPolicy.isLocalOrPrivate(host);
    }
'''

if text.count(old_ipv4) != 1:
    raise SystemExit(f"Expected one legacy private IPv4 method, found {text.count(old_ipv4)}")
if text.count(old_local) != 1:
    raise SystemExit(f"Expected one legacy local/private host method, found {text.count(old_local)}")

text = text.replace(old_ipv4, new_ipv4, 1)
text = text.replace(old_local, new_local, 1)

if text.count("LocalHostPolicy.isPrivateIpv4(") != 1:
    raise SystemExit("Expected one private IPv4 delegation")
if text.count("LocalHostPolicy.isLocalOrPrivate(") != 1:
    raise SystemExit("Expected one local/private host delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity local host classification delegated")
