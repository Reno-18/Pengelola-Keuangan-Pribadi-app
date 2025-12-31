package com.example.personal_finance_manager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.personal_finance_manager.database.AppDatabase;
import com.example.personal_finance_manager.database.DatabaseClient;
import com.example.personal_finance_manager.database.dao.TransactionDao;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AnalysisActivity extends AppCompatActivity {

    private PieChart pieChart;
    private Spinner spinnerPeriod;
    private TextView tvTotalExpense, tvAverageExpense;
    private AppDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        database = DatabaseClient.getInstance(this).getAppDatabase();

        pieChart = findViewById(R.id.pieChart);
        spinnerPeriod = findViewById(R.id.spinnerPeriod);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvAverageExpense = findViewById(R.id.tvAverageExpense);

        setupSpinner();
        setupChart();
        loadData("Bulan Ini");
    }

    private void setupSpinner() {
        String[] periods = {"Hari Ini", "Minggu Ini", "Bulan Ini", "Tahun Ini", "Semua"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, periods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriod.setAdapter(adapter);

        spinnerPeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadData(parent.getItemAtPosition(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupChart() {
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setDrawEntryLabels(true);
        pieChart.setUsePercentValues(true);

        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(12f);
    }

    private void loadData(String period) {
        new Thread(() -> {
            Date[] dateRange = getDateRange(period);
            List<TransactionDao.CategorySummary> summaries =
                    database.transactionDao().getExpenseByCategory(dateRange[0], dateRange[1]);

            double totalExpense = database.transactionDao().getTotalExpense(dateRange[0], dateRange[1]);
            double averageExpense = calculateAverageExpense(period, totalExpense);

            runOnUiThread(() -> {
                updateChart(summaries);
                updateSummary(totalExpense, averageExpense);
            });
        }).start();
    }

    private Date[] getDateRange(String period) {
        Calendar cal = Calendar.getInstance();
        Date endDate = cal.getTime();

        Calendar startCal = Calendar.getInstance();

        switch (period) {
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

        return new Date[]{startCal.getTime(), endDate};
    }

    private double calculateAverageExpense(String period, double totalExpense) {
        Calendar cal = Calendar.getInstance();
        int days = 1;

        switch (period) {
            case "Hari Ini":
                days = 1;
                break;
            case "Minggu Ini":
                days = 7;
                break;
            case "Bulan Ini":
                days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                break;
            case "Tahun Ini":
                days = cal.getActualMaximum(Calendar.DAY_OF_YEAR);
                break;
            case "Semua":
                // For all time, use number of months since first transaction
                days = 30; // Default approximation
                break;
        }

        return totalExpense / days;
    }

    private void updateChart(List<TransactionDao.CategorySummary> summaries) {
        if (summaries.isEmpty()) {
            pieChart.clear();
            pieChart.setNoDataText("Tidak ada data pengeluaran");
            pieChart.setNoDataTextColor(Color.GRAY);
            return;
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (TransactionDao.CategorySummary summary : summaries) {
            entries.add(new PieEntry((float) summary.total, summary.category));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueFormatter(new PercentFormatter(pieChart));

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.invalidate();
    }

    private void updateSummary(double totalExpense, double averageExpense) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

        tvTotalExpense.setText(format.format(totalExpense));
        tvAverageExpense.setText(format.format(averageExpense) + "/hari");
    }
}