package com.example.facelandmarklibrary;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.facelandmarklibrary.IrisDetector.FaceLandmarkerHelper;
import com.example.facelandmarklibrary.IrisDetector.OverlayView;
import com.example.facelandmarklibrary.ViewModel.DataShareViewModel;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.tasks.vision.core.RunningMode;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements FaceLandmarkerHelper.LandmarkerListener{

    private PreviewView previewView;
    private FaceLandmarkerHelper faceLandmarkerHelper;
    private ProcessCameraProvider cameraProvider = null;
    private int cameraFacing = CameraSelector.LENS_FACING_FRONT;
    private Preview preview = null;
    private ImageAnalysis imageAnalyzer = null;
    private ExecutorService backgroundExecutor;
    private DataShareViewModel viewModel;
    private Camera camera = null;
    OverlayView overlay;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = (PreviewView)findViewById(R.id.view_finder);
        faceLandmarkerHelper = new FaceLandmarkerHelper();
        viewModel = new ViewModelProvider(this).get(DataShareViewModel.class);
        backgroundExecutor = Executors.newSingleThreadExecutor();
        overlay = (OverlayView)findViewById(R.id.overlay);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            // You have the permission, you can proceed with camera-related tasks
            // For example, you can start your camera activity here
        } else {
            // Request camera permission
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        }

        faceLandmarkerHelper = new FaceLandmarkerHelper(
                viewModel.getCurrentMinFaceDetectionConfidence(),
                viewModel.getCurrentMinFaceTrackingConfidence(),
                viewModel.getCurrentMinFacePresenceConfidence(),
                viewModel.getCurrentMaxFaces(),
                viewModel.getCurrentDelegate(),
                RunningMode.LIVE_STREAM,
                MainActivity.this,
                this
        );

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can proceed with camera-related tasks
                //Toast.makeText(this, "Camera Permission Accepted", Toast.LENGTH_SHORT).show();

                previewView.post(new Runnable() {
                    @Override
                    public void run() {
                        // Set up the camera and its use cases
                        setUpCamera();
                    }
                });
            } else {
                // Permission denied, handle this situation (e.g., show a message to the user)
                Toast.makeText(this, "Camera Permission not Accepted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setUpCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    // CameraProvider
                    cameraProvider = cameraProviderFuture.get();

                    // Build and bind the camera use cases
                    bindCameraUseCases();
                } catch (Exception e) {
                    //   Log.e(TAG, "Error initializing cameraProvider", e);
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void bindCameraUseCases() {
        // CameraProvider
        ProcessCameraProvider cameraProvider = this.cameraProvider;
        if (cameraProvider == null) {
            throw new IllegalStateException("Camera initialization failed.");
        }

        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(cameraFacing).build();

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview = new Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(previewView.getDisplay().getRotation())
                .build();

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer = new ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setTargetRotation(previewView.getDisplay().getRotation())
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        // .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();

        // The analyzer can then be assigned to the instance
        imageAnalyzer.setAnalyzer(backgroundExecutor, new ImageAnalysis.Analyzer() {

            @Override
            public void analyze(@NonNull ImageProxy image) {
                detectFace(image);
            }
        });

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll();

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
            );

            // Attach the viewfinder's surface provider to preview use case
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
        } catch (Exception exc) {
            // Log.e(TAG, "Use case binding failed", exc);
        }
    }

    private void detectFace(ImageProxy imageProxy) {
        faceLandmarkerHelper.detectLiveStream(
                imageProxy, cameraFacing == CameraSelector.LENS_FACING_FRONT
        );
    }

    @Override
    public void onError(String error) {

    }

    @Override
    public void onError(String error, int errorCode) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResults(FaceLandmarkerHelper.ResultBundle resultBundle) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i("bitmapBuffer", "returnLivestreamResult = " );
                if (resultBundle != null) {

                    Log.i("bitmapBuffer", "returnLivestreamResult !=null " );
                    // Pass necessary information to OverlayView for drawing on the canvas
                    overlay.setResults(
                            resultBundle.getResult(),
                            resultBundle.getInputImageHeight(),
                            resultBundle.getInputImageWidth(),
                            RunningMode.LIVE_STREAM
                    );

                    // Force a redraw
                    overlay.invalidate();
                }
            }
        });

    }
}