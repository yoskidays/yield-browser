from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "PageScriptExecutionPolicy.plan(" in text:
    print("Page script execution delegation already installed")
    raise SystemExit(0)

old_method = '''    private void runPageScript(String js) {
        if (webView == null || js == null || js.length() == 0) return;
        try {
            String code = js;
            if (code.startsWith("javascript:")) code = code.substring("javascript:".length());
            if (Build.VERSION.SDK_INT >= 19) webView.evaluateJavascript(code, null);
            else webView.loadUrl("javascript:" + code);
        } catch (Exception ignored) {
        }
    }
'''

new_method = '''    private void runPageScript(String js) {
        if (webView == null) return;
        PageScriptExecutionPolicy.Plan plan = PageScriptExecutionPolicy.plan(
                js, Build.VERSION.SDK_INT >= 19);
        if (!plan.execute) return;
        try {
            if (plan.evaluateJavascript) webView.evaluateJavascript(plan.payload, null);
            else webView.loadUrl(plan.payload);
        } catch (Exception ignored) {
        }
    }
'''

if text.count(old_method) != 1:
    raise SystemExit(
        f"Expected one legacy page script method, found {text.count(old_method)}")

text = text.replace(old_method, new_method, 1)

if text.count("PageScriptExecutionPolicy.plan(") != 1:
    raise SystemExit("Expected exactly one page script execution delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity page script execution delegated")
