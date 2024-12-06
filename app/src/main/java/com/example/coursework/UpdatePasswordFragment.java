package com.example.coursework;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class UpdatePasswordFragment extends Fragment {

    private TextInputEditText inputOldPassword, inputNewPassword, inputConfirmNewPassword;
    private TextInputLayout inputOldPasswordLayout, inputNewPasswordLayout, inputConfirmNewPasswordLayout;
    private MaterialButton btnUpdatePassword;
    private UserDAO userDAO;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_update_password, container, false);

        MaterialToolbar topAppBar = view.findViewById(R.id.topAppBarUpdatePassword);
        topAppBar.setNavigationOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        inputOldPassword = view.findViewById(R.id.inputOldPassword);
        inputNewPassword = view.findViewById(R.id.inputNewPassword);
        inputConfirmNewPassword = view.findViewById(R.id.inputConfirmNewPassword);

        inputOldPasswordLayout = view.findViewById(R.id.inputOldPasswordLayout);
        inputNewPasswordLayout = view.findViewById(R.id.inputNewPasswordLayout);
        inputConfirmNewPasswordLayout = view.findViewById(R.id.inputConfirmNewPasswordLayout);

        btnUpdatePassword = view.findViewById(R.id.btnUpdatePassword);

        // Initialize UserDAO
        DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
        userDAO = new UserDAO(dbHelper.getWritableDatabase(), getActivity());

        // Retrieve current user from SharedPreferences
        SharedPreferences sharedPref = requireContext().getSharedPreferences("LoginSession", Context.MODE_PRIVATE);
        String userEmail = sharedPref.getString("userEmail", null);
        currentUser = userDAO.getUserByEmail(userEmail);

        btnUpdatePassword.setOnClickListener(v -> validateAndUpdatePassword());

        return view;
    }

    private void validateAndUpdatePassword() {
        String oldPassword = inputOldPassword.getText().toString().trim();
        String newPassword = inputNewPassword.getText().toString().trim();
        String confirmNewPassword = inputConfirmNewPassword.getText().toString().trim();

        boolean isValid = true;

        // Reset error messages
        inputOldPasswordLayout.setError(null);
        inputNewPasswordLayout.setError(null);
        inputConfirmNewPasswordLayout.setError(null);

        // Validate old password
        if (TextUtils.isEmpty(oldPassword)) {
            inputOldPasswordLayout.setError("Old password is required");
            isValid = false;
        } else if (!userDAO.authenticateUser(currentUser.getEmail(), oldPassword)) {
            inputOldPasswordLayout.setError("Old password is incorrect");
            isValid = false;
        }

        // Validate new password
        if (TextUtils.isEmpty(newPassword)) {
            inputNewPasswordLayout.setError("New password is required");
            isValid = false;
        } else if (newPassword.length() < 8 || !newPassword.matches(".*[A-Z].*") || !newPassword.matches(".*[a-z].*") || !newPassword.matches(".*[0-9].*") || !newPassword.matches(".*[!@#$%^&*+=?/_~()-].*")) {
            inputNewPasswordLayout.setError("Password must be at least 8 characters long, contain an uppercase letter, a lowercase letter, a digit, and a special character");
            isValid = false;
        }

        // Validate confirm new password
        if (TextUtils.isEmpty(confirmNewPassword)) {
            inputConfirmNewPasswordLayout.setError("Please confirm your new password");
            isValid = false;
        } else if (!confirmNewPassword.equals(newPassword)) {
            inputConfirmNewPasswordLayout.setError("Passwords do not match");
            isValid = false;
        }

        // If all inputs are valid, update the password
        if (isValid) {
            String hashedNewPassword = userDAO.hashPassword(newPassword);
            currentUser.setPassword(hashedNewPassword);
            userDAO.updateUser(currentUser);

            Toast.makeText(getActivity(), "Password updated successfully!", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
        }
    }
}
