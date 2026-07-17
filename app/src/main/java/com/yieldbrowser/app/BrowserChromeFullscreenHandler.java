package com.yieldbrowser.app;

import android.content.pm.ActivityInfo;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;

/** Applies the mechanical Android fullscreen lifecycle for WebChrome custom views. */
final class BrowserChromeFullscreenHandler {
    interface ShowStateWriter {
        void set(View fullscreenView,
                 WebChromeClient.CustomViewCallback callback,
                 int originalSystemUiVisibility);
    }

    interface OrientationSetter {
        void set(int orientation);
    }

    private BrowserChromeFullscreenHandler() {
    }

    static boolean show(View existingFullscreenView,
                        View incomingView,
                        WebChromeClient.CustomViewCallback incomingCallback,
                        Window window,
                        View topBar,
                        View bottomBar,
                        ShowStateWriter stateWriter,
                        OrientationSetter orientationSetter,
                        Runnable moveVideoControls,
                        Runnable updateVideoModeToggle,
                        Runnable showVideoControls) {
        BrowserChromeFullscreenPolicy.ShowAction action =
                BrowserChromeFullscreenPolicy.showAction(existingFullscreenView != null);
        if (action == BrowserChromeFullscreenPolicy.ShowAction.REJECT_DUPLICATE) {
            if (incomingCallback != null) incomingCallback.onCustomViewHidden();
            return false;
        }
        if (incomingView == null || window == null) return false;

        int originalVisibility = window.getDecorView().getSystemUiVisibility();
        if (stateWriter != null) {
            stateWriter.set(incomingView, incomingCallback, originalVisibility);
        }

        FrameLayout decor = (FrameLayout) window.getDecorView();
        decor.addView(incomingView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        if (orientationSetter != null) {
            orientationSetter.set(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }

        if (topBar != null) topBar.setVisibility(View.GONE);
        if (bottomBar != null) bottomBar.setVisibility(View.GONE);
        run(moveVideoControls);
        run(updateVideoModeToggle);
        run(showVideoControls);
        return true;
    }

    static boolean hide(View fullscreenView,
                        WebChromeClient.CustomViewCallback fullscreenCallback,
                        Window window,
                        Runnable clearFullscreenView,
                        Runnable clearFullscreenCallback,
                        Runnable restoreAfterFullscreen,
                        Runnable updateVideoModeToggle) {
        BrowserChromeFullscreenPolicy.HideAction action =
                BrowserChromeFullscreenPolicy.hideAction(fullscreenView != null);
        if (action == BrowserChromeFullscreenPolicy.HideAction.NO_OP) return false;
        if (window == null) return false;

        FrameLayout decor = (FrameLayout) window.getDecorView();
        decor.removeView(fullscreenView);
        run(clearFullscreenView);

        if (fullscreenCallback != null) {
            fullscreenCallback.onCustomViewHidden();
            run(clearFullscreenCallback);
        }

        run(restoreAfterFullscreen);
        run(updateVideoModeToggle);
        return true;
    }

    private static void run(Runnable runnable) {
        if (runnable != null) runnable.run();
    }
}
