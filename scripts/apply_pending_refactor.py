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

cheap_pattern_marker = "    static final Pattern CHEAP_AD_TLD = Pattern.compile("
trusted_pattern = r'''    static final Pattern TRUSTED_DOWNLOAD_HOST = Pattern.compile(
            "(?:^|\\.)(?:drive\\.usercontent\\.google\\.com|drive\\.google\\.com|docs\\.google\\.com|googleusercontent\\.com|github\\.com|githubusercontent\\.com|sourceforge\\.net|mediafire\\.com|dropbox\\.com|dropboxusercontent\\.com|onedrive\\.live\\.com|1drv\\.ms|mega\\.nz|pixeldrain\\.com|gofile\\.io|archive\\.org)$",
            Pattern.CASE_INSENSITIVE);

'''
if "TRUSTED_DOWNLOAD_HOST" in rules:
    raise SystemExit("Trusted download host pattern already exists")
if rules.count(cheap_pattern_marker) != 1:
    raise SystemExit("Unexpected CHEAP_AD_TLD marker count")
rules = rules.replace(cheap_pattern_marker, trusted_pattern + cheap_pattern_marker, 1)

cheap_method_marker = "    static boolean isCheapAdHost(String host) {"
trusted_method = '''    static boolean isTrustedDownloadHost(String host) {
        return host != null && TRUSTED_DOWNLOAD_HOST.matcher(host).find();
    }

'''
if rules.count(cheap_method_marker) != 1:
    raise SystemExit("Unexpected isCheapAdHost marker count")
rules = rules.replace(cheap_method_marker, trusted_method + cheap_method_marker, 1)

safe_anchor = '''        return ShieldUrlRules.DOWNLOAD_TARGET_HINT
                .matcher(ShieldUrlRules.decodedLower(targetUrl)).find();'''
if policy.count(safe_anchor) != 1:
    raise SystemExit("Unexpected safe-download cross-site anchor count")
policy = policy.replace(
        safe_anchor,
        "        return ShieldUrlRules.isTrustedDownloadHost(targetHost);",
        1)

allow_start_marker = "    static boolean isTrustedDownloadHostForAllow(String host) {"
allow_end_marker = "\n    static boolean isSuspiciousAdHostForDownloadAllow"
allow_start = download_policy.find(allow_start_marker)
allow_end = download_policy.find(allow_end_marker, allow_start)
if allow_start < 0 or allow_end < 0:
    raise SystemExit("Trusted download allow method boundary missing")
download_policy = (
        download_policy[:allow_start]
        + '''    static boolean isTrustedDownloadHostForAllow(String host) {
        return ShieldUrlRules.isTrustedDownloadHost(host);
    }
'''
        + download_policy[allow_end:])

js_cheap_marker = '                + "function cheap(h){return /'
trusted_js = r'''                + "function trustedDownloadHost(h){return /(?:^|\\.)(?:drive\\.usercontent\\.google\\.com|drive\\.google\\.com|docs\\.google\\.com|googleusercontent\\.com|github\\.com|githubusercontent\\.com|sourceforge\\.net|mediafire\\.com|dropbox\\.com|dropboxusercontent\\.com|onedrive\\.live\\.com|1drv\\.ms|mega\\.nz|pixeldrain\\.com|gofile\\.io|archive\\.org)$/i.test(h||'');}"
'''
if script.count(js_cheap_marker) != 1:
    raise SystemExit("Unexpected JS cheap-host marker count")
script = script.replace(js_cheap_marker, trusted_js + js_cheap_marker, 1)

js_safe_old = "return downloadControl(node)&&downloadTarget(a);"
js_safe_new = "return downloadControl(node)&&trustedDownloadHost(h);"
if script.count(js_safe_old) != 1:
    raise SystemExit("Unexpected JS safeDownloadNav anchor count")
script = script.replace(js_safe_old, js_safe_new, 1)

if "DOWNLOAD_TARGET_HINT\n                .matcher" in policy:
    raise SystemExit("Generic download-looking host allowance remains")
if rules.count("isTrustedDownloadHost(String host)") != 1:
    raise SystemExit("Native trusted-host policy missing")
if script.count("function trustedDownloadHost(h)") != 1:
    raise SystemExit("Document-start trusted-host policy missing")
if len(MAIN.read_text().splitlines()) > 3000:
    raise SystemExit("MainActivity target regressed")

RULES.write_text(rules)
POLICY.write_text(policy)
DOWNLOAD_POLICY.write_text(download_policy)
SCRIPT.write_text(script)
