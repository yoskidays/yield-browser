# Stage 94 guarded whitespace normalization.
from pathlib import Path
import re

PATH = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = PATH.read_text()
normalized = re.sub(r"\n[ \t]*\n(?:[ \t]*\n)+", "\n\n", text)
if normalized == text:
    raise SystemExit("No repeated blank-line groups found")
PATH.write_text(normalized)
