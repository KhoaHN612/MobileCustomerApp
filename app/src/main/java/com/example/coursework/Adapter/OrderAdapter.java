package com.example.coursework.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coursework.DAO.DatabaseHelper;
import com.example.coursework.DAO.UserDAO;
import com.example.coursework.Model.Order;
import com.example.coursework.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private Context context;
    private List<Order> orders;
    private DatabaseHelper db;
    private UserDAO userDAO;

    public OrderAdapter(DatabaseHelper db, Context context, List<Order> orders) {
        this.db = db;
        this.context = context;
        this.orders = orders;
        userDAO = new UserDAO(this.db.getWritableDatabase(), context);
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.order_item, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);
        String formattedDate = formatOrderDate(order.getOrderDate());
        holder.tvOrderDate.setText("Order Date: " + formattedDate);
        holder.tvUserEmail.setText("User Email: " + userDAO.getUserById(order.getUserId()).getEmail());
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderDate, tvUserEmail;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderDate = itemView.findViewById(R.id.tv_order_date);
            tvUserEmail = itemView.findViewById(R.id.tv_user_email);
        }
    }

    public static String formatOrderDate(String orderDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(orderDate);
            return outputFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return orderDate;
        }
    }
}
