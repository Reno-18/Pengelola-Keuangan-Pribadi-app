package com.example.personal_finance_manager.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.personal_finance_manager.database.DatabaseClient;
import com.example.personal_finance_manager.model.Transaction;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExportService {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface ExportCallback {
        void onSuccess(String filePath);
        void onError(String message);
    }

    public static void exportToCSV(Context context, Date startDate, Date endDate,
                                   ExportCallback callback) {
        executor.execute(() -> {
            try {
                List<Transaction> transactions = DatabaseClient.getInstance(context)
                        .getAppDatabase()
                        .transactionDao()
                        .getByDateRange(startDate, endDate);

                if (transactions.isEmpty()) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("Tidak ada data untuk diekspor"));
                    return;
                }

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                SimpleDateFormat fileSdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());

                // Create directory if not exists
                File exportDir = new File(context.getExternalFilesDir(null), "Exports");
                if (!exportDir.exists()) {
                    exportDir.mkdirs();
                }

                String fileName = "transaksi_" + fileSdf.format(new Date()) + ".csv";
                File file = new File(exportDir, fileName);

                FileWriter writer = new FileWriter(file);
                CSVPrinter csvPrinter = new CSVPrinter(writer,
                        CSVFormat.DEFAULT.withHeader(
                                "Tanggal",
                                "Judul",
                                "Kategori",
                                "Tipe",
                                "Jumlah",
                                "Keterangan",
                                "Status Sync"
                        ));

                for (Transaction t : transactions) {
                    csvPrinter.printRecord(
                            sdf.format(t.getDate()),
                            t.getTitle(),
                            t.getCategory(),
                            t.getType(),
                            t.getAmount(),
                            t.getDescription() != null ? t.getDescription() : "",
                            t.isSynced() ? "Tersinkronisasi" : "Belum Sync"
                    );
                }

                csvPrinter.flush();
                csvPrinter.close();

                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onSuccess(file.getAbsolutePath()));

            } catch (IOException e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError(e.getMessage()));
            }
        });
    }

    public static void exportToPDF(Context context, ExportCallback callback) {
        // PDF export belum diimplementasi
        new Handler(Looper.getMainLooper()).post(() ->
                callback.onError("Ekspor PDF belum tersedia"));
    }
}