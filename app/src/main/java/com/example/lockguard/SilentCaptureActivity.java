package com.example.lockguard;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SilentCaptureActivity extends AppCompatActivity {

    public static final String EXTRA_IS_TEST = "extra_is_test";
    public static final String ACTION_CAPTURE_SAVED = "com.example.lockguard.CAPTURE_SAVED";

    private ExecutorService cameraExecutor;
    private boolean captureTriggered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        );

        cameraExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            finish();
            return;
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing()) {
                finish();
            }
        }, 7000);

        startFrontCameraCapture();
    }

    private void startFrontCameraCapture() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                ImageCapture imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector selector = selectBestCamera(cameraProvider);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, selector, imageCapture);

                new Handler(Looper.getMainLooper()).postDelayed(() -> takePictureNow(imageCapture), 450);
            } catch (ExecutionException | InterruptedException | IllegalArgumentException e) {
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private CameraSelector selectBestCamera(ProcessCameraProvider cameraProvider) {
        try {
            if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                return CameraSelector.DEFAULT_FRONT_CAMERA;
            }
        } catch (CameraInfoUnavailableException ignored) {
        }
        return CameraSelector.DEFAULT_BACK_CAMERA;
    }

    private void takePictureNow(ImageCapture imageCapture) {
        if (captureTriggered || isFinishing()) {
            return;
        }
        captureTriggered = true;

        boolean isTest = getIntent() != null && getIntent().getBooleanExtra(EXTRA_IS_TEST, false);
        File outputFile = CaptureStorage.createNewCaptureFile(this, isTest ? "TEST" : "INTRUDER");

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(outputFile).build();

        imageCapture.takePicture(outputOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Intent broadcast = new Intent(ACTION_CAPTURE_SAVED);
                broadcast.setPackage(getPackageName());
                sendBroadcast(broadcast);
                runOnUiThread(() -> finish());
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() -> finish());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
