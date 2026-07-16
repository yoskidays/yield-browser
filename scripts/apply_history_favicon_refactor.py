from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "historyFaviconLoader.load(url, target, fallback);" in text:
    print("History favicon pipeline delegation already installed")
    raise SystemExit(0)

old_fields = """    // A bounded favicon pipeline prevents one thread/request per history row.
    private final ExecutorService faviconExecutor = Executors.newFixedThreadPool(3);
    private final LruCache<String, Bitmap> faviconMemoryCache = new LruCache<>(96);
"""
new_fields = """    // A bounded favicon pipeline prevents one thread/request per history row.
    private final HistoryFaviconLoader historyFaviconLoader =
            new HistoryFaviconLoader(mainHandler);
"""

if text.count(old_fields) != 1:
    raise SystemExit("Favicon field block changed; refusing unsafe refactor")
text = text.replace(old_fields, new_fields)

old_shutdown = "        try { faviconExecutor.shutdownNow(); } catch (Exception ignored) {}"
new_shutdown = "        historyFaviconLoader.shutdown();"
if text.count(old_shutdown) != 1:
    raise SystemExit("Favicon shutdown call changed; refusing unsafe refactor")
text = text.replace(old_shutdown, new_shutdown)

signature = "    private void loadFavicon(String url, ImageView target, TextView fallback) {"
start = text.find(signature)
if start < 0:
    raise SystemExit("loadFavicon method not found; refusing unsafe refactor")

brace = text.find("{", start)
depth = 0
in_string = False
escaped = False
quote = ""
end = -1
for index in range(brace, len(text)):
    char = text[index]
    if in_string:
        if escaped:
            escaped = False
        elif char == "\\":
            escaped = True
        elif char == quote:
            in_string = False
        continue
    if char in ('"', "'"):
        in_string = True
        quote = char
    elif char == "{":
        depth += 1
    elif char == "}":
        depth -= 1
        if depth == 0:
            end = index + 1
            break

if end < 0:
    raise SystemExit("loadFavicon method is unclosed; refusing unsafe refactor")

replacement = """    private void loadFavicon(String url, ImageView target, TextView fallback) {
        historyFaviconLoader.load(url, target, fallback);
    }"""
text = text[:start] + replacement + text[end:]

path.write_text(text, encoding="utf-8")
print("MainActivity favicon network, cache, and lifecycle delegated to HistoryFaviconLoader")
