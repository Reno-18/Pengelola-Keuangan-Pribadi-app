package com.example.personal_finance_manager.model;

public class ChartData {
    private String label;
    private float value;
    private int color;

    public ChartData(String label, float value, int color) {
        this.label = label;
        this.value = value;
        this.color = color;
    }

    // Getters
    public String getLabel() { return label; }
    public float getValue() { return value; }
    public int getColor() { return color; }
}