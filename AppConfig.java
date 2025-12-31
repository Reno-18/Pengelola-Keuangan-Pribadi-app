package com.example.personal_finance_manager.config;

import com.example.personal_finance_manager.model.Category;

import java.util.Arrays;
import java.util.List;

public class AppConfig {

    // Predefined categories with colors
    public static List<Category> getIncomeCategories() {
        return Arrays.asList(
                new Category("Gaji", "income", android.R.drawable.ic_menu_month),
                new Category("Bonus", "income", android.R.drawable.ic_menu_agenda),
                new Category("Investasi", "income", android.R.drawable.ic_menu_share),
                new Category("Freelance", "income", android.R.drawable.ic_menu_edit),
                new Category("Hadiah", "income", android.R.drawable.ic_menu_gallery),
                new Category("Penjualan", "income", android.R.drawable.ic_menu_save),
                new Category("Lainnya", "income", android.R.drawable.ic_menu_more)
        );
    }

    public static List<Category> getExpenseCategories() {
        return Arrays.asList(
                new Category("Makanan", "expense", android.R.drawable.ic_lock_lock),
                new Category("Transportasi", "expense", android.R.drawable.ic_menu_directions),
                new Category("Hiburan", "expense", android.R.drawable.ic_menu_slideshow),
                new Category("Belanja", "expense", android.R.drawable.ic_menu_my_calendar),
                new Category("Kesehatan", "expense", android.R.drawable.ic_menu_myplaces),
                new Category("Pendidikan", "expense", android.R.drawable.ic_menu_edit),
                new Category("Tagihan", "expense", android.R.drawable.ic_menu_set_as),
                new Category("Pulsa/Internet", "expense", android.R.drawable.ic_menu_call),
                new Category("Pajak", "expense", android.R.drawable.ic_menu_manage),
                new Category("Donasi", "expense", android.R.drawable.ic_menu_help),
                new Category("Lainnya", "expense", android.R.drawable.ic_menu_more)
        );
    }

    // Get all categories
    public static List<Category> getAllCategories() {
        List<Category> allCategories = getIncomeCategories();
        allCategories.addAll(getExpenseCategories());
        return allCategories;
    }

    // Get category by name
    public static Category getCategoryByName(String name) {
        for (Category category : getAllCategories()) {
            if (category.getName().equals(name)) {
                return category;
            }
        }
        return new Category("Lainnya", "expense", android.R.drawable.ic_menu_more);
    }

    // Date format patterns
    public static final String DATE_DISPLAY_FORMAT = "dd/MM/yyyy";
    public static final String DATE_DB_FORMAT = "yyyy-MM-dd";
    public static final String DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm";
    public static final String CURRENCY_FORMAT = "Rp #,##0";
}