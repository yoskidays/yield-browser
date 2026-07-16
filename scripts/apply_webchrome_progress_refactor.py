from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "BrowserChromeProgressHandler.handle(" in text:
    print("WebChrome progress delegation already installed")
    raise SystemExit(0)

old = '''            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (view != webView) return;
                boolean homeVisible = homeScroll != null && homeScroll.getVisibility() == View.VISIBLE;
                if (homeVisible || view.getVisibility() != View.VISIBLE) {
                    progressBar.setProgress(0);
                    progressBar.setVisibility(View.GONE);
                    return;
                }
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }
'''

new = '''            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                BrowserChromeProgressHandler.handle(
                        webView,
                        view,
                        newProgress,
                        homeScroll != null && homeScroll.getVisibility() == View.VISIBLE,
                        progressBar);
            }
'''

if text.count(old) != 1:
    raise SystemExit(f"Expected one legacy WebChrome progress block, found {text.count(old)}")
text = text.replace(old, new, 1)
if text.count("BrowserChromeProgressHandler.handle(") != 1:
    raise SystemExit("Expected exactly one WebChrome progress delegation")
path.write_text(text, encoding="utf-8")
print("MainActivity WebChrome progress delegated")
