package com.example.personal_finance_manager.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.example.personal_finance_manager.config.SupabaseConfig;
import com.example.personal_finance_manager.database.DatabaseClient;
import com.example.personal_finance_manager.database.dao.TransactionDao;
import com.example.personal_finance_manager.model.Transaction;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseService {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();
    private static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());

    public interface SyncCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface FetchCallback {
        void onSuccess(List<Transaction> transactions);
        void onError(String message);
    }

    public static void syncTransactions(Context context, List<Transaction> transactions, SyncCallback callback) {
        executor.execute(() -> {
            try {
                boolean allSynced = true;

                for (Transaction transaction : transactions) {
                    if (!transaction.isSynced()) {
                        JSONObject json = new JSONObject();
                        try {
                            json.put("id", transaction.getId());
                            json.put("user_id", "user_" + android.provider.Settings.Secure.getString(
                                    context.getContentResolver(),
                                    android.provider.Settings.Secure.ANDROID_ID));
                            json.put("title", transaction.getTitle());
                            json.put("description", transaction.getDescription());
                            json.put("amount", transaction.getAmount());
                            json.put("type", transaction.getType());
                            json.put("category", transaction.getCategory());
                            json.put("date", dateFormat.format(transaction.getDate()));
                            json.put("created_at", dateFormat.format(transaction.getCreatedAt()));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        RequestBody body = RequestBody.create(
                                json.toString(),
                                MediaType.parse("application/json")
                        );

                        Request request = new Request.Builder()
                                .url(SupabaseConfig.TRANSACTIONS_ENDPOINT)
                                .post(body)
                                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY)
                                .addHeader("Content-Type", "application/json")
                                .addHeader("Prefer", "return=minimal")
                                .build();

                        try (Response response = client.newCall(request).execute()) {
                            if (response.isSuccessful()) {
                                transaction.setSynced(true);
                                DatabaseClient.getInstance(context)
                                        .getAppDatabase()
                                        .transactionDao()
                                        .update(transaction);
                            } else {
                                allSynced = false;
                            }
                        } catch (IOException e) {
                            allSynced = false;
                            e.printStackTrace();
                        }
                    }
                }

                final boolean finalAllSynced = allSynced;
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (finalAllSynced) {
                        callback.onSuccess();
                    } else {
                        callback.onError("Some transactions failed to sync");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError(e.getMessage()));
            }
        });
    }

    public static void fetchTransactions(Context context, Date startDate, Date endDate,
                                         FetchCallback callback) {
        executor.execute(() -> {
            try {
                String url = SupabaseConfig.TRANSACTIONS_ENDPOINT +
                        "?date=gte." + dateFormat.format(startDate) +
                        "&date=lte." + dateFormat.format(endDate) +
                        "&order=date.desc";

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String jsonResponse = response.body().string();
                        Type listType = new TypeToken<List<Transaction>>(){}.getType();
                        List<Transaction> transactions = gson.fromJson(jsonResponse, listType);

                        // Mark as synced and save to local database
                        TransactionDao dao = DatabaseClient.getInstance(context)
                                .getAppDatabase()
                                .transactionDao();

                        for (Transaction t : transactions) {
                            t.setSynced(true);
                            Transaction existing = dao.getById(t.getId());
                            if (existing == null) {
                                dao.insert(t);
                            }
                        }

                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onSuccess(transactions));
                    } else {
                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onError("Failed to fetch: " + response.code()));
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError(e.getMessage()));
            }
        });
    }

    public static void checkConnection(Context context, ConnectionCallback callback) {
        executor.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(SupabaseConfig.SUPABASE_URL + "/rest/v1/")
                        .head()
                        .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    final boolean isConnected = response.isSuccessful();
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onResult(isConnected));
                }
            } catch (IOException e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onResult(false));
            }
        });
    }

    public interface ConnectionCallback {
        void onResult(boolean isConnected);
    }
}