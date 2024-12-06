package com.example.coursework.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.coursework.DAO.UserDAO;
import com.example.coursework.Model.User;
import com.example.coursework.R;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<User> userList;
    private Context context;
    private UserDAO userDAO;

    public UserAdapter(List<User> userList, Context context, UserDAO userDAO) {
        this.userList = userList;
        this.context = context;
        this.userDAO = userDAO;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_item, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.tvUserName.setText(user.getUserName());
        holder.tvEmail.setText(user.getEmail());

        holder.btnConfirm.setOnClickListener(v -> {
            user.setRole("Admin");
            userDAO.updateUserRole(user.getId(), "Admin");
            Toast.makeText(context, "User confirmed as Admin", Toast.LENGTH_SHORT).show();
            userList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, userList.size());
        });

        holder.btnDelete.setOnClickListener(v -> {
            userDAO.deleteUser(user.getId());
            Toast.makeText(context, "User deleted", Toast.LENGTH_SHORT).show();
            userList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, userList.size());
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvEmail;
        MaterialButton btnConfirm, btnDelete;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            btnConfirm = itemView.findViewById(R.id.btnConfirm);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
