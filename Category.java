package com.example.personal_finance_manager.model;

public class Category {
    private String name;
    private String type; // "income" or "expense"
    private int iconResId;

    public Category(String name, String type, int iconResId) {
        this.name = name;
        this.type = type;
        this.iconResId = iconResId;
    }

    // Getters
    public String getName() { return name; }
    public String getType() { return type; }
    public int getIconResId() { return iconResId; }
}