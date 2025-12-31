package com.example.personal_finance_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.personal_finance_manager.model.ExportStatus;
import com.example.personal_finance_manager.repository.ExportRepository;

import java.util.Date;

public class ExportViewModel extends AndroidViewModel {

    private final ExportRepository exportRepository;
    private final MutableLiveData<Date> startDate = new MutableLiveData<>();
    private final MutableLiveData<Date> endDate = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isExporting = new MutableLiveData<>(false);

    public ExportViewModel(@NonNull Application application) {
        super(application);
        exportRepository = ExportRepository.getInstance();

        // Set default date range (last month)
        Date end = new Date();
        startDate.setValue(getStartOfMonth(end));
        endDate.setValue(end);
    }

    public LiveData<ExportStatus> getExportStatus() {
        return exportRepository.getExportStatus();
    }

    public LiveData<Date> getStartDate() {
        return startDate;
    }

    public LiveData<Date> getEndDate() {
        return endDate;
    }

    public LiveData<Boolean> getIsExporting() {
        return isExporting;
    }

    public void setDateRange(Date start, Date end) {
        startDate.setValue(start);
        endDate.setValue(end);
    }

    public void startExport() {
        Date start = startDate.getValue();
        Date end = endDate.getValue();

        if (start != null && end != null) {
            isExporting.setValue(true);
            exportRepository.exportToWeb(getApplication(), start, end);
        }
    }

    public void resetExport() {
        exportRepository.resetExportStatus();
        isExporting.setValue(false);
    }

    private Date getStartOfMonth(Date date) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(date);
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        return cal.getTime();
    }
}