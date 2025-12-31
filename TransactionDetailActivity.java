package com.example.personal_finance_manager;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.personal_finance_manager.database.AppDatabase;
import com.example.personal_finance_manager.database.DatabaseClient;
import com.example.personal_finance_manager.model.Transaction;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class TransactionDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvAmount, tvCategory, tvType, tvDate, tvDescription, tvSyncStatus;
    private Button btnEdit, btnDelete, btnBack;

    private AppDatabase database;
    private Transaction transaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);

        database = DatabaseClient.getInstance(this).getAppDatabase();

        String transactionId = getIntent().getStringExtra("transaction_id");
        if (transactionId == null) {
            finish();
            return;
        }

        initViews();
        loadTransaction(transactionId);
        setupListeners();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvAmount = findViewById(R.id.tvAmount);
        tvCategory = findViewById(R.id.tvCategory);
        tvType = findViewById(R.id.tvType);
        tvDate = findViewById(R.id.tvDate);
        tvDescription = findViewById(R.id.tvDescription);
        tvSyncStatus = findViewById(R.id.tvSyncStatus);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        btnBack = findViewById(R.id.btnBack);
    }

    private void loadTransaction(String transactionId) {
        new Thread(() -> {
            transaction = database.transactionDao().getById(transactionId);
            runOnUiThread(this::updateUI);
        }).start();
    }

    private void updateUI() {
        if (transaction == null) {
            Toast.makeText(this, "Transaksi tidak ditemukan", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvTitle.setText(transaction.getTitle());

        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        String amount = format.format(transaction.getAmount());

        if ("income".equals(transaction.getType())) {
            tvAmount.setText("+" + amount);
            tvAmount.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            tvType.setText("Pemasukan");
        } else {
            tvAmount.setText("-" + amount);
            tvAmount.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            tvType.setText("Pengeluaran");
        }

        tvCategory.setText(transaction.getCategory());

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        tvDate.setText(sdf.format(transaction.getDate()));

        if (transaction.getDescription() != null && !transaction.getDescription().isEmpty()) {
            tvDescription.setText(transaction.getDescription());
            tvDescription.setVisibility(View.VISIBLE);
        } else {
            tvDescription.setVisibility(View.GONE);
        }

        tvSyncStatus.setText(transaction.isSynced() ? "Tersinkronisasi" : "Belum Sinkron");
        tvSyncStatus.setTextColor(transaction.isSynced() ?
                getResources().getColor(android.R.color.holo_green_dark) :
                getResources().getColor(android.R.color.holo_orange_dark));
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnEdit.setOnClickListener(v -> {
            // For simplicity, we'll just delete and let user add new
            // In real app, you would create an EditActivity
            Toast.makeText(this, "Fitur edit akan datang", Toast.LENGTH_SHORT).show();
        });

        btnDelete.setOnClickListener(v -> {
            new Thread(() -> {
                database.transactionDao().delete(transaction);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Transaksi dihapus", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }).start();
        });
    }
}