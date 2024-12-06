package com.example.coursework;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.example.coursework.DAO.CartDAO;
import com.example.coursework.DAO.ClassDAO;
import com.example.coursework.DAO.CourseDAO;
import com.example.coursework.DAO.DatabaseHelper;
import com.example.coursework.DAO.OrderDAO;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

public class SettingFragment extends Fragment {
    private MaterialButton btnManageAccout, btnResetDatabase, btnLogout, btnUpdateUser, btnUpdatePassword;

    private OrderDAO orderDAO;
    private ClassDAO classDAO;
    private CourseDAO courseDAO;
    private CartDAO cartDAO;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View view =  inflater.inflate(R.layout.fragment_setting, container, false);

        btnLogout = view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).logout();
            }
        });

        MaterialTextView tvUserName = view.findViewById(R.id.tvUserName);
        tvUserName.setText(getUserName());

        btnManageAccout = view.findViewById(R.id.btnManageAccount);
        btnManageAccout.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).loadFragment(new ManageUserFragment());
        });

        btnUpdateUser = view.findViewById(R.id.btnUpdateUser);
        btnUpdateUser.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).loadFragment(new UpdateUserFragment());
        });

        btnUpdatePassword = view.findViewById(R.id.btnUpdatePassword);
        btnUpdatePassword.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).loadFragment(new UpdatePasswordFragment());
        });

        DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
        orderDAO = new OrderDAO(dbHelper.getWritableDatabase(), requireContext());
        classDAO = new ClassDAO(dbHelper.getWritableDatabase(), requireContext());
        classDAO.syncClassesFromFirestoreToSQLite();
        courseDAO = new CourseDAO(dbHelper.getWritableDatabase(), requireContext());
        cartDAO = new CartDAO(dbHelper.getWritableDatabase(), requireContext());

        btnResetDatabase = view.findViewById(R.id.btnResetDatabase);
        btnResetDatabase.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                showResetConfirmationDialog();
            } else {
                Toast.makeText(requireContext(), "Network connection is required to reset the database", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void showResetConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_reset, null);
        EditText inputConfirm = dialogView.findViewById(R.id.inputConfirm);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        btnConfirm.setEnabled(false);

        inputConfirm.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isNetworkAvailable()){
                    btnConfirm.setEnabled("database".equals(s.toString().trim()));
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        AlertDialog dialog = builder.setView(dialogView).create();

        btnConfirm.setOnClickListener(v -> {
            resetDatabase();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    public String getUserName() {
        SharedPreferences sharedPref = requireContext().getSharedPreferences("LoginSession", Context.MODE_PRIVATE);
        return sharedPref.getString("userName", "");
    }

    private void resetDatabase() {
        cartDAO.deleteAllCartItems();
        orderDAO.deleteAllOrders();
        classDAO.deleteAllClasses();
        courseDAO.deleteAllCourses();
        Toast.makeText(requireContext(), "Database reset successfully", Toast.LENGTH_SHORT).show();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}