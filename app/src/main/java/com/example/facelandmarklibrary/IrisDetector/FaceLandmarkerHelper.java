package com.example.facelandmarklibrary.IrisDetector;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.camera.core.ImageProxy;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.core.Delegate;
import com.google.mediapipe.tasks.core.ErrorListener;
import com.google.mediapipe.tasks.core.OutputHandler;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;

public class FaceLandmarkerHelper {
    public static final float DEFAULT_FACE_DETECTION_CONFIDENCE = 0.7f;
    public static final float DEFAULT_FACE_TRACKING_CONFIDENCE = 0.5f;
    public static final float DEFAULT_FACE_PRESENCE_CONFIDENCE = 0.5f;
    public static final int DEFAULT_NUM_FACES = 1;
    public static final int DELEGATE_CPU = 0;
    public static final int DELEGATE_GPU = 1;
    public static final int DELEGATE_VNN = 2;
    public static final String MP_FACE_LANDMARKER_TASK = "face_landmarker.task";
    public float minFaceDetectionConfidence;
    public float minFaceTrackingConfidence;
    public float minFacePresenceConfidence;
    public int maxNumFaces;
    public int currentDelegate;
    public RunningMode runningMode;
    public Context context;
    public LandmarkerListener faceLandmarkerHelperListener;

    public FaceLandmarkerHelper(
            float minFaceDetectionConfidence,
            float minFaceTrackingConfidence,
            float minFacePresenceConfidence,
            int maxNumFaces,
            int currentDelegate,
            RunningMode runningMode,
            Context context,
            LandmarkerListener faceLandmarkerHelperListener) {
        this.minFaceDetectionConfidence = minFaceDetectionConfidence;
        this.minFaceTrackingConfidence = minFaceTrackingConfidence;
        this.minFacePresenceConfidence = minFacePresenceConfidence;
        this.maxNumFaces = maxNumFaces;
        this.currentDelegate = currentDelegate;
        this.runningMode = runningMode;
        this.context = context;
        this.faceLandmarkerHelperListener = faceLandmarkerHelperListener;
        setupFaceLandmarker();
    }
    private FaceLandmarker faceLandmarker = null;

    public FaceLandmarkerHelper() {

    }

    @SuppressLint("UnsafeOptInUsageError")
    public void detectLiveStream(ImageProxy imageProxy, boolean isFrontCamera) {
        if (runningMode != RunningMode.LIVE_STREAM) {
            throw new IllegalArgumentException("Attempting to call detectLiveStream" +
                    " while not using RunningMode.LIVE_STREAM");
        }









        Bitmap bitmapBuffer = toBitmap(imageProxy.getImage());




        long frameTime = SystemClock.uptimeMillis();
//
//        // Copy out RGB bits from the frame to a bitmap buffer
//        ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer();
//
        int width = imageProxy.getWidth();
        int height = imageProxy.getHeight();
//        Bitmap bitmapBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//      //   buffer = new ByteBuffer(bitmapBuffer.getByteCount());
//        Log.i("bitmapBuffer", "Bitmap size = " + bitmapBuffer.getByteCount());
//        Log.i("bitmapBuffer", "Buffer size = " + buffer.capacity());
//
//        bitmapBuffer.copyPixelsFromBuffer(buffer);


        imageProxy.close();

        Matrix matrix = new Matrix();
        // Rotate the frame received from the camera to be in the same direction as it'll be shown
        matrix.postRotate(imageProxy.getImageInfo().getRotationDegrees());

        // Flip image if user uses the front camera
        if (isFrontCamera) {
            matrix.postScale(-1f, 1f, width / 2f, height / 2f);
        }

        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmapBuffer, 0, 0, width, height, matrix, true);

        // Convert the input Bitmap object to an InputImage object to run inference
        MPImage inputImage = new BitmapImageBuilder(rotatedBitmap).build();

        detectAsync(inputImage, frameTime);
    }

    private Bitmap toBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }
    public void clearFaceLandmarker() {
        if (faceLandmarker != null) {
            faceLandmarker.close();
            faceLandmarker = null;
        }
    }

    public boolean isClose() {
        return faceLandmarker == null;
    }

    public void setupFaceLandmarker() {
        // Set general face detector options
        BaseOptions.Builder faceDetectorOptionsBuilder = BaseOptions.builder();


        if (currentDelegate == DELEGATE_CPU) {
            faceDetectorOptionsBuilder.setDelegate(Delegate.CPU);
        } else if (currentDelegate == DELEGATE_GPU) {
            faceDetectorOptionsBuilder.setDelegate(Delegate.GPU);
        }
        faceDetectorOptionsBuilder.setModelAssetPath(MP_FACE_LANDMARKER_TASK);


        try {
            BaseOptions faceDetectorOptions = faceDetectorOptionsBuilder.build();
            FaceLandmarker.FaceLandmarkerOptions.Builder optionBuilder = FaceLandmarker.FaceLandmarkerOptions.builder()
                    .setBaseOptions(faceDetectorOptions)
                    .setMinFaceDetectionConfidence(minFaceDetectionConfidence)
                    .setMinTrackingConfidence(minFaceTrackingConfidence)
                    .setMinFacePresenceConfidence(minFacePresenceConfidence)
                    .setNumFaces(maxNumFaces)
                    .setRunningMode(runningMode);
            if (runningMode == RunningMode.LIVE_STREAM) {
                optionBuilder
                        .setResultListener(new OutputHandler.ResultListener<FaceLandmarkerResult, MPImage>() {
                            @Override
                            public void run(FaceLandmarkerResult result, MPImage input) {
                                returnLivestreamResult(result,input);

                            }
                        })
                        .setErrorListener(new ErrorListener() {
                            @Override
                            public void onError(RuntimeException e) {
                                returnLivestreamError(e);
                            }
                        });
            }
            FaceLandmarker.FaceLandmarkerOptions option = optionBuilder.build();
            faceLandmarker= FaceLandmarker.createFromOptions(context,option);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
        }

    }
    // Run face landmark using MediaPipe Face Landmarker API
    @VisibleForTesting
    public void detectAsync(MPImage mlKitImage, long frameTime) {
        faceLandmarker.detectAsync(mlKitImage, frameTime);
    }

    private void returnLivestreamResult(FaceLandmarkerResult result, MPImage input) {
        if (result.faceLandmarks().size() > 0) {
            long finishTimeMs = SystemClock.uptimeMillis();
            long inferenceTime = finishTimeMs - result.timestampMs();

            ResultBundle resultBundle = new ResultBundle(
                    result,
                    inferenceTime,
                    input.getHeight(),
                    input.getWidth()
            );

            if (faceLandmarkerHelperListener != null) {
                faceLandmarkerHelperListener.onResults(resultBundle);
            }
        } else {
            if (faceLandmarkerHelperListener != null) {
                faceLandmarkerHelperListener.onEmpty();
            }
        }
    }

    private void returnLivestreamError(Exception error) {
        String errorMessage = (error != null && error.getMessage() != null) ? error.getMessage() : "An unknown error has occurred";
        if (faceLandmarkerHelperListener != null) {
            faceLandmarkerHelperListener.onError(errorMessage);
        }
    }

    public interface LandmarkerListener {
        int OTHER_ERROR = 0;

        void onError(String error);

        void onError(String error, int errorCode);

        void onResults(ResultBundle resultBundle);

        default void onEmpty() {
        }
    }
    public class ResultBundle {
        private FaceLandmarkerResult result;
        private long inferenceTime;
        private int inputImageHeight;
        private int inputImageWidth;

        public ResultBundle(FaceLandmarkerResult result, long inferenceTime, int inputImageHeight, int inputImageWidth) {
            this.result = result;
            this.inferenceTime = inferenceTime;
            this.inputImageHeight = inputImageHeight;
            this.inputImageWidth = inputImageWidth;
        }

        public FaceLandmarkerResult getResult() {
            return result;
        }

        public long getInferenceTime() {
            return inferenceTime;
        }

        public int getInputImageHeight() {
            return inputImageHeight;
        }

        public int getInputImageWidth() {
            return inputImageWidth;
        }
    }

    public class VideoResultBundle {
        private List<FaceLandmarkerResult> results;
        private long inferenceTime;
        private int inputImageHeight;
        private int inputImageWidth;

        public VideoResultBundle(List<FaceLandmarkerResult> results, long inferenceTime, int inputImageHeight, int inputImageWidth) {
            this.results = results;
            this.inferenceTime = inferenceTime;
            this.inputImageHeight = inputImageHeight;
            this.inputImageWidth = inputImageWidth;
        }

        public List<FaceLandmarkerResult> getResults() {
            return results;
        }

        public long getInferenceTime() {
            return inferenceTime;
        }

        public int getInputImageHeight() {
            return inputImageHeight;
        }

        public int getInputImageWidth() {
            return inputImageWidth;
        }
    }
}
