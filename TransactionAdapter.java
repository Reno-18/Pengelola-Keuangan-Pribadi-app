package com.example.personal_finance_manager.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personal_finance_manager.R;
import com.example.personal_finance_manager.model.Transaction;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<Transaction> transactions;
    private OnItemClickListener listener;
    private Context context;

    public TransactionAdapter(Context context) {
        this.context = context;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);

        // Set data
        holder.tvTitle.setText(transaction.getTitle());
        holder.tvCategory.setText(transaction.getCategory());

        // Format date
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        holder.tvDate.setText(sdf.format(transaction.getDate()));

        // Format amount
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        String amount = format.format(transaction.getAmount());

        if ("income".equals(transaction.getType())) {
            holder.tvAmount.setText("+" + amount);
            holder.tvAmount.setTextColor(Color.parseColor("#4CAF50"));
            holder.icon.setImageResource(R.drawable.ic_income);
        } else {
            holder.tvAmount.setText("-" + amount);
            holder.tvAmount.setTextColor(Color.parseColor("#F44336"));
            holder.icon.setImageResource(R.drawable.ic_expense);
        }

        // Set description if exists
        if (transaction.getDescription() != null && !transaction.getDescription().isEmpty()) {
            holder.tvDescription.setText(transaction.getDescription());
            holder.tvDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }

        // Sync status indicator
        if (transaction.isSynced()) {
            holder.syncIndicator.setBackgroundColor(Color.parseColor("#4CAF50"));
        } else {
            holder.syncIndicator.setBackgroundColor(Color.parseColor("#FF9800"));
        }

        // Click listener
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(transaction);
            }
        });

        holder.cardView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onItemLongClick(transaction);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return transactions != null ? transactions.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView icon;
        TextView tvTitle, tvCategory, tvDate, tvAmount, tvDescription;
        View syncIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            icon = itemView.findViewById(R.id.icon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            syncIndicator = itemView.findViewById(R.id.syncIndicator);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Transaction transaction);
        void onItemLongClick(Transaction transaction);
    }
}