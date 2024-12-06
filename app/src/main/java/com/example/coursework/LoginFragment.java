package com.example.coursework;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.coursework.DAO.DatabaseHelper;
import com.example.coursework.DAO.UserDAO;
import com.example.coursework.Model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginFragment extends Fragment {
    private TextInputEditText inputEmail, inputPassword;
    private MaterialButton btnLogin;
    private DatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        inputEmail = view.findViewById(R.id.inputEmail);
        inputPassword = view.findViewById(R.id.inputPassword);
        btnLogin = view.findViewById(R.id.btnLogin);

        dbHelper = new DatabaseHelper(getActivity());
        UserDAO userDAO = new UserDAO(dbHelper.getWritableDatabase(), getActivity());

        btnLogin.setOnClickListener(v -> {
            String email = inputEmail.getText().toString();
            String password = inputPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getActivity(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else  if (userDAO.authenticateUser(email, password)) {
                SharedPreferences sharedPref = getActivity().getSharedPreferences("LoginSession", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                User user = userDAO.getUserByEmail(email);
                editor.putString("userName", user.getUserName());
                editor.putString("userEmail", email);
                editor.putBoolean("isLoggedIn", true);
                editor.apply();

                Toast.makeText(getActivity(), "Login successful", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(getActivity(), MainActivity.class);
                startActivity(intent);
                getActivity().finish();
            } else {
                Toast.makeText(getActivity(), "Invalid login credentials", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}
