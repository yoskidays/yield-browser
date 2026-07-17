from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "BlankCompatibilityReportPolicy.isLikelyBlank(" in text:
    print("Blank compatibility report delegation already installed")
    raise SystemExit(0)

old_method = '''    private boolean isLikelyBlankCompatibilityReport(String report) {
        try {
            String raw = report == null ? "" : report.trim();
            if (raw.length() == 0) return false;
            String[] parts = raw.split("\\|", -1);
            if (parts.length < 7) return false;
            int textLen = Integer.parseInt(parts[0]);
            int nodeCount = Integer.parseInt(parts[1]);
            int mediaCount = Integer.parseInt(parts[2]);
            int linkCount = Integer.parseInt(parts[3]);
            int htmlLen = Integer.parseInt(parts[4]);
            int scrollHeight = Integer.parseInt(parts[5]);
            String readyState = parts[6];
            boolean complete = "complete".equalsIgnoreCase(readyState) || "interactive".equalsIgnoreCase(readyState);
            if (!complete) return false;
            boolean stronglyBlank = textLen <= 8 && mediaCount == 0 && nodeCount <= 18 && htmlLen <= 1600;
            boolean sparseBlank = textLen <= 20 && mediaCount == 0 && nodeCount <= 30 && linkCount <= 8 && htmlLen <= 3000;
            boolean viewportBlank = textLen <= 8 && mediaCount == 0 && nodeCount <= 40 && scrollHeight >= 300;
            return stronglyBlank || sparseBlank || viewportBlank;
        } catch (Exception e) {
            return false;
        }
    }
'''

new_method = '''    private boolean isLikelyBlankCompatibilityReport(String report) {
        return BlankCompatibilityReportPolicy.isLikelyBlank(report);
    }
'''

if text.count(old_method) != 1:
    raise SystemExit(
        f"Expected one legacy blank compatibility report method, found {text.count(old_method)}")

text = text.replace(old_method, new_method, 1)

if text.count("BlankCompatibilityReportPolicy.isLikelyBlank(") != 1:
    raise SystemExit("Expected exactly one blank compatibility report delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity blank compatibility report parsing delegated")
