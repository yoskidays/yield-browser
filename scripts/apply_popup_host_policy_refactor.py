from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "PopupHostPolicy.isKnown(" in text:
    print("Popup host policy delegation already installed")
    raise SystemExit(0)

old_block = '''    // v0.9.92: dipindah ke konstanta static (lihat catatan AD_URL_HOST_PATTERNS).
    private static final String[] POPUP_HOST_PATTERNS = new String[]{
            "hotterydiseur", "sewarsremeets", "sewarsremeet", "onclickads", "clickadu", "popads", "popcash",
            "popunder", "adsterra", "propellerads", "hilltopads", "exoclick", "trafficjunky", "juicyads",
            "admaven", "pushpush", "pushengage", "pushwoosh", "realsrv", "invest-tracing", "highperformanceformat",
            "highperformancedisplayformat", "xmladfeed", "rotator", "smartlink", "adnxs", "rubiconproject",
            "taboola", "outbrain", "mgid", "revcontent", "doubleclick", "googlesyndication", "googleadservices"
    };

    private boolean isKnownPopupHost(String url) {
        String host = normalizeHostForAdBlock(url);
        if (host.length() == 0) return false;
        if (isTrustedDownloadIntentUrl(url)) return false;

        for (String s : POPUP_HOST_PATTERNS) {
            if (host.contains(s)) return true;
        }

        if (host.endsWith(".cfd") || host.endsWith(".click") || host.endsWith(".cam") || host.endsWith(".monster")
                || host.endsWith(".quest") || host.endsWith(".buzz") || host.endsWith(".icu") || host.endsWith(".cyou")) {
            return true;
        }

        String u = url.toLowerCase(Locale.US);
        return u.contains("/popunder") || u.contains("/popup") || u.contains("/redirect")
                || u.contains("/push/") || u.contains("?utm_source=ad") || u.contains("&ad_id=")
                || u.contains("?ad_id=") || u.contains("/prebid") || u.contains("/vast") || u.contains("/vpaid");
    }
'''

new_block = '''    private boolean isKnownPopupHost(String url) {
        return PopupHostPolicy.isKnown(
                url,
                normalizeHostForAdBlock(url),
                MainActivity.this::isTrustedDownloadIntentUrl);
    }
'''

if text.count(old_block) != 1:
    raise SystemExit(
        f"Expected one legacy popup host block, found {text.count(old_block)}")

text = text.replace(old_block, new_block, 1)

if text.count("PopupHostPolicy.isKnown(") != 1:
    raise SystemExit("Expected exactly one popup host policy delegation")
if "POPUP_HOST_PATTERNS" in text:
    raise SystemExit("Legacy popup host patterns remain in MainActivity")

path.write_text(text, encoding="utf-8")
print("MainActivity popup host classification delegated")
