# Stage 96 guarded extraction of browser page scripts and video playback UI.
from pathlib import Path
import textwrap

MAIN = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
FACTORY = Path("app/src/main/java/com/yieldbrowser/app/BrowserPageScripts.java")


def method_span(source: str, signature: str):
    start = source.find(signature)
    if start < 0:
        raise SystemExit(f"Missing method signature: {signature}")
    brace = source.find("{", start)
    if brace < 0:
        raise SystemExit(f"Missing method brace: {signature}")
    depth = 0
    end = -1
    for index in range(brace, len(source)):
        char = source[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                end = index + 1
                break
    if end < 0:
        raise SystemExit(f"Unbalanced method: {signature}")
    return start, end, source[start:end]


def js_assignment(method: str, signature: str):
    marker = "        String js ="
    start = method.find(marker)
    if start < 0:
        raise SystemExit(f"Missing JavaScript assignment: {signature}")
    end = method.find(";\n", start)
    if end < 0:
        raise SystemExit(f"Unterminated JavaScript assignment: {signature}")
    end += 1
    return start, end, method[start:end]


def factory_return(assignment: str) -> str:
    block = textwrap.dedent(assignment)
    if not block.startswith("String js ="):
        raise SystemExit("Unexpected JavaScript assignment format")
    block = block.replace("String js =", "return", 1)
    return textwrap.indent(block, "        ")


def replace_method(source: str, signature: str, replacement: str) -> str:
    start, end, _ = method_span(source, signature)
    return source[:start] + replacement + source[end:]


def replace_assignment(source: str, signature: str, expression: str) -> str:
    start, end, method = method_span(source, signature)
    assignment_start, assignment_end, _ = js_assignment(method, signature)
    updated = (method[:assignment_start]
               + f"        String js = {expression};"
               + method[assignment_end:])
    return source[:start] + updated + source[end:]


text = MAIN.read_text()
assignments = {}
for key, signature in {
    "premium": "    private void injectPremiumAdBlock() {",
    "youtube": "    private void injectYouTubeSafeAdBlockV6() {",
    "stop_youtube": "    private void stopYouTubeAutoAssistantNow() {",
    "watcher": "    private void injectVideoPlaybackWatcher() {",
    "quality": "    private void setVideoQuality(String quality) {",
}.items():
    _, _, method = method_span(text, signature)
    _, _, assignments[key] = js_assignment(method, signature)

factory = """package com.yieldbrowser.app;

/** Pure JavaScript builders extracted verbatim from MainActivity. */
final class BrowserPageScripts {
    private BrowserPageScripts() {
    }

    static String premiumAdBlock(String popupEnabled, String clickEnabled, String scriptEnabled) {
%s
    }

    static String youtubeSafeAdBlock() {
%s
    }

    static String stopYouTubeAssistant() {
%s
    }

    static String videoPlaybackWatcher(boolean youtubePage,
                                       boolean videoBufferBooster,
                                       boolean hlsSegmentPrefetch,
                                       boolean videoBackgroundPlay) {
%s
    }

    static String videoQuality(String target) {
%s
    }

    static String videoControl(String action) {
        if ("play".equals(action)) {
            return "(function(){var v=document.querySelector('video');if(v){v.play();'play';}else{'no_video';}})()";
        }
        if ("pause".equals(action)) {
            return "(function(){var v=document.querySelector('video');if(v){v.pause();'pause';}else{'no_video';}})()";
        }
        if ("toggle".equals(action)) {
            return "(function(){try{var v=document.querySelector('video');if(!v)return 'no_video';if(v.paused||v.ended){v.play();return 'play';}else{v.pause();return 'pause';}}catch(e){return 'error';}})()";
        }
        return "(function(){var v=document.querySelector('video');if(v){v.pause();try{v.currentTime=0;}catch(e){}'stop';}else{'no_video';}})()";
    }
}
""" % (
    factory_return(assignments["premium"]),
    factory_return(assignments["youtube"]),
    factory_return(assignments["stop_youtube"]),
    factory_return(assignments["watcher"]),
    factory_return(assignments["quality"]),
)
FACTORY.write_text(factory)

old_field = "    private View videoPlayPauseButton;"
new_field = """    private View videoPlayPauseButton;
    private VideoPlaybackController videoPlaybackController;"""
if old_field not in text:
    raise SystemExit("Missing video playback field anchor")
text = text.replace(old_field, new_field, 1)

video_dialogs = """    private void ensureVideoPlaybackController() {
        if (videoPlaybackController == null) {
            videoPlaybackController = new VideoPlaybackController(this);
        }
    }

    private void detectVideoQualities() {
        ensureVideoPlaybackController();
        videoPlaybackController.detectQualities(webView);
    }"""
text = replace_method(text, "    private void detectVideoQualities() {", video_dialogs)
text = replace_method(
    text,
    "    private void showVideoQualityDialog() {",
    """    private void showVideoQualityDialog() {
        ensureVideoPlaybackController();
        videoPlaybackController.showQualityDialog(
                webView,
                selectedVideoQuality,
                MainActivity.this::injectVideoOptimizationIfNeeded,
                MainActivity.this::setVideoQuality);
    }""")
text = replace_method(
    text,
    "    private void setVideoQuality(String quality) {",
    """    private void setVideoQuality(String quality) {
        selectedVideoQuality = quality == null ? "Auto" : quality;
        ensureVideoPlaybackController();
        videoPlaybackController.applyQuality(
                webView,
                selectedVideoQuality,
                videoQualityLabel,
                MainActivity.this::saveSettings);
    }""")
text = replace_method(
    text,
    "    private void showVideoSpeedDialog() {",
    """    private void showVideoSpeedDialog() {
        ensureVideoPlaybackController();
        videoPlaybackController.showSpeedDialog(
                webView,
                videoSpeed,
                MainActivity.this::setVideoSpeed);
    }""")
text = replace_method(
    text,
    "    private void setVideoSpeed(float speed) {",
    """    private void setVideoSpeed(float speed) {
        videoSpeed = speed;
        ensureVideoPlaybackController();
        videoPlaybackController.applySpeed(
                webView,
                videoSpeed,
                videoSpeedLabel,
                MainActivity.this::saveSettings);
    }""")
text = replace_method(
    text,
    "    private void injectVideoPlaybackWatcher() {",
    """    private void injectVideoPlaybackWatcher() {
        if (webView == null) return;
        if (isSiteCompatibilityModeActiveForUrl(getEffectiveCurrentUrl())) return;
        boolean youtubePage = isYouTubePlaybackUrl(getEffectiveCurrentUrl());
        String js = BrowserPageScripts.videoPlaybackWatcher(
                youtubePage,
                videoBufferBooster,
                hlsSegmentPrefetch,
                videoBackgroundPlay);
        try {
            runPageScript(js);
        } catch (Exception ignored) {
        }
    }""")
text = replace_method(
    text,
    "    private void controlVideo(String action) {",
    """    private void controlVideo(String action) {
        injectVideoPlaybackWatcher();
        ensureVideoPlaybackController();
        videoPlaybackController.control(
                webView,
                action,
                MainActivity.this::refreshVideoPlayPauseButtonState);
    }""")

text = replace_assignment(
    text,
    "    private void injectPremiumAdBlock() {",
    "BrowserPageScripts.premiumAdBlock(popupEnabled, clickEnabled, scriptEnabled)")
text = replace_assignment(
    text,
    "    private void injectYouTubeSafeAdBlockV6() {",
    "BrowserPageScripts.youtubeSafeAdBlock()")
text = replace_assignment(
    text,
    "    private void stopYouTubeAutoAssistantNow() {",
    "BrowserPageScripts.stopYouTubeAssistant()")

line_count = len(text.splitlines())
if line_count > 10000:
    raise SystemExit(f"MainActivity target not reached: {line_count} lines")
MAIN.write_text(text)
