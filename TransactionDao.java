package com.example.personal_finance_manager.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.example.personal_finance_manager.model.Transaction;

import java.util.List;
import java.util.Date;

@Dao
public interface TransactionDao {
    @Insert
    long insert(Transaction transaction);

    @Update
    void update(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    @Query("SELECT * FROM 'transaction' ORDER BY date DESC")
    List<Transaction> getAll();

    @Query("SELECT * FROM 'transaction' WHERE type = :type ORDER BY date DESC")
    List<Transaction> getByType(String type);

    @Query("SELECT * FROM 'transaction' WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    List<Transaction> getByDateRange(Date start, Date end);

    @Query("SELECT * FROM 'transaction' WHERE isSynced = 0")
    List<Transaction> getUnsynced();

    @Query("SELECT SUM(amount) FROM 'transaction' WHERE type = 'income' AND date BETWEEN :start AND :end")
    double getTotalIncome(Date start, Date end);

    @Query("SELECT SUM(amount) FROM 'transaction' WHERE type = 'expense' AND date BETWEEN :start AND :end")
    double getTotalExpense(Date start, Date end);

    @Query("SELECT SUM(amount) FROM 'transaction' WHERE type = 'income'")
    double getTotalIncome();

    @Query("SELECT SUM(amount) FROM 'transaction' WHERE type = 'expense'")
    double getTotalExpense();

    @Query("SELECT category, SUM(amount) as total FROM 'transaction' WHERE type = 'expense' AND date BETWEEN :start AND :end GROUP BY category")
    List<CategorySummary> getExpenseByCategory(Date start, Date end);

    @Query("SELECT * FROM 'transaction' WHERE id = :id")
    Transaction getById(String id);

    @Query("DELETE FROM 'transaction'")
    void deleteAll();

    static class CategorySummary {
        public String category;
        public double total;
    }
}