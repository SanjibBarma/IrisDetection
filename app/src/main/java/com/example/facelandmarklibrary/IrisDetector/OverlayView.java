package com.example.facelandmarklibrary.IrisDetector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.core.content.ContextCompat;


import com.example.facelandmarklibrary.R;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult;

import java.util.Arrays;
import java.util.List;


public class OverlayView  extends View {
    private FaceLandmarkerResult results;
    private Paint linePaint = new Paint();
    private Paint pointPaint = new Paint();

    private static final float LANDMARK_STROKE_WIDTH = 8f;
    private static final String TAG = "Face Landmarker Overlay";

    private float scaleFactor = 1f;
    private int imageWidth = 1;
    private int imageHeight = 1;
    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }


    public void clear() {
        results = null;
        linePaint.reset();
        pointPaint.reset();
        invalidate();
        initPaints();
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (results == null || results.faceLandmarks().isEmpty()) {
            clear();
            return;
        }

        List<List<NormalizedLandmark>> faceLandmarks = results.faceLandmarks();
//        if (results.faceBlendshapes().isPresent()) {
//            List<List<Category>> faceBlendshapes = results.faceBlendshapes().get();
//            for (List<Category> landmarks : faceBlendshapes) {
//                for (Category landmark : landmarks) {
//                    Log.e("faceLandmarkerResult", landmark.displayName() + " " + landmark.score());
//                }
//            }
//        }
//        Log.e("faceLandmarkerResult", " leftEyelidLandmarks " +faceLandmarks.size());
//        List<NormalizedLandmark> leftEyelidLandmarks= new ArrayList<>();
//        List<NormalizedLandmark> rightEyelidLandmarks= new ArrayList<>();
        for (List<NormalizedLandmark> landmarks : faceLandmarks) {

//                    = List.of(
//                    landmarks.get(159),
//                    landmarks.get(145)
//            );

//            for (NormalizedLandmark landmark : landmarks) {
//                if(landmark.equals(landmarks.get(159))||landmark.equals(landmarks.get(145))){
//                    leftEyelidLandmarks.add(landmark);
//
//                }
//                if(landmark.equals(landmarks.get(386))||landmark.equals(landmarks.get(374))){
//                    rightEyelidLandmarks.add(landmark);
//
//                }
//            }

            List<NormalizedLandmark> leftEyelidLandmarks = Arrays.asList(
                    landmarks.get(159),
                    landmarks.get(145)
            );
            List<NormalizedLandmark> rightEyelidLandmarks = Arrays.asList(
                    landmarks.get(386),
                    landmarks.get(374)
            );

//            Log.e("faceLandmarkerResult", " leftEyelidLandmarks "+leftEyelidLandmarks );
//            Log.e("faceLandmarkerResult", " RightEyeOpen "+rightEyelidLandmarks);
           // List<NormalizedLandmark> leftEyelidLandmarks = Stream.of(landmarks).filter(p -> landmarks.contains(p.get(159)) || landmarks.contains(p.get(145)));
           // List<List<NormalizedLandmark>> rightEyelidLandmarks = Stream.of(landmarks).filter(p -> landmarks.contains(p.get(386)) || landmarks.contains(p.get(374))).collect(Collectors.toList());

            //            List<NormalizedLandmark> rightEyelidLandmarks = List.of(
//                    landmarks.get(386),
//                    landmarks.get(374)
//            );

            double threshold = 0.01; // Adjust this threshold based on your requirements
            boolean leftEyelidOpen = Math.abs(leftEyelidLandmarks.get(0).y() - leftEyelidLandmarks.get(1).y()) > threshold;
            boolean rightEyelidOpen = Math.abs(rightEyelidLandmarks.get(0).y()  - rightEyelidLandmarks.get(1).y()) > threshold;

            double angle = calculateAngle(leftEyelidLandmarks.get(0), leftEyelidLandmarks.get(1), rightEyelidLandmarks.get(0));
            Log.e("faceLandmarkerResult", " RightEyeOpen " + rightEyelidOpen + "     LeftEyeOpen " + leftEyelidOpen + " EyeAngle: " + angle);
           // ObserveEyes.getInstance().updateValue(new EyeModel(leftEyelidOpen, rightEyelidOpen, angle));
        }

//        List<FaceLandmarkConnection> FACE_LANDMARKS_CONNECTORS = FaceLandmarker.getFaceLandmarkConnectors();
//        for (FaceLandmarkConnection connector : FACE_LANDMARKS_CONNECTORS) {
//            NormalizedLandmark startLandmark = faceLandmarks.get(0).get(connector.getStart());
//            NormalizedLandmark endLandmark = faceLandmarks.get(0).get(connector.getEnd());
//
//            float startX = (float) (startLandmark.getX() * imageWidth * scaleFactor);
//            float startY = (float) (startLandmark.getY() * imageHeight * scaleFactor);
//            float endX = (float) (endLandmark.getX() * imageWidth * scaleFactor);
//            float endY = (float) (endLandmark.getY() * imageHeight * scaleFactor);
//
//            canvas.drawLine(startX, startY, endX, endY, linePaint);
//        }
    }

    private double calculateAngle(NormalizedLandmark point1, NormalizedLandmark point2, NormalizedLandmark point3) {
        double[] vector1 = new double[]{
                point1.x() - point2.x(),
                point1.y() - point2.y()
        };
        double[] vector2 = new double[]{
                point3.x() - point2.x(),
                point3.y() - point2.y()
        };

        double dotProduct = vector1[0] * vector2[0] + vector1[1] * vector2[1];
        double magnitude1 = Math.sqrt(vector1[0] * vector1[0] + vector1[1] * vector1[1]);
        double magnitude2 = Math.sqrt(vector2[0] * vector2[0] + vector2[1] * vector2[1]);

        double angleRad = Math.acos(dotProduct / (magnitude1 * magnitude2));
        double angleDeg = Math.toDegrees(angleRad);

        return angleDeg;
    }

    public void setResults(
            FaceLandmarkerResult faceLandmarkerResults,
            int imageHeight,
            int imageWidth,
            RunningMode runningMode
    ) {
        results = faceLandmarkerResults;
        this.imageHeight = imageHeight;
        this.imageWidth = imageWidth;

        switch (runningMode) {
            case IMAGE:
            case VIDEO:
                scaleFactor = Math.min(getWidth() * 1f / imageWidth, getHeight() * 1f / imageHeight);
                break;
            case LIVE_STREAM:
                // PreviewView is in FILL_START mode. So we need to scale up the
                // landmarks to match with the size that the captured images will be displayed.
                scaleFactor = Math.max(getWidth() * 1f / imageWidth, getHeight() * 1f / imageHeight);
                break;
        }
        invalidate();
    }
    private void initPaints() {
        linePaint.setColor(ContextCompat.getColor(getContext(), R.color.mp_color_primary));
        linePaint.setStrokeWidth(LANDMARK_STROKE_WIDTH);
        linePaint.setStyle(Paint.Style.STROKE);

        pointPaint.setColor(Color.YELLOW);
        pointPaint.setStrokeWidth(LANDMARK_STROKE_WIDTH);
        pointPaint.setStyle(Paint.Style.FILL);
    }
}
