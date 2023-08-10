package com.example.facelandmarklibrary.ViewModel;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.facelandmarklibrary.IrisDetector.FaceLandmarkerHelper;


public class DataShareViewModel extends ViewModel {
    Application application;
    MutableLiveData<ChecklistLiveDataModel> checklistMutableLiveData = new MutableLiveData<>();
    private MutableLiveData<String> stringData = new MutableLiveData<>();



    public MutableLiveData<String> getStringData() {


        return stringData;
    }

    public void setStringData(String data) {
        stringData.setValue(data);
    }


    //................................

    private MutableLiveData<BlockStatusModel> data = new MutableLiveData<>();

    public LiveData<BlockStatusModel> getBlockStatusData() {
        return data;
    }

    public void updateData(BlockStatusModel newData) {
        data.setValue(newData);
    }


//    public void setMuatableLivedata(ChecklistLiveDataModel liveDataModel) {
//
//        checklistMutableLiveData.setValue(liveDataModel);
//
//    }
//
//    public MutableLiveData<ChecklistLiveDataModel> getChecklistMutableLiveData() {
//
//        if (checklistMutableLiveData == null) {
//            checklistMutableLiveData = new MutableLiveData<>();
//        }
//
//        return checklistMutableLiveData;
//    }
    private int _delegate = FaceLandmarkerHelper.DELEGATE_CPU;
    private float _minFaceDetectionConfidence = FaceLandmarkerHelper.DEFAULT_FACE_DETECTION_CONFIDENCE;
    private float _minFaceTrackingConfidence = FaceLandmarkerHelper.DEFAULT_FACE_TRACKING_CONFIDENCE;
    private float _minFacePresenceConfidence = FaceLandmarkerHelper.DEFAULT_FACE_PRESENCE_CONFIDENCE;
    private int _maxFaces = FaceLandmarkerHelper.DEFAULT_NUM_FACES;

    public int getCurrentDelegate() {
        return _delegate;
    }

    public float getCurrentMinFaceDetectionConfidence() {
        return _minFaceDetectionConfidence;
    }

    public float getCurrentMinFaceTrackingConfidence() {
        return _minFaceTrackingConfidence;
    }

    public float getCurrentMinFacePresenceConfidence() {
        return _minFacePresenceConfidence;
    }

    public int getCurrentMaxFaces() {
        return _maxFaces;
    }

    public void setDelegate(int delegate) {
        _delegate = delegate;
    }

    public void setMinFaceDetectionConfidence(float confidence) {
        _minFaceDetectionConfidence = confidence;
    }

    public void setMinFaceTrackingConfidence(float confidence) {
        _minFaceTrackingConfidence = confidence;
    }

    public void setMinFacePresenceConfidence(float confidence) {
        _minFacePresenceConfidence = confidence;
    }

    public void setMaxFaces(int maxResults) {
        _maxFaces = maxResults;
    }

}
