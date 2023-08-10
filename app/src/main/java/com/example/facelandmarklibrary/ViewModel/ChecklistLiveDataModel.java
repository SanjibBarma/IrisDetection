package com.example.facelandmarklibrary.ViewModel;

public class ChecklistLiveDataModel {

    private int position;
    private boolean status;

    public ChecklistLiveDataModel(int position, boolean status) {
        this.position = position;
        this.status = status;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "ChecklistLiveDataModel{" +
                "position=" + position +
                ", status=" + status +
                '}';
    }
}
