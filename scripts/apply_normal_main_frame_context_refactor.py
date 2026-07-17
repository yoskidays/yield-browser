from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "NormalMainFrameContextPolicy.allowUnknownCrossSite(" in text:
    print("Normal main-frame context delegation already installed")
    raise SystemExit(0)

old_return = '''        return hasGesture && !isKnownPopupHost(targetUrl) && !isAdUrl(targetUrl) && !isLikelyAdClickUrl(targetUrl);
'''

new_return = '''        return NormalMainFrameContextPolicy.allowUnknownCrossSite(
                hasGesture,
                isKnownPopupHost(targetUrl),
                isAdUrl(targetUrl),
                isLikelyAdClickUrl(targetUrl));
'''

if text.count(old_return) != 1:
    raise SystemExit(
        f"Expected one normal main-frame final decision, found {text.count(old_return)}")

text = text.replace(old_return, new_return, 1)

if text.count("NormalMainFrameContextPolicy.allowUnknownCrossSite(") != 1:
    raise SystemExit("Expected exactly one normal main-frame context delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity normal main-frame context delegated")
