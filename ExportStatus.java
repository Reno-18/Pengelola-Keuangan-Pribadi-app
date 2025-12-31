package com.example.personal_finance_manager.model;

public class ExportStatus {
    public enum State {
        IDLE,
        PREPARING,
        UPLOADING,
        SUCCESS,
        ERROR
    }

    private State state;
    private String message;
    private String downloadUrl;
    private float progress;

    public ExportStatus(State state) {
        this.state = state;
    }

    public ExportStatus(State state, String message) {
        this.state = state;
        this.message = message;
    }

    public ExportStatus(State state, String message, String downloadUrl) {
        this.state = state;
        this.message = message;
        this.downloadUrl = downloadUrl;
    }

    public ExportStatus(State state, float progress) {
        this.state = state;
        this.progress = progress;
    }

    // Getters
    public State getState() { return state; }
    public String getMessage() { return message; }
    public String getDownloadUrl() { return downloadUrl; }
    public float getProgress() { return progress; }

    // Static helper methods
    public static ExportStatus idle() {
        return new ExportStatus(State.IDLE);
    }

    public static ExportStatus preparing() {
        return new ExportStatus(State.PREPARING, "Mempersiapkan data...");
    }

    public static ExportStatus uploading(float progress) {
        return new ExportStatus(State.UPLOADING, progress);
    }

    public static ExportStatus success(String downloadUrl) {
        return new ExportStatus(State.SUCCESS, "Upload berhasil!", downloadUrl);
    }

    public static ExportStatus error(String message) {
        return new ExportStatus(State.ERROR, message);
    }
}