package com.example.personal_finance_manager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.personal_finance_manager.config.AppConfig;
import com.example.personal_finance_manager.database.AppDatabase;
import com.example.personal_finance_manager.database.DatabaseClient;
import com.example.personal_finance_manager.model.Category;
import com.example.personal_finance_manager.model.Transaction;
import com.example.personal_finance_manager.ui.CategoryAdapter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddTransactionActivity extends AppCompatActivity {

    private EditText etTitle, etAmount, etDescription;
    private RadioGroup rgType;
    private RadioButton rbIncome, rbExpense;
    private RecyclerView rvCategories;
    private Button btnDate, btnSave, btnCancel;

    private AppDatabase database;
    private Date selectedDate;
    private Category selectedCategory;
    private CategoryAdapter categoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        database = DatabaseClient.getInstance(this).getAppDatabase();
        selectedDate = new Date();

        initViews();
        setupCategoryRecyclerView();
        setupListeners();
    }

    private void initViews() {
        // Find views by ID
        etTitle = findViewById(R.id.etTitle);
        etAmount = findViewById(R.id.etAmount);
        etDescription = findViewById(R.id.etDescription);
        rgType = findViewById(R.id.rgType);
        rbIncome = findViewById(R.id.rbIncome);
        rbExpense = findViewById(R.id.rbExpense);
        rvCategories = findViewById(R.id.rvCategories);
        btnDate = findViewById(R.id.btnDate);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        // Set default date
        updateDateButton();

        // Set default type
        rbExpense.setChecked(true);
    }

    private void setupCategoryRecyclerView() {
        // Setup RecyclerView
        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        rvCategories.setLayoutManager(layoutManager);

        // Setup adapter - PERBAIKAN: PASS CONTEXT
        categoryAdapter = new CategoryAdapter(this);  // <-- TAMBAHKAN this
        rvCategories.setAdapter(categoryAdapter);

        // Set click listener
        categoryAdapter.setOnCategoryClickListener(new CategoryAdapter.OnCategoryClickListener() {
            @Override
            public void onCategoryClick(Category category, int position) {
                selectedCategory = category;
                categoryAdapter.setSelectedPosition(position);
                Toast.makeText(AddTransactionActivity.this,
                        "Kategori dipilih: " + category.getName(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Load initial categories
        updateCategories();
    }

    private void setupListeners() {
        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            updateCategories();
        });

        btnDate.setOnClickListener(v -> showDatePicker());

        btnSave.setOnClickListener(v -> saveTransaction());

        btnCancel.setOnClickListener(v -> finish());
    }

    private void updateCategories() {
        List<Category> categories;
        if (rbIncome.isChecked()) {
            categories = AppConfig.getIncomeCategories();
        } else {
            categories = AppConfig.getExpenseCategories();
        }

        categoryAdapter.setCategories(categories);

        // Select first category by default
        if (!categories.isEmpty()) {
            selectedCategory = categories.get(0);
            categoryAdapter.setSelectedPosition(0);
        }
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(selectedDate);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        Calendar cal = Calendar.getInstance();
                        cal.set(year, month, dayOfMonth);
                        selectedDate = cal.getTime();
                        updateDateButton();
                    }
                },
                year, month, day
        );

        datePickerDialog.show();
    }

    private void updateDateButton() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        btnDate.setText(sdf.format(selectedDate));
    }

    private void saveTransaction() {
        // Validate input
        String title = etTitle.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Judul tidak boleh kosong");
            return;
        }

        if (TextUtils.isEmpty(amountStr)) {
            etAmount.setError("Jumlah tidak boleh kosong");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                etAmount.setError("Jumlah harus lebih dari 0");
                return;
            }
        } catch (NumberFormatException e) {
            etAmount.setError("Format jumlah tidak valid");
            return;
        }

        if (selectedCategory == null) {
            Toast.makeText(this, "Pilih kategori terlebih dahulu",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Get transaction type
        String type = rbIncome.isChecked() ? "income" : "expense";

        // Create transaction
        Transaction transaction = new Transaction();
        transaction.setTitle(title);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setCategory(selectedCategory.getName());
        transaction.setDate(selectedDate);

        if (!TextUtils.isEmpty(description)) {
            transaction.setDescription(description);
        }

        // Save to database
        new Thread(() -> {
            long id = database.transactionDao().insert(transaction);
            runOnUiThread(() -> {
                if (id > 0) {
                    Toast.makeText(AddTransactionActivity.this,
                            "Transaksi berhasil disimpan ✓",
                            Toast.LENGTH_SHORT).show();

                    // Data akan auto-update di MainActivity via LiveData
                    setResult(RESULT_OK);
                    finish();
                }
            });
        }).start();
    }
}