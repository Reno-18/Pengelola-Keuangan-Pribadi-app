package com.example.personal_finance_manager.config;

public class SupabaseConfig {
    // Replace with your Supabase project credentials
    public static final String SUPABASE_URL = "https://pbczkqqdcrkuybrmrzjw.supabase.co";
    public static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBiY3prcXFkY3JrdXlicm1yemp3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjYwMDE4MTksImV4cCI6MjA4MTU3NzgxOX0.Lh_gBclv6pcm4VNBTXxVq4alfmEdN87p_TN93c35Xdg";

    // Table names
    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String TABLE_CATEGORIES = "categories";
    public static final String TABLE_BUDGETS = "budgets";

    public static final String STORAGE_BUCKET = "exports";

    // API endpoints
    public static final String TRANSACTIONS_ENDPOINT = SUPABASE_URL + "/rest/v1/" + TABLE_TRANSACTIONS;
    public static final String CATEGORIES_ENDPOINT = SUPABASE_URL + "/rest/v1/" + TABLE_CATEGORIES;
}