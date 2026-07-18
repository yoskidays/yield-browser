from pathlib import Path

DOWNLOAD = Path("app/src/main/java/com/yieldbrowser/app/YieldDownloadActivity.java")
MAIN = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")

text = DOWNLOAD.read_text()
anchor = """                case MINIMIZE_NORMAL:
                    videoFloatingPlayer = false;
                    saveSettings();
                    QuietToast.makeText(this,
                            "Minimize normal aktif",
                            QuietToast.LENGTH_SHORT).show();
                    break;"""
replacement = """                case MINIMIZE_NORMAL:
                    videoFloatingPlayer =
                            VideoOptimizationDialogController.toggledFloatingPlayer(
                                    videoFloatingPlayer);
                    saveSettings();
                    QuietToast.makeText(this,
                            videoFloatingPlayer
                                    ? "Floating player aktif"
                                    : "Minimize normal aktif",
                            QuietToast.LENGTH_SHORT).show();
                    break;"""
if text.count(anchor) != 1:
    raise SystemExit("Unexpected minimize-normal action count")
text = text.replace(anchor, replacement, 1)
if "videoFloatingPlayer = false;" in text[text.find("case MINIMIZE_NORMAL:"):text.find("case BACKGROUND_PLAY:")]:
    raise SystemExit("Minimize switch is still one-way")
if len(MAIN.read_text().splitlines()) > 3000:
    raise SystemExit("MainActivity target regressed")
DOWNLOAD.write_text(text)
