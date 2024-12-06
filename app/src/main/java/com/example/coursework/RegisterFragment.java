package com.example.coursework;

import android.os.Bundle;
import android.util.Patterns;
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
import com.google.android.material.textfield.TextInputLayout;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class RegisterFragment extends Fragment {
    private TextInputEditText inputEmail, inputUserName, inputPassword, inputRePassword;
    private TextInputLayout inputEmailLayout, inputUserNameLayout, inputPasswordLayout, inputRePasswordLayout;
    private MaterialButton btnRegister;
    private DatabaseHelper dbHelper;
    private UserDAO userDAO;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        inputEmail = view.findViewById(R.id.inputEmail);
        inputUserName = view.findViewById(R.id.inputUserName);
        inputPassword = view.findViewById(R.id.inputPassword);
        inputRePassword = view.findViewById(R.id.inputRePassword);

        inputEmailLayout = view.findViewById(R.id.inputEmailLayout);
        inputUserNameLayout = view.findViewById(R.id.inputUserNameLayout);
        inputPasswordLayout = view.findViewById(R.id.inputPasswordLayout);
        inputRePasswordLayout = view.findViewById(R.id.inputRePasswordLayout);

        btnRegister = view.findViewById(R.id.btnRegister);

        dbHelper = new DatabaseHelper(getActivity());
        userDAO = new UserDAO(dbHelper.getWritableDatabase(), getActivity());

        btnRegister.setOnClickListener(v -> validateAndRegister());

        return view;
    }

    private void validateAndRegister(){
        String email = inputEmail.getText().toString().trim();
        String username = inputUserName.getText().toString().trim();
        String password = inputPassword.getText().toString();
        String rePassword = inputRePassword.getText().toString();

        boolean isValid = true;

        // Reset error messages
        inputEmailLayout.setError(null);
        inputUserNameLayout.setError(null);
        inputPasswordLayout.setError(null);
        inputRePasswordLayout.setError(null);

        // Email validation
        if (email.isEmpty()) {
            inputEmailLayout.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmailLayout.setError("Invalid email format");
            isValid = false;
        } else if (userDAO.isEmailExists(email)) {
            inputEmailLayout.setError("Email is already registered");
            isValid = false;
        }

        // Username validation
        if (username.isEmpty()) {
            inputUserNameLayout.setError("Username is required");
            isValid = false;
        } else if (userDAO.isUserNameExists(username)) {
            inputUserNameLayout.setError("Username already exists");
            isValid = false;
        }

        // Password validation
        if (password.isEmpty()) {
            inputPasswordLayout.setError("Password is required");
            isValid = false;
        } else if (password.length() < 8 || !password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*") || !password.matches(".*[0-9].*") || !password.matches(".*[!@#$%^&*+=?/_~()-].*")) {
            inputPasswordLayout.setError("Password must be at least 8 characters long, contain an uppercase letter, a lowercase letter, a digit, and a special character");
            isValid = false;
        }

        // Re-password validation
        if (rePassword.isEmpty()) {
            inputRePasswordLayout.setError("Please confirm your password");
            isValid = false;
        } else if (!rePassword.equals(password)) {
            inputRePasswordLayout.setError("Passwords do not match");
            isValid = false;
        }

        // If all inputs are valid
        if (isValid) {
            String hashedPassword = userDAO.hashPassword(password);
            User newUser = new User(null, email, hashedPassword, "Pending", username);
            userDAO.insertUser(newUser);

            Toast.makeText(getActivity(), "Registration successful!", Toast.LENGTH_SHORT).show();
            ((LoginActivity) getActivity()).setCurrentTab(0);
        }
    }
}

