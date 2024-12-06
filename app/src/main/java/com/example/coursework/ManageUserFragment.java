package com.example.coursework;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.coursework.Adapter.UserAdapter;
import com.example.coursework.DAO.DatabaseHelper;
import com.example.coursework.DAO.UserDAO;
import com.example.coursework.Model.User;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

public class ManageUserFragment extends Fragment {
    private RecyclerView recyclerView;
    private UserDAO userDAO;
    private UserAdapter userAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_user, container, false);

        MaterialToolbar topAppBar = view.findViewById(R.id.topAppBarManageUser);
        topAppBar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        recyclerView = view.findViewById(R.id.recyclerViewUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        userDAO = new UserDAO(new DatabaseHelper(requireContext()).getReadableDatabase(), requireContext());
        List<User> pendingUsers = userDAO.getUsersByRole("Pending");

        userAdapter = new UserAdapter(pendingUsers, requireContext(), userDAO);
        recyclerView.setAdapter(userAdapter);

        return view;
    }
}
