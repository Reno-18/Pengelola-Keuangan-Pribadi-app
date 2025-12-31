package com.example.personal_finance_manager.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.personal_finance_manager.model.Transaction;
import com.example.personal_finance_manager.database.dao.TransactionDao;

@Database(entities = {Transaction.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract TransactionDao transactionDao();
}