from pathlib import Path

RULES = Path("app/src/main/java/com/yieldbrowser/app/ShieldUrlRules.java")
POLICY = Path("app/src/main/java/com/yieldbrowser/app/ShieldNavigationPolicy.java")
DOWNLOAD_POLICY = Path("app/src/main/java/com/yieldbrowser/app/DownloadUrlPolicy.java")
SCRIPT = Path("app/src/main/java/com/yieldbrowser/app/ShieldScriptPartOne.java")
MAIN = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")

rules = RULES.read_text()
policy = POLICY.read_text()
download_policy = DOWNLOAD_POLICY.read_text()
script = SCRIPT.read_text()

known_ad_anchor = '''    static final Pattern KNOWN_AD_HOST = Pattern.compile(
            "(?:^|\\.)(?:doubleclick\\.net|googlesyndication\\.com|googleadservices\\.com|adservice\\.google\\.[a-z.]+|onclickads\\.net|clickadu\\.com|popads\\.net|popcash\\.net|propellerads\\.com|adsterra\\.com|hilltopads\\.net|exoclick\\.com|trafficjunky\\.net|juicyads\\.com|admaven\\.com|realsrv\\.com|taboola\\.com|outbrain\\.com|mgid\\.com|revcontent\\.com|hotterydiseur\\.[a-z.]+|sewarsremeets\\.[a-z.]+|invest-tracing\\.[a-z.]+)$",
            Pattern.CASE_INSENSITIVE);

    static final Pattern CHEAP_AD_TLD'''
trusted_pattern = '''    static final Pattern KNOWN_AD_HOST = Pattern.compile(
            "(?:^|\\.)(?:doubleclick\\.net|googlesyndication\\.com|googleadservices\\.com|adservice\\.google\\.[a-z.]+|onclickads\\.net|clickadu\\.com|popads\\.net|popcash\\.net|propellerads\\.com|adsterra\\.com|hilltopads\\.net|exoclick\\.com|trafficjunky\\.net|juicyads\\.com|admaven\\.com|realsrv\\.com|taboola\\.com|outbrain\\.com|mgid\\.com|revcontent\\.com|hotterydiseur\\.[a-z.]+|sewarsremeets\\.[a-z.]+|invest-tracing\\.[a-z.]+)$",
            Pattern.CASE_INSENSITIVE);

    static final Pattern TRUSTED_DOWNLOAD_HOST = Pattern.compile(
            "(?:^|\\.)(?:drive\\.usercontent\\.google\\.com|drive\\.google\\.com|docs\\.google\\.com|googleusercontent\\.com|github\\.com|githubusercontent\\.com|sourceforge\\.net|mediafire\\.com|dropbox\\.com|dropboxusercontent\\.com|onedrive\\.live\\.com|1drv\\.ms|mega\\.nz|pixeldrain\\.com|gofile\\.io|archive\\.org)$",
            Pattern.CASE_INSENSITIVE);

    static final Pattern CHEAP_AD_TLD'''
if rules.count(known_ad_anchor) != 1:
    raise SystemExit("Unexpected KNOWN_AD_HOST anchor count")
rules = rules.replace(known_ad_anchor, trusted_pattern, 1)

method_anchor = '''    static boolean isKnownAdHost(String host) {
        return host != null && KNOWN_AD_HOST.matcher(host).find();
    }

    static boolean isCheapAdHost'''
method_replacement = '''    static boolean isKnownAdHost(String host) {
        return host != null && KNOWN_AD_HOST.matcher(host).find();
    }

    static boolean isTrustedDownloadHost(String host) {
        return host != null && TRUSTED_DOWNLOAD_HOST.matcher(host).find();
    }

    static boolean isCheapAdHost'''
if rules.count(method_anchor) != 1:
    raise SystemExit("Unexpected trusted-host method anchor count")
rules = rules.replace(method_anchor, method_replacement, 1)

safe_anchor = '''        return ShieldUrlRules.DOWNLOAD_TARGET_HINT
                .matcher(ShieldUrlRules.decodedLower(targetUrl)).find();'''
safe_replacement = '''        return ShieldUrlRules.isTrustedDownloadHost(targetHost);'''
if policy.count(safe_anchor) != 1:
    raise SystemExit("Unexpected safe-download cross-site anchor count")
policy = policy.replace(safe_anchor, safe_replacement, 1)

allow_start = download_policy.index("    static boolean isTrustedDownloadHostForAllow(String host) {")
allow_end = download_policy.index("\n    static boolean isSuspiciousAdHostForDownloadAllow", allow_start)
allow_replacement = '''    static boolean isTrustedDownloadHostForAllow(String host) {
        return ShieldUrlRules.isTrustedDownloadHost(host);
    }
'''
download_policy = download_policy[:allow_start] + allow_replacement + download_policy[allow_end:]

js_ad_host = '''                + "function adHost(h){return /(?:^|\\\\.)(?:doubleclick\\\\.net|googlesyndication\\\\.com|googleadservices\\\\.com|adservice\\\\.google\\\\.[a-z.]+|onclickads\\\\.net|clickadu\\\\.com|popads\\\\.net|popcash\\\\.net|propellerads\\\\.com|adsterra\\\\.com|hilltopads\\\\.net|exoclick\\\\.com|trafficjunky\\\\.net|juicyads\\\\.com|admaven\\\\.com|realsrv\\\\.com|taboola\\\\.com|outbrain\\\\.com|mgid\\\\.com|revcontent\\\\.com|hotterydiseur\\\\.[a-z.]+|sewarsremeets\\\\.[a-z.]+|invest-tracing\\\\.[a-z.]+)$/i.test(h||'');}"
                + "function cheap(h)'''
js_trusted = '''                + "function adHost(h){return /(?:^|\\\\.)(?:doubleclick\\\\.net|googlesyndication\\\\.com|googleadservices\\\\.com|adservice\\\\.google\\\\.[a-z.]+|onclickads\\\\.net|clickadu\\\\.com|popads\\\\.net|popcash\\\\.net|propellerads\\\\.com|adsterra\\\\.com|hilltopads\\\\.net|exoclick\\\\.com|trafficjunky\\\\.net|juicyads\\\\.com|admaven\\\\.com|realsrv\\\\.com|taboola\\\\.com|outbrain\\\\.com|mgid\\\\.com|revcontent\\\\.com|hotterydiseur\\\\.[a-z.]+|sewarsremeets\\\\.[a-z.]+|invest-tracing\\\\.[a-z.]+)$/i.test(h||'');}"
                + "function trustedDownloadHost(h){return /(?:^|\\\\.)(?:drive\\\\.usercontent\\\\.google\\\\.com|drive\\\\.google\\\\.com|docs\\\\.google\\\\.com|googleusercontent\\\\.com|github\\\\.com|githubusercontent\\\\.com|sourceforge\\\\.net|mediafire\\\\.com|dropbox\\\\.com|dropboxusercontent\\\\.com|onedrive\\\\.live\\\\.com|1drv\\\\.ms|mega\\\\.nz|pixeldrain\\\\.com|gofile\\\\.io|archive\\\\.org)$/i.test(h||'');}"
                + "function cheap(h)'''
if script.count(js_ad_host) != 1:
    raise SystemExit("Unexpected JS adHost anchor count")
script = script.replace(js_ad_host, js_trusted, 1)

js_safe_old = '''return downloadControl(node)&&downloadTarget(a);'''
js_safe_new = '''return downloadControl(node)&&trustedDownloadHost(h);'''
if script.count(js_safe_old) != 1:
    raise SystemExit("Unexpected JS safeDownloadNav anchor count")
script = script.replace(js_safe_old, js_safe_new, 1)

if "DOWNLOAD_TARGET_HINT\n                .matcher" in policy:
    raise SystemExit("Generic download-looking host allowance remains")
if "trustedDownloadHost(h)" not in script:
    raise SystemExit("Document-start trusted host boundary missing")
if len(MAIN.read_text().splitlines()) > 3000:
    raise SystemExit("MainActivity target regressed")

RULES.write_text(rules)
POLICY.write_text(policy)
DOWNLOAD_POLICY.write_text(download_policy)
SCRIPT.write_text(script)
