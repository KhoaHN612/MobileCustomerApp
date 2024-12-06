package com.example.coursework;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class UpdateUserFragment extends Fragment {
    private TextInputEditText inputUserName;
    private MaterialButton btnUpdateUser;
    private UserDAO userDAO;
    private String userEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_update_user, container, false);

        MaterialToolbar topAppBar = view.findViewById(R.id.topAppBarUpdateUser);
        topAppBar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        inputUserName = view.findViewById(R.id.inputUserName);
        btnUpdateUser = view.findViewById(R.id.btnUpdateUser);

        userDAO = new UserDAO(new DatabaseHelper(requireContext()).getWritableDatabase(), requireContext());

        SharedPreferences sharedPref = requireContext().getSharedPreferences("LoginSession", Context.MODE_PRIVATE);
        userEmail = sharedPref.getString("userEmail", "");

        User user = userDAO.getUserByEmail(userEmail);
        if (user != null) {
            inputUserName.setText(user.getUserName());
        } else {
            Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        }

        btnUpdateUser.setOnClickListener(v -> {
            String newUserName = inputUserName.getText().toString().trim();
            if (newUserName.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a valid username", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isNetworkAvailable()) {
                Toast.makeText(getContext(), "Network connection required to update username", Toast.LENGTH_SHORT).show();
                return;
            }

            userDAO.isUserNameExistOnFirestore(newUserName, new UserDAO.FirestoreUserCallback() {
                @Override
                public void onCallback(boolean exists) {
                    if (exists) {
                        Toast.makeText(getContext(), "Username already taken, please choose a different one", Toast.LENGTH_SHORT).show();
                    } else {
                        user.setUserName(newUserName);
                        userDAO.updateUser(user);

                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString("userName", newUserName);
                        editor.apply();

                        Toast.makeText(getContext(), "User updated successfully", Toast.LENGTH_SHORT).show();
                        if (getActivity() != null) {
                            getActivity().onBackPressed();
                        }
                    }
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(getContext(), "Error checking username availability", Toast.LENGTH_SHORT).show();
                }
            });
        });

        return view;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
