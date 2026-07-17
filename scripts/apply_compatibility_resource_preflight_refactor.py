from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "CompatibilityThirdPartyResourcePreflightPolicy.shouldClassify(" in text:
    print("Third-party resource preflight delegation already installed")
    raise SystemExit(0)

old_block = '''        try {
            if (!adBlock || !isHttpOrHttpsUrl(resourceUrl) || !isHttpOrHttpsUrl(pageUrl)) return false;
            if (isTrustedDownloadIntentUrl(resourceUrl) || isYoutubeCoreUrl(resourceUrl)) return false;
            boolean hardAd = isKnownPopupHost(resourceUrl) || isAdUrl(resourceUrl);
'''

new_block = '''        try {
            if (!CompatibilityThirdPartyResourcePreflightPolicy.shouldClassify(
                    adBlock,
                    isHttpOrHttpsUrl(resourceUrl),
                    isHttpOrHttpsUrl(pageUrl),
                    isTrustedDownloadIntentUrl(resourceUrl),
                    isYoutubeCoreUrl(resourceUrl))) return false;
            boolean hardAd = isKnownPopupHost(resourceUrl) || isAdUrl(resourceUrl);
'''

if text.count(old_block) != 1:
    raise SystemExit(
        f"Expected one third-party resource preflight block, found {text.count(old_block)}")

text = text.replace(old_block, new_block, 1)

if text.count("CompatibilityThirdPartyResourcePreflightPolicy.shouldClassify(") != 1:
    raise SystemExit("Expected exactly one third-party resource preflight delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity third-party resource preflight delegated")
