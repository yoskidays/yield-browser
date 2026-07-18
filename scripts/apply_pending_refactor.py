from pathlib import Path

transform_path = Path(__file__).with_name("stage96_transform.py")
source = transform_path.read_text()
old = """    line_count = len(text.splitlines())
    if line_count > 10000:
        raise SystemExit(f\"MainActivity target not reached: {line_count} lines\")
    MAIN.write_text(text)
    print(f\"MainActivity final line count: {line_count}\")
"""
new = """    text = text.replace(
        \"formatVideoSpeed(videoSpeed)\",
        \"VideoUi.formatVideoSpeed(videoSpeed)\")
    text = replace_method(
        text,
        \"    private String formatVideoSpeed(float speed) {\",
        \"\")
    import re
    text = re.sub(r\"\\n[ \\t]*\\n(?:[ \\t]*\\n)+\", \"\\n\\n\", text)
    line_count = len(text.splitlines())
    if line_count > 10000:
        raise SystemExit(f\"MainActivity target not reached: {line_count} lines\")
    MAIN.write_text(text)
    print(f\"MainActivity final line count: {line_count}\")
"""
if old not in source:
    raise SystemExit("Missing Stage 96 line-count guard")
transform_path.write_text(source.replace(old, new, 1))

from stage96_transform import apply

apply()
