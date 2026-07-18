from pathlib import Path
import textwrap

MAIN = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
FACTORY = Path("app/src/main/java/com/yieldbrowser/app/BrowserPageScripts.java")


def method_span(source, signature):
    start = source.find(signature)
    if start < 0:
        raise SystemExit(f"Missing method signature: {signature}")
    brace = source.find("{", start)
    depth = 0
    for index in range(brace, len(source)):
        char = source[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return start, index + 1, source[start:index + 1]
    raise SystemExit(f"Unbalanced method: {signature}")


def replace_method(source, signature, replacement):
    start, end, _ = method_span(source, signature)
    return source[:start] + replacement + source[end:]


def assignment(method, signature):
    start = method.find("        String js =")
    if start < 0:
        raise SystemExit(f"Missing JavaScript assignment: {signature}")
    end = method.find(";\n", start)
    if end < 0:
        raise SystemExit(f"Unterminated JavaScript assignment: {signature}")
    return start, end + 1, method[start:end + 1]


def factory_return(block):
    value = textwrap.dedent(block).replace("String js =", "return", 1)
    return textwrap.indent(value, "        ")


def replace_assignment(source, signature, expression):
    start, end, method = method_span(source, signature)
    assignment_start, assignment_end, _ = assignment(method, signature)
    method = (method[:assignment_start]
              + f"        String js = {expression};"
              + method[assignment_end:])
    return source[:start] + method + source[end:]


def build_factory(source):
    signatures = {
        "premium": "    private void injectPremiumAdBlock() {",
        "youtube": "    private void injectYouTubeSafeAdBlockV6() {",
        "stop": "    private void stopYouTubeAutoAssistantNow() {",
        "watcher": "    private void injectVideoPlaybackWatcher() {",
        "quality": "    private void setVideoQuality(String quality) {",
    }
    values = {}
    for key, signature in signatures.items():
        _, _, method = method_span(source, signature)
        _, _, values[key] = assignment(method, signature)
    return """package com.yieldbrowser.app;

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
""" % tuple(factory_return(values[key]) for key in
              ("premium", "youtube", "stop", "watcher", "quality"))


def apply():
    text = MAIN.read_text()
    FACTORY.write_text(build_factory(text))

    field_anchor = "    private View videoPlayPauseButton;"
    replacement = """    private View videoPlayPauseButton;
    private VideoPlaybackController videoPlaybackController;
    private DownloadQueueDialogController downloadQueueDialogController;"""
    if field_anchor not in text:
        raise SystemExit("Missing video playback field anchor")
    text = text.replace(field_anchor, replacement, 1)

    text = replace_method(text, "    private void detectVideoQualities() {", """    private void ensureVideoPlaybackController() {
        if (videoPlaybackController == null) {
            videoPlaybackController = new VideoPlaybackController(this);
        }
    }

    private void detectVideoQualities() {
        ensureVideoPlaybackController();
        videoPlaybackController.detectQualities(webView);
    }""")
    text = replace_method(text, "    private void showVideoQualityDialog() {", """    private void showVideoQualityDialog() {
        ensureVideoPlaybackController();
        videoPlaybackController.showQualityDialog(
                webView,
                selectedVideoQuality,
                MainActivity.this::injectVideoOptimizationIfNeeded,
                MainActivity.this::setVideoQuality);
    }""")
    text = replace_method(text, "    private void setVideoQuality(String quality) {", """    private void setVideoQuality(String quality) {
        selectedVideoQuality = quality == null ? "Auto" : quality;
        ensureVideoPlaybackController();
        videoPlaybackController.applyQuality(
                webView,
                selectedVideoQuality,
                videoQualityLabel,
                MainActivity.this::saveSettings);
    }""")
    text = replace_method(text, "    private void showVideoSpeedDialog() {", """    private void showVideoSpeedDialog() {
        ensureVideoPlaybackController();
        videoPlaybackController.showSpeedDialog(
                webView,
                videoSpeed,
                MainActivity.this::setVideoSpeed);
    }""")
    text = replace_method(text, "    private void setVideoSpeed(float speed) {", """    private void setVideoSpeed(float speed) {
        videoSpeed = speed;
        ensureVideoPlaybackController();
        videoPlaybackController.applySpeed(
                webView,
                videoSpeed,
                videoSpeedLabel,
                MainActivity.this::saveSettings);
    }""")
    text = replace_method(text, "    private void injectVideoPlaybackWatcher() {", """    private void injectVideoPlaybackWatcher() {
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
    text = replace_method(text, "    private void controlVideo(String action) {", """    private void controlVideo(String action) {
        injectVideoPlaybackWatcher();
        ensureVideoPlaybackController();
        videoPlaybackController.control(
                webView,
                action,
                MainActivity.this::refreshVideoPlayPauseButtonState);
    }""")

    text = replace_method(text, "    private void showDownloadQueueSettingsDialog() {", """    private void showDownloadQueueSettingsDialog() {
        if (downloadQueueDialogController == null) {
            downloadQueueDialogController = new DownloadQueueDialogController(this);
        }
        downloadQueueDialogController.show(
                new DownloadQueueDialogController.State(
                        downloadQueueEnabled, downloadMaxActive),
                () -> {
                    downloadQueueEnabled = !downloadQueueEnabled;
                    downloadQueuePaused = false;
                    saveSettings();
                    pumpDownloadQueue();
                    refreshDownloadPanel();
                },
                value -> {
                    downloadMaxActive = value;
                    downloadQueueEnabled = true;
                    downloadQueuePaused = false;
                    saveSettings();
                    pumpDownloadQueue();
                    refreshDownloadPanel();
                    QuietToast.makeText(this,
                            "Maksimal download aktif: " + value,
                            QuietToast.LENGTH_SHORT).show();
                },
                MainActivity.this::pauseAllDownloads,
                MainActivity.this::resumeAllDownloads,
                () -> {
                    activeDownloadSort = "Antrian";
                    renderDownloadList();
                    QuietToast.makeText(this,
                            "Tampilan diurutkan berdasarkan antrian",
                            QuietToast.LENGTH_SHORT).show();
                },
                MainActivity.this::getDownloadQueueSummary);
    }""")
    text = replace_method(text, "    private TextView queueChoiceChip(String label, int value, Runnable refresh) {", "")
    text = replace_method(text, "    private void updateQueueChoiceChip(TextView chip, int value) {", "")

    text = replace_assignment(text, "    private void injectPremiumAdBlock() {",
                              "BrowserPageScripts.premiumAdBlock(popupEnabled, clickEnabled, scriptEnabled)")
    text = replace_assignment(text, "    private void injectYouTubeSafeAdBlockV6() {",
                              "BrowserPageScripts.youtubeSafeAdBlock()")
    text = replace_assignment(text, "    private void stopYouTubeAutoAssistantNow() {",
                              "BrowserPageScripts.stopYouTubeAssistant()")

    line_count = len(text.splitlines())
    if line_count > 10000:
        raise SystemExit(f"MainActivity target not reached: {line_count} lines")
    MAIN.write_text(text)
    print(f"MainActivity final line count: {line_count}")
