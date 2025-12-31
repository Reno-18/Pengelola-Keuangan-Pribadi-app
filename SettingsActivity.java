package com.example.personal_finance_manager;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.example.personal_finance_manager.config.SupabaseConfig;

public class SettingsActivity extends AppCompatActivity {

    private EditText etSupabaseUrl, etSupabaseKey;
    private CheckBox cbAutoSync, cbNotifications;
    private Button btnSave, btnTestConnection, btnReset;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("finance_prefs", MODE_PRIVATE);

        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        etSupabaseUrl = findViewById(R.id.etSupabaseUrl);
        etSupabaseKey = findViewById(R.id.etSupabaseKey);
        cbAutoSync = findViewById(R.id.cbAutoSync);
        cbNotifications = findViewById(R.id.cbNotifications);
        btnSave = findViewById(R.id.btnSave);
        btnTestConnection = findViewById(R.id.btnTestConnection);
        btnReset = findViewById(R.id.btnReset);
    }

    private void loadSettings() {
        etSupabaseUrl.setText(prefs.getString("supabase_url", SupabaseConfig.SUPABASE_URL));
        etSupabaseKey.setText(prefs.getString("supabase_key", SupabaseConfig.SUPABASE_KEY));
        cbAutoSync.setChecked(prefs.getBoolean("auto_sync", true));
        cbNotifications.setChecked(prefs.getBoolean("notifications", true));
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveSettings());

        btnTestConnection.setOnClickListener(v -> testConnection());

        btnReset.setOnClickListener(v -> resetToDefaults());
    }

    private void saveSettings() {
        String url = etSupabaseUrl.getText().toString().trim();
        String key = etSupabaseKey.getText().toString().trim();

        if (url.isEmpty() || key.isEmpty()) {
            Toast.makeText(this, "URL dan Key tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("supabase_url", url);
        editor.putString("supabase_key", key);
        editor.putBoolean("auto_sync", cbAutoSync.isChecked());
        editor.putBoolean("notifications", cbNotifications.isChecked());
        editor.apply();

        Toast.makeText(this, "Pengaturan disimpan", Toast.LENGTH_SHORT).show();
    }

    private void testConnection() {
        // Implement connection test
        Toast.makeText(this, "Fitur test koneksi akan datang", Toast.LENGTH_SHORT).show();
    }

    private void resetToDefaults() {
        etSupabaseUrl.setText(SupabaseConfig.SUPABASE_URL);
        etSupabaseKey.setText(SupabaseConfig.SUPABASE_KEY);
        cbAutoSync.setChecked(true);
        cbNotifications.setChecked(true);

        Toast.makeText(this, "Pengaturan direset ke default", Toast.LENGTH_SHORT).show();
    }
}