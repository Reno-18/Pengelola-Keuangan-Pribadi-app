package com.example.personal_finance_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.personal_finance_manager.database.AppDatabase;
import com.example.personal_finance_manager.database.DatabaseClient;
import com.example.personal_finance_manager.database.dao.TransactionDao;
import com.example.personal_finance_manager.model.Transaction;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransactionViewModel extends AndroidViewModel {

    private final AppDatabase database;
    private final TransactionDao transactionDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<Transaction>> transactions = new MutableLiveData<>();
    private final MutableLiveData<Double> totalIncome = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> totalExpense = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> balance = new MutableLiveData<>(0.0);
    private final MutableLiveData<String> period = new MutableLiveData<>("Bulan Ini");
    private final MutableLiveData<Date> startDate = new MutableLiveData<>();
    private final MutableLiveData<Date> endDate = new MutableLiveData<>();

    public TransactionViewModel(@NonNull Application application) {
        super(application);
        database = DatabaseClient.getInstance(application).getAppDatabase();
        transactionDao = database.transactionDao();

        // Set default date range (current month)
        setDateRange("Bulan Ini");
    }

    // LiveData getters
    public LiveData<List<Transaction>> getTransactions() {
        return transactions;
    }

    public LiveData<Double> getTotalIncome() {
        return totalIncome;
    }

    public LiveData<Double> getTotalExpense() {
        return totalExpense;
    }

    public LiveData<Double> getBalance() {
        return balance;
    }

    public LiveData<String> getPeriod() {
        return period;
    }

    public LiveData<Date> getStartDate() {
        return startDate;
    }

    public LiveData<Date> getEndDate() {
        return endDate;
    }

    // Format currency
    public String formatCurrency(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        return format.format(amount);
    }

    // Load all data
    public void loadData() {
        executor.execute(() -> {
            try {
                Date start = startDate.getValue();
                Date end = endDate.getValue();

                if (start != null && end != null) {
                    // Load transactions
                    List<Transaction> transactionList = transactionDao.getByDateRange(start, end);
                    transactions.postValue(transactionList);

                    // Calculate totals
                    double income = transactionDao.getTotalIncome(start, end);
                    double expense = transactionDao.getTotalExpense(start, end);
                    double bal = income - expense;

                    totalIncome.postValue(income);
                    totalExpense.postValue(expense);
                    balance.postValue(bal);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // Set date range and reload data
    public void setDateRange(String periodType) {
        executor.execute(() -> {
            Calendar cal = Calendar.getInstance();
            Date end = cal.getTime();

            Calendar startCal = Calendar.getInstance();

            switch (periodType) {
                case "Hari Ini":
                    startCal.set(Calendar.HOUR_OF_DAY, 0);
                    startCal.set(Calendar.MINUTE, 0);
                    startCal.set(Calendar.SECOND, 0);
                    startCal.set(Calendar.MILLISECOND, 0);
                    break;
                case "Minggu Ini":
                    startCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                    break;
                case "Bulan Ini":
                    startCal.set(Calendar.DAY_OF_MONTH, 1);
                    break;
                case "Tahun Ini":
                    startCal.set(Calendar.MONTH, Calendar.JANUARY);
                    startCal.set(Calendar.DAY_OF_MONTH, 1);
                    break;
                case "Semua":
                    startCal.set(1970, Calendar.JANUARY, 1);
                    break;
            }

            Date start = startCal.getTime();

            startDate.postValue(start);
            endDate.postValue(end);
            period.postValue(periodType);

            // Reload data dengan range baru
            loadDataWithRange(start, end);
        });
    }

    private void loadDataWithRange(Date start, Date end) {
        executor.execute(() -> {
            try {
                // Load transactions
                List<Transaction> transactionList = transactionDao.getByDateRange(start, end);
                transactions.postValue(transactionList);

                // Calculate totals
                double income = transactionDao.getTotalIncome(start, end);
                double expense = transactionDao.getTotalExpense(start, end);
                double bal = income - expense;

                totalIncome.postValue(income);
                totalExpense.postValue(expense);
                balance.postValue(bal);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // Add new transaction
    public void addTransaction(Transaction transaction) {
        executor.execute(() -> {
            try {
                transactionDao.insert(transaction);
                loadData(); // Reload data setelah insert
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // Delete transaction
    public void deleteTransaction(Transaction transaction) {
        executor.execute(() -> {
            try {
                transactionDao.delete(transaction);
                loadData(); // Reload data setelah delete
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // Update transaction
    public void updateTransaction(Transaction transaction) {
        executor.execute(() -> {
            try {
                transactionDao.update(transaction);
                loadData(); // Reload data setelah update
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // Clear all data
    public void clearAllData() {
        executor.execute(() -> {
            try {
                transactionDao.deleteAll();
                loadData(); // Reload data (akan kosong)
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // Get transaction by ID
    public LiveData<Transaction> getTransactionById(String id) {
        MutableLiveData<Transaction> result = new MutableLiveData<>();
        executor.execute(() -> {
            Transaction transaction = transactionDao.getById(id);
            result.postValue(transaction);
        });
        return result;
    }

    // Search transactions
    public void searchTransactions(String query) {
        executor.execute(() -> {
            try {
                // Implement search logic
                List<Transaction> allTransactions = transactionDao.getAll();
                List<Transaction> filtered = allTransactions.stream()
                        .filter(t -> t.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                                t.getCategory().toLowerCase().contains(query.toLowerCase()) ||
                                (t.getDescription() != null &&
                                        t.getDescription().toLowerCase().contains(query.toLowerCase())))
                        .collect(java.util.stream.Collectors.toList());
                transactions.postValue(filtered);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}