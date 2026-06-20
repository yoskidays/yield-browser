package com.yieldbrowser.app;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

/** Camera preview that emits the first QR/barcode value detected by ZXing. */
@SuppressWarnings("deprecation")
final class QrScannerView extends SurfaceView
        implements SurfaceHolder.Callback, Camera.PreviewCallback {

    interface ResultListener {
        void onResult(String text);
    }

    private Camera camera;
    private final MultiFormatReader reader = new MultiFormatReader();
    private final ResultListener listener;
    private boolean scanned;

    QrScannerView(Context context, ResultListener listener) {
        super(context);
        this.listener = listener;
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startCamera(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Preview dimensions are controlled by the camera driver.
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopCamera();
    }

    private void startCamera(SurfaceHolder holder) {
        try {
            camera = Camera.open();
            Camera.Parameters parameters = camera.getParameters();
            try {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                camera.setParameters(parameters);
            } catch (RuntimeException ignored) {
                // Some camera drivers do not support continuous focus.
            }
            camera.setDisplayOrientation(90);
            camera.setPreviewDisplay(holder);
            camera.setPreviewCallback(this);
            camera.startPreview();
        } catch (Exception error) {
            stopCamera();
            Toast.makeText(
                    getContext(),
                    "Kamera tidak bisa dibuka: " + error.getMessage(),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    void stopCamera() {
        Camera current = camera;
        camera = null;
        if (current == null) return;

        try {
            current.setPreviewCallback(null);
            current.stopPreview();
        } catch (RuntimeException ignored) {
            // Camera may already have stopped during lifecycle changes.
        }
        try {
            current.release();
        } catch (RuntimeException ignored) {
            // Ignore vendor-specific release failures.
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera sourceCamera) {
        if (scanned || sourceCamera == null || data == null) return;

        try {
            Camera.Size size = sourceCamera.getParameters().getPreviewSize();
            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                    data,
                    size.width,
                    size.height,
                    0,
                    0,
                    size.width,
                    size.height,
                    false
            );
            Result result = reader.decodeWithState(
                    new BinaryBitmap(new HybridBinarizer(source))
            );
            if (result != null && result.getText() != null) {
                scanned = true;
                stopCamera();
                listener.onResult(result.getText());
            }
        } catch (NotFoundException ignored) {
            // Normal path while the QR code is not yet centered.
        } catch (RuntimeException ignored) {
            // Ignore malformed frames from vendor camera implementations.
        } finally {
            reader.reset();
        }
    }
}
