package com.example.personal_finance_manager.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.personal_finance_manager.config.SupabaseConfig;
import com.example.personal_finance_manager.database.DatabaseClient;
import com.example.personal_finance_manager.model.ExportStatus;
import com.example.personal_finance_manager.model.Transaction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ExportRepository {
    private static final String TAG = "ExportRepository";
    private static ExportRepository instance;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final MutableLiveData<ExportStatus> exportStatus = new MutableLiveData<>();

    private ExportRepository() {
        exportStatus.setValue(ExportStatus.idle());
    }

    public static synchronized ExportRepository getInstance() {
        if (instance == null) {
            instance = new ExportRepository();
        }
        return instance;
    }

    public LiveData<ExportStatus> getExportStatus() {
        return exportStatus;
    }

    public void exportToWeb(Context context, Date startDate, Date endDate) {
        executor.execute(() -> {
            try {
                // Update status: Preparing
                exportStatus.postValue(ExportStatus.preparing());

                Log.d(TAG, "Starting export from " + startDate + " to " + endDate);

                // 1. Get data from database
                List<Transaction> transactions = DatabaseClient.getInstance(context)
                        .getAppDatabase()
                        .transactionDao()
                        .getByDateRange(startDate, endDate);

                Log.d(TAG, "Found " + transactions.size() + " transactions");

                if (transactions.isEmpty()) {
                    exportStatus.postValue(ExportStatus.error("Tidak ada data untuk diekspor"));
                    return;
                }

                // 2. Calculate summary for filename
                double totalIncome = DatabaseClient.getInstance(context)
                        .getAppDatabase()
                        .transactionDao()
                        .getTotalIncome(startDate, endDate);
                double totalExpense = DatabaseClient.getInstance(context)
                        .getAppDatabase()
                        .transactionDao()
                        .getTotalExpense(startDate, endDate);
                double balance = totalIncome - totalExpense;

                // 3. Convert to CSV
                exportStatus.postValue(new ExportStatus(ExportStatus.State.PREPARING,
                        "Mengkonversi " + transactions.size() + " transaksi ke CSV..."));

                String csvData = convertToCSV(transactions);
                Log.d(TAG, "CSV data length: " + csvData.length() + " bytes");

                // 4. Generate beautiful filename
                String fileName = generateBeautifulFilename(startDate, endDate,
                        transactions.size(), totalIncome, totalExpense, balance);

                // 5. Upload to Supabase Storage
                uploadToSupabase(csvData, fileName);

            } catch (Exception e) {
                Log.e(TAG, "Export error: " + e.getMessage(), e);
                exportStatus.postValue(ExportStatus.error("Error: " + e.getMessage()));
            }
        });
    }

    /**
     * Generate beautiful and informative filename
     */
    private String generateBeautifulFilename(Date startDate, Date endDate,
                                             int transactionCount,
                                             double totalIncome,
                                             double totalExpense,
                                             double balance) {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM_yyyy", new Locale("id", "ID"));
        SimpleDateFormat fullFormat = new SimpleDateFormat("dd_MMMM_yyyy", new Locale("id", "ID"));

        String startDateStr = dateFormat.format(startDate);
        String endDateStr = dateFormat.format(endDate);
        String monthYear = monthFormat.format(startDate);
        String fullStartDate = fullFormat.format(startDate);
        String fullEndDate = fullFormat.format(endDate);

        // Format untuk jumlah transaksi
        String countStr = String.format(Locale.getDefault(), "%d_transaksi", transactionCount);

        // Format untuk saldo
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        String balanceStr = currencyFormat.format(balance)
                .replace("Rp", "")
                .replace(",", "")
                .replace(".", "")
                .trim();

        // Pilih format filename berdasarkan periode
        String fileName;

        if (isSameMonth(startDate, endDate)) {
            // Jika dalam bulan yang sama
            if (isSameDay(startDate, endDate)) {
                // Hari yang sama
                fileName = String.format(Locale.getDefault(),
                        "Laporan_Harian_%s_%s_%s.csv",
                        fullStartDate, countStr, balanceStr);
            } else {
                // Rentang dalam bulan yang sama
                fileName = String.format(Locale.getDefault(),
                        "Laporan_Bulanan_%s_%s_%s.csv",
                        monthYear, countStr, balanceStr);
            }
        } else if (isSameYear(startDate, endDate)) {
            // Tahun yang sama, bulan berbeda
            fileName = String.format(Locale.getDefault(),
                    "Laporan_Periodik_%s_sampai_%s_%s_%s.csv",
                    fullStartDate, fullEndDate, countStr, balanceStr);
        } else {
            // Tahun berbeda
            fileName = String.format(Locale.getDefault(),
                    "Laporan_Keuangan_%s_sampai_%s_%s_%s.csv",
                    startDateStr, endDateStr, countStr, balanceStr);
        }

        // Clean filename (remove spaces and special chars)
        fileName = fileName
                .replace(" ", "_")
                .replace("__", "_")
                .replace("___", "_")
                .replace("____", "_");

        Log.d(TAG, "Generated filename: " + fileName);
        return fileName;
    }

    private boolean isSameMonth(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH);
    }

    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    private boolean isSameYear(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR);
    }

    private String convertToCSV(List<Transaction> transactions) {
        StringBuilder csv = new StringBuilder();

        // UTF-8 BOM for Excel compatibility
        csv.append("\uFEFF");

        // Beautiful header with Indonesian labels
        csv.append("No,Tanggal,Judul Transaksi,Kategori,Tipe,Jumlah (Rp),Keterangan,Status Sinkronisasi\n");

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

        int counter = 1;
        for (Transaction t : transactions) {
            csv.append(counter).append(",");
            csv.append(sdf.format(t.getDate())).append(",");
            csv.append(escapeCsv(t.getTitle())).append(",");
            csv.append(escapeCsv(t.getCategory())).append(",");
            csv.append(t.getType().equals("income") ? "Pemasukan" : "Pengeluaran").append(",");
            csv.append(currencyFormat.format(t.getAmount())).append(",");
            csv.append(escapeCsv(t.getDescription() != null ? t.getDescription() : "")).append(",");
            csv.append(t.isSynced() ? "Tersinkronisasi" : "Belum Sinkron").append("\n");
            counter++;
        }

        // Add summary section
        csv.append("\n\n"); // Blank line
        csv.append("RINGKASAN\n");
        csv.append("=========\n");

        double totalIncome = transactions.stream()
                .filter(t -> t.getType().equals("income"))
                .mapToDouble(Transaction::getAmount)
                .sum();

        double totalExpense = transactions.stream()
                .filter(t -> t.getType().equals("expense"))
                .mapToDouble(Transaction::getAmount)
                .sum();

        double balance = totalIncome - totalExpense;

        csv.append("Total Pemasukan,").append(currencyFormat.format(totalIncome)).append("\n");
        csv.append("Total Pengeluaran,").append(currencyFormat.format(totalExpense)).append("\n");
        csv.append("Saldo,").append(currencyFormat.format(balance)).append("\n");
        csv.append("Jumlah Transaksi,").append(transactions.size()).append("\n");

        // Add metadata
        csv.append("\n\n"); // Blank line
        csv.append("METADATA\n");
        csv.append("========\n");
        csv.append("Tanggal Ekspor,").append(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
        csv.append("Aplikasi,KeuanganKu Personal Finance Manager\n");
        csv.append("Versi,1.0\n");

        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void uploadToSupabase(String csvData, String fileName) {
        try {
            // Update status: Uploading
            exportStatus.postValue(new ExportStatus(ExportStatus.State.UPLOADING,
                    "Mengupload file: " + fileName));

            Log.d(TAG, "Uploading file: " + fileName);

            // Convert CSV string to bytes
            byte[] fileBytes = csvData.getBytes("UTF-8");

            // Create request body
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fileName,
                            RequestBody.create(fileBytes, MediaType.parse("text/csv")))
                    .build();

            // Upload URL
            String uploadUrl = SupabaseConfig.SUPABASE_URL +
                    "/storage/v1/object/" +
                    SupabaseConfig.STORAGE_BUCKET + "/" + fileName;

            Log.d(TAG, "Upload URL: " + uploadUrl);

            // Create request
            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY)
                    .addHeader("Content-Type", "multipart/form-data")
                    .addHeader("Cache-Control", "no-cache")
                    .build();

            // Execute upload
            Response response = httpClient.newCall(request).execute();
            String responseBody = response.body() != null ? response.body().string() : "No response body";

            Log.d(TAG, "Upload response code: " + response.code());

            if (response.isSuccessful()) {
                JSONObject json = new JSONObject(responseBody);

                // Generate public download URL
                String downloadUrl = SupabaseConfig.SUPABASE_URL +
                        "/storage/v1/object/public/" +
                        SupabaseConfig.STORAGE_BUCKET + "/" + fileName;

                Log.d(TAG, "Download URL: " + downloadUrl);

                // Update status: Success
                exportStatus.postValue(new ExportStatus(ExportStatus.State.SUCCESS,
                        "✅ Ekspor berhasil!\nFile: " + fileName, downloadUrl));

            } else {
                String errorMessage;
                try {
                    JSONObject errorJson = new JSONObject(responseBody);
                    errorMessage = errorJson.optString("message", "Unknown error");
                    if (errorMessage.contains("bucket")) {
                        errorMessage = "Bucket 'exports' belum dibuat di Supabase. Buat dulu di dashboard Supabase.";
                    }
                } catch (Exception e) {
                    errorMessage = "Upload gagal: HTTP " + response.code();
                }

                Log.e(TAG, "Upload failed: " + errorMessage);
                exportStatus.postValue(ExportStatus.error("❌ " + errorMessage));
            }

        } catch (Exception e) {
            String errorMessage = e.getMessage();

            // Handle specific errors
            if (errorMessage != null) {
                if (errorMessage.contains("Failed to connect")) {
                    errorMessage = "Gagal koneksi ke server. Periksa koneksi internet.";
                } else if (errorMessage.contains("Unable to resolve host")) {
                    errorMessage = "Tidak bisa terhubung ke Supabase. Periksa URL di konfigurasi.";
                }
            }

            Log.e(TAG, "Upload error: " + errorMessage, e);
            exportStatus.postValue(ExportStatus.error("❌ " + (errorMessage != null ? errorMessage : "Unknown error")));
        }
    }

    public void resetExportStatus() {
        exportStatus.postValue(ExportStatus.idle());
    }
}