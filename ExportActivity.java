package com.example.personal_finance_manager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.personal_finance_manager.model.ExportStatus;
import com.example.personal_finance_manager.viewmodel.ExportViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ExportActivity extends AppCompatActivity {

    private RadioGroup rgFormat;
    private RadioButton rbWeb;
    private TextView tvStartDate, tvEndDate, tvStatus, tvProgressText;
    private Button btnPickStartDate, btnPickEndDate, btnExport, btnCancel;
    private ProgressBar progressBar;

    private ExportViewModel exportViewModel;
    private Date startDate, endDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_web);

        // Initialize ViewModel
        exportViewModel = new ViewModelProvider(this).get(ExportViewModel.class);

        // Set default date range (last month)
        Calendar cal = Calendar.getInstance();
        endDate = cal.getTime();

        cal.add(Calendar.MONTH, -1);
        startDate = cal.getTime();

        initViews();
        updateDateTexts();
        setupObservers();
        setupListeners();
    }

    private void initViews() {
        rgFormat = findViewById(R.id.rgFormat);
        rbWeb = findViewById(R.id.rbWeb);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvEndDate = findViewById(R.id.tvEndDate);
        tvStatus = findViewById(R.id.tvStatus);
        tvProgressText = findViewById(R.id.tvProgressText);
        btnPickStartDate = findViewById(R.id.btnPickStartDate);
        btnPickEndDate = findViewById(R.id.btnPickEndDate);
        btnExport = findViewById(R.id.btnExport);
        btnCancel = findViewById(R.id.btnCancel);
        progressBar = findViewById(R.id.progressBar);

        // Default format
        rbWeb.setChecked(true);
    }

    private void updateDateTexts() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvStartDate.setText(sdf.format(startDate));
        tvEndDate.setText(sdf.format(endDate));
    }

    private void setupObservers() {
        // Observe export status
        exportViewModel.getExportStatus().observe(this, exportStatus -> {
            if (exportStatus != null) {
                handleExportStatus(exportStatus);
            }
        });

        // Observe exporting state
        exportViewModel.getIsExporting().observe(this, isExporting -> {
            btnExport.setEnabled(!isExporting);
            btnExport.setText(isExporting ? "Mengekspor..." : "Ekspor ke Web");

            if (isExporting) {
                progressBar.setVisibility(View.VISIBLE);
                tvStatus.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupListeners() {
        btnPickStartDate.setOnClickListener(v -> showDatePicker(true));
        btnPickEndDate.setOnClickListener(v -> showDatePicker(false));

        btnExport.setOnClickListener(v -> startExport());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void showDatePicker(final boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        Date dateToShow = isStartDate ? startDate : endDate;
        calendar.setTime(dateToShow);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year1, month1, dayOfMonth) -> {
                    Calendar cal = Calendar.getInstance();
                    cal.set(year1, month1, dayOfMonth);

                    if (isStartDate) {
                        startDate = cal.getTime();
                    } else {
                        endDate = cal.getTime();
                    }

                    // Validate dates
                    if (startDate.after(endDate)) {
                        Toast.makeText(ExportActivity.this,
                                "Tanggal mulai tidak boleh setelah tanggal akhir",
                                Toast.LENGTH_SHORT).show();
                        if (isStartDate) {
                            startDate = endDate;
                        }
                    }

                    updateDateTexts();
                },
                year, month, day
        );

        datePickerDialog.show();
    }

    private void startExport() {
        // Validate dates
        if (startDate.after(endDate)) {
            Toast.makeText(this, "Tanggal mulai harus sebelum tanggal akhir",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Update ViewModel with selected dates
        exportViewModel.setDateRange(startDate, endDate);

        // Start export process
        exportViewModel.startExport();
    }

    private void handleExportStatus(ExportStatus status) {
        switch (status.getState()) {
            case PREPARING:
                tvStatus.setText("📊 " + status.getMessage());
                progressBar.setIndeterminate(true);
                progressBar.setVisibility(View.VISIBLE);
                tvStatus.setVisibility(View.VISIBLE);
                break;

            case UPLOADING:
                tvStatus.setText("📤 " + status.getMessage());
                progressBar.setIndeterminate(false);
                progressBar.setVisibility(View.VISIBLE);
                tvStatus.setVisibility(View.VISIBLE);
                break;

            case SUCCESS:
                tvStatus.setText("✅ " + status.getMessage());
                progressBar.setVisibility(View.GONE);
                tvProgressText.setVisibility(View.GONE);

                // Extract filename from message
                String message = status.getMessage();
                String fileName = extractFileName(message);
                String downloadUrl = status.getDownloadUrl();

                showSuccessDialog(fileName, downloadUrl);
                exportViewModel.resetExport();
                break;

            case ERROR:
                tvStatus.setText("❌ " + status.getMessage());
                progressBar.setVisibility(View.GONE);
                tvProgressText.setVisibility(View.GONE);
                Toast.makeText(this, status.getMessage(), Toast.LENGTH_LONG).show();
                exportViewModel.resetExport();
                break;

            case IDLE:
                tvStatus.setText("");
                tvStatus.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                tvProgressText.setVisibility(View.GONE);
                break;
        }
    }

    private String extractFileName(String message) {
        // Extract filename from message like "✅ Ekspor berhasil!\nFile: Laporan_Harian_..."
        if (message != null && message.contains("File: ")) {
            String[] parts = message.split("File: ");
            if (parts.length > 1) {
                return parts[1];
            }
        }
        return "data_export.csv";
    }

    private void showSuccessDialog(String fileName, String downloadUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("✅ Ekspor Berhasil");
        builder.setMessage("File berhasil diupload ke Supabase Storage.\n\n" +
                "📁 Nama File: " + fileName + "\n\n" +
                "🔗 Download URL:\n" + downloadUrl);

        builder.setPositiveButton("📥 Download File", (dialog, which) -> {
            openInBrowser(downloadUrl);
        });

        builder.setNegativeButton("📋 Salin Link", (dialog, which) -> {
            copyToClipboard(downloadUrl);
        });

        builder.setNeutralButton("✏️ Ganti Nama File", (dialog, which) -> {
            showRenameDialog(fileName, downloadUrl);
        });

        builder.show();
    }

    private void showRenameDialog(String currentFileName, String downloadUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ganti Nama File");

        // Create input field
        final EditText input = new EditText(this);
        input.setText(currentFileName);
        input.setSelectAllOnFocus(true);

        builder.setView(input);

        builder.setPositiveButton("Simpan", (dialog, which) -> {
            String newFileName = input.getText().toString().trim();
            if (!newFileName.isEmpty()) {
                if (!newFileName.endsWith(".csv")) {
                    newFileName += ".csv";
                }
                // TODO: Implement rename functionality
                Toast.makeText(this, "Fitur rename akan datang", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Batal", null);
        builder.show();
    }

    private void openInBrowser(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Tidak bisa membuka browser", Toast.LENGTH_SHORT).show();
        }
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Download URL", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Link disalin ke clipboard", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        exportViewModel.resetExport();
    }
}