package com.yieldbrowser.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

/** Owns video quality/speed dialogs and script evaluation while MainActivity owns settings state. */
final class VideoPlaybackController {
    interface QualityHandler {
        void select(String quality);
    }

    interface SpeedHandler {
        void select(float speed);
    }

    private final Activity activity;

    VideoPlaybackController(Activity activity) {
        this.activity = activity;
    }

    void detectQualities(WebView webView) {
        if (!isVisible(webView)) return;
        try {
            webView.evaluateJavascript(
                    "(function(){try{return (window.__yieldVideoQualities||[]).join(', ');}catch(e){return '';}})()",
                    value -> {
                        String result = value == null ? "" : value.replace("\"", "");
                        if (result.length() > 0) {
                            QuietToast.makeText(activity,
                                    "Kualitas terdeteksi: " + result + "p",
                                    QuietToast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception ignored) {
        }
    }

    void showQualityDialog(WebView webView,
                           String selectedQuality,
                           Runnable injectOptimization,
                           QualityHandler handler) {
        if (!isVisible(webView)) {
            QuietToast.makeText(activity, "Buka video dulu", QuietToast.LENGTH_SHORT).show();
            return;
        }
        if (injectOptimization != null) injectOptimization.run();
        detectQualities(webView);

        String[] labels = new String[]{"Auto", "240p", "360p", "480p", "720p"};
        int checked = 0;
        for (int index = 0; index < labels.length; index++) {
            if (labels[index].equals(selectedQuality)) {
                checked = index;
                break;
            }
        }
        new AlertDialog.Builder(activity)
                .setTitle("Kualitas video")
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    if (handler != null) handler.select(labels[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    void applyQuality(WebView webView,
                      String quality,
                      TextView qualityLabel,
                      Runnable saveSettings) {
        String selected = quality == null ? "Auto" : quality;
        if (qualityLabel != null) qualityLabel.setText(selected);
        if (saveSettings != null) saveSettings.run();

        if ("Auto".equals(selected)) {
            try {
                if (webView != null) {
                    webView.evaluateJavascript(
                            "(function(){try{if(window.videojs){var ps=window.videojs.getPlayers?window.videojs.getPlayers():{};for(var k in ps){var p=ps[k];if(p&&p.qualityLevels){var ql=p.qualityLevels();for(var i=0;i<ql.length;i++)ql[i].enabled=true;}}}return 'auto';}catch(e){return 'auto_err';}})()",
                            null);
                }
            } catch (Exception ignored) {
            }
            QuietToast.makeText(activity, "Kualitas video: Auto",
                    QuietToast.LENGTH_SHORT).show();
            return;
        }

        String target = selected.replace("p", "");
        String script = BrowserPageScripts.videoQuality(target);
        try {
            webView.evaluateJavascript(script, value -> {
                String result = value == null ? "" : value.replace("\"", "");
                if (result.contains("changed") || result.contains("clicked")
                        || result.contains("already")) {
                    QuietToast.makeText(activity, "Kualitas video: " + selected,
                            QuietToast.LENGTH_SHORT).show();
                } else if (result.contains("no_video")) {
                    QuietToast.makeText(activity, "Video belum ditemukan",
                            QuietToast.LENGTH_SHORT).show();
                } else {
                    QuietToast.makeText(activity,
                            "Kualitas " + selected + " tidak tersedia di player ini",
                            QuietToast.LENGTH_LONG).show();
                }
            });
        } catch (Exception error) {
            QuietToast.makeText(activity, "Gagal mengubah kualitas video",
                    QuietToast.LENGTH_SHORT).show();
        }
    }

    void showSpeedDialog(WebView webView, float currentSpeed, SpeedHandler handler) {
        if (!isVisible(webView)) {
            QuietToast.makeText(activity, "Buka video dulu", QuietToast.LENGTH_SHORT).show();
            return;
        }
        String[] speeds = new String[]{"0.5x", "1x", "1.25x", "1.5x", "2x"};
        float[] values = new float[]{0.5f, 1.0f, 1.25f, 1.5f, 2.0f};
        int checked = selectedSpeedIndex(values, currentSpeed);
        new AlertDialog.Builder(activity)
                .setTitle("Kecepatan video")
                .setSingleChoiceItems(speeds, checked, (dialog, which) -> {
                    if (handler != null) handler.select(values[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    void applySpeed(WebView webView,
                    float speed,
                    TextView speedLabel,
                    Runnable saveSettings) {
        String formatted = VideoUi.formatVideoSpeed(speed);
        if (speedLabel != null) speedLabel.setText(formatted);
        if (webView != null) {
            webView.loadUrl("javascript:(function(){var v=document.querySelector('video');if(v){v.playbackRate="
                    + speed + ";}})()");
        }
        if (saveSettings != null) saveSettings.run();
        QuietToast.makeText(activity, "Speed video: " + formatted,
                QuietToast.LENGTH_SHORT).show();
    }

    void control(WebView webView, String action, Runnable refreshState) {
        if (!isVisible(webView)) {
            QuietToast.makeText(activity, "Buka video dulu", QuietToast.LENGTH_SHORT).show();
            return;
        }
        String script = BrowserPageScripts.videoControl(action);
        try {
            webView.evaluateJavascript(script, value -> run(refreshState));
        } catch (Exception error) {
            webView.loadUrl("javascript:" + script);
            run(refreshState);
        }
    }

    static int selectedSpeedIndex(float[] values, float currentSpeed) {
        if (values == null || values.length == 0) return 0;
        int selected = Math.min(1, values.length - 1);
        for (int index = 0; index < values.length; index++) {
            if (Math.abs(values[index] - currentSpeed) < 0.01f) selected = index;
        }
        return selected;
    }

    private static boolean isVisible(WebView webView) {
        return webView != null && webView.getVisibility() == View.VISIBLE;
    }

    private static void run(Runnable action) {
        if (action != null) action.run();
    }
}
