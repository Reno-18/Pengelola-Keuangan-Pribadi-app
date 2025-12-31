package com.example.personal_finance_manager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.personal_finance_manager.model.Transaction;
import com.example.personal_finance_manager.service.SupabaseService;
import com.example.personal_finance_manager.ui.TransactionAdapter;
import com.example.personal_finance_manager.viewmodel.TransactionViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvBalance, tvIncome, tvExpense, tvPeriod;
    private FloatingActionButton fabAdd;
    private Button btnAnalysis, btnExport, btnSync;

    private TransactionViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        // Initialize views
        initViews();

        // Setup observers
        setupObservers();

        // Set listeners
        setupListeners();

        // Load initial data
        viewModel.loadData();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        tvBalance = findViewById(R.id.tvBalance);
        tvIncome = findViewById(R.id.tvIncome);
        tvExpense = findViewById(R.id.tvExpense);
        tvPeriod = findViewById(R.id.tvPeriod);
        fabAdd = findViewById(R.id.fabAdd);
        btnAnalysis = findViewById(R.id.btnAnalysis);
        btnExport = findViewById(R.id.btnExport);
        btnSync = findViewById(R.id.btnSync);

        adapter = new TransactionAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Set up toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    public void onAddFirstTransaction(View view) {
        Intent intent = new Intent(this, AddTransactionActivity.class);
        startActivityForResult(intent, 1);
    }

    private void setupObservers() {
        // Observe transactions
        viewModel.getTransactions().observe(this, transactions -> {
            if (transactions != null) {
                adapter.setTransactions(transactions);

                // Handle empty state
                View emptyState = findViewById(R.id.emptyState);
                TextView tvTransactionsHeader = findViewById(R.id.tvTransactionsHeader);

                if (transactions.isEmpty()) {
                    // Show empty state
                    if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
                    if (tvTransactionsHeader != null) tvTransactionsHeader.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    // Show transactions
                    if (emptyState != null) emptyState.setVisibility(View.GONE);
                    if (tvTransactionsHeader != null) tvTransactionsHeader.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        });

        // Observe total income
        viewModel.getTotalIncome().observe(this, income -> {
            if (income != null) {
                tvIncome.setText(viewModel.formatCurrency(income));
            }
        });

        // Observe total expense
        viewModel.getTotalExpense().observe(this, expense -> {
            if (expense != null) {
                tvExpense.setText(viewModel.formatCurrency(expense));
            }
        });

        // Observe balance
        viewModel.getBalance().observe(this, balance -> {
            if (balance != null) {
                tvBalance.setText(viewModel.formatCurrency(balance));

                // Auto-change color based on balance
                if (balance >= 0) {
                    tvBalance.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                } else {
                    tvBalance.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                }
            }
        });

        // Observe period
        viewModel.getPeriod().observe(this, period -> {
            if (period != null) {
                tvPeriod.setText(period);
            }
        });
    }

    private void setupListeners() {
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddTransactionActivity.class);
            startActivityForResult(intent, 1);
        });

        swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.loadData();
            swipeRefreshLayout.setRefreshing(false);
        });

        btnAnalysis.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AnalysisActivity.class);
            startActivity(intent);
        });

        btnExport.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ExportActivity.class);
            startActivity(intent);
        });

        btnSync.setOnClickListener(v -> {
            syncWithCloud();
        });

        adapter.setOnItemClickListener(new TransactionAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Transaction transaction) {
                Intent intent = new Intent(MainActivity.this, TransactionDetailActivity.class);
                intent.putExtra("transaction_id", transaction.getId());
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(Transaction transaction) {
                showDeleteDialog(transaction);
            }
        });

        // Period selection buttons
        findViewById(R.id.btnToday).setOnClickListener(v -> {
            viewModel.setDateRange("Hari Ini");
        });

        findViewById(R.id.btnWeek).setOnClickListener(v -> {
            viewModel.setDateRange("Minggu Ini");
        });

        findViewById(R.id.btnMonth).setOnClickListener(v -> {
            viewModel.setDateRange("Bulan Ini");
        });

        findViewById(R.id.btnYear).setOnClickListener(v -> {
            viewModel.setDateRange("Tahun Ini");
        });

        findViewById(R.id.btnAll).setOnClickListener(v -> {
            viewModel.setDateRange("Semua");
        });
    }

    private void syncWithCloud() {
        btnSync.setEnabled(false);
        btnSync.setText("Menyinkronkan...");

        new Thread(() -> {
            List<Transaction> unsynced = viewModel.getTransactions().getValue().stream()
                    .filter(t -> !t.isSynced())
                    .collect(java.util.stream.Collectors.toList());

            if (!unsynced.isEmpty()) {
                SupabaseService.syncTransactions(this, unsynced, new SupabaseService.SyncCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this,
                                    "Sinkronisasi berhasil",
                                    Toast.LENGTH_SHORT).show();
                            viewModel.loadData(); // Reload data setelah sync
                            btnSync.setEnabled(true);
                            btnSync.setText("Sync");
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this,
                                    "Error: " + message,
                                    Toast.LENGTH_SHORT).show();
                            btnSync.setEnabled(true);
                            btnSync.setText("Sync");
                        });
                    }
                });
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                            "Semua data sudah tersinkronisasi",
                            Toast.LENGTH_SHORT).show();
                    btnSync.setEnabled(true);
                    btnSync.setText("Sync");
                });
            }
        }).start();
    }

    private void showDeleteDialog(Transaction transaction) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Transaksi")
                .setMessage("Apakah Anda yakin ingin menghapus transaksi ini?")
                .setPositiveButton("Hapus", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        viewModel.deleteTransaction(transaction);
                        Toast.makeText(MainActivity.this,
                                "Transaksi dihapus",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_clear_all) {
            showClearAllDialog();
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        } else if (id == R.id.action_search) {
            showSearchDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showClearAllDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Semua Data")
                .setMessage("Apakah Anda yakin ingin menghapus semua transaksi? Tindakan ini tidak dapat dibatalkan.")
                .setPositiveButton("Hapus", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        viewModel.clearAllData();
                        Toast.makeText(MainActivity.this,
                                "Semua data dihapus",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cari Transaksi");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Masukkan kata kunci...");
        builder.setView(input);

        builder.setPositiveButton("Cari", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String query = input.getText().toString();
                if (!query.trim().isEmpty()) {
                    viewModel.searchTransactions(query);
                }
            }
        });

        builder.setNegativeButton("Batal", null);
        builder.setNeutralButton("Reset", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                viewModel.loadData();
            }
        });

        builder.show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Tentang Aplikasi")
                .setMessage("Pengelola Keuangan Pribadi v1.0\n\n" +
                        "Aplikasi untuk mengelola keuangan pribadi dengan fitur:\n" +
                        "• Catatan pemasukan dan pengeluaran\n" +
                        "• Auto-update UI dengan LiveData\n" +
                        "• Sinkronisasi cloud\n" +
                        "• Analisis pengeluaran\n" +
                        "• Ekspor data ke CSV/Web")
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Data akan auto-update via LiveData observer
            Toast.makeText(this, "Data diperbarui ✓", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data setiap kembali ke MainActivity
        viewModel.loadData();
    }
}