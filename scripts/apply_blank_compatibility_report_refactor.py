from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "BlankCompatibilityReportPolicy.isLikelyBlank(" in text:
    print("Blank compatibility report delegation already installed")
    raise SystemExit(0)

start_marker = "    private boolean isLikelyBlankCompatibilityReport(String report) {\n"
next_marker = "\n    private void scheduleUniversalBlankCompatibilityRecovery(String url) {\n"

if text.count(start_marker) != 1:
    raise SystemExit(
        f"Expected one blank compatibility report method start, found {text.count(start_marker)}")
if text.count(next_marker) != 1:
    raise SystemExit(
        f"Expected one recovery method boundary, found {text.count(next_marker)}")

start = text.index(start_marker)
end = text.index(next_marker, start)
new_method = '''    private boolean isLikelyBlankCompatibilityReport(String report) {
        return BlankCompatibilityReportPolicy.isLikelyBlank(report);
    }
'''

text = text[:start] + new_method + text[end:]

if text.count("BlankCompatibilityReportPolicy.isLikelyBlank(") != 1:
    raise SystemExit("Expected exactly one blank compatibility report delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity blank compatibility report parsing delegated")
