from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "UniversalCompatibilityCandidatePolicy.isCandidate(" in text:
    print("Universal compatibility candidate delegation already installed")
    raise SystemExit(0)

old_block = '''            String lowerUrl = url == null ? "" : url.toLowerCase(Locale.US);
            if (lowerUrl.contains(".pdf") || lowerUrl.contains("application/pdf")) return false;
            String host = hostOfUrl(url);
            if (host == null || host.length() == 0) return false;
            String h = host.toLowerCase(Locale.US);
            if (h.startsWith("www.")) h = h.substring(4);
            if (h.equals("youtube.com") || h.endsWith(".youtube.com") || h.equals("youtu.be")) return false;
            if (h.equals("google.com") || h.endsWith(".google.com") || h.equals("google.co.id") || h.endsWith(".google.co.id")) return false;
            if (h.equals("bing.com") || h.endsWith(".bing.com")) return false;
            if (h.equals("duckduckgo.com") || h.endsWith(".duckduckgo.com")) return false;
            if (h.equals("startpage.com") || h.endsWith(".startpage.com")) return false;
            return true;
'''

new_block = '''            return UniversalCompatibilityCandidatePolicy.isCandidate(
                    url, hostOfUrl(url));
'''

if text.count(old_block) != 1:
    raise SystemExit(
        f"Expected one legacy universal compatibility classification block, found {text.count(old_block)}")

text = text.replace(old_block, new_block, 1)

if text.count("UniversalCompatibilityCandidatePolicy.isCandidate(") != 1:
    raise SystemExit("Expected exactly one universal compatibility candidate delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity universal compatibility candidate classification delegated")
