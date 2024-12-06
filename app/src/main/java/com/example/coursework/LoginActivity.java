package com.example.coursework;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.coursework.Adapter.LoginPagerAdapter;
import com.example.coursework.DAO.ClassTypeDAO;
import com.example.coursework.DAO.DatabaseHelper;
import com.example.coursework.DAO.UserDAO;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class LoginActivity extends AppCompatActivity {
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private UserDAO userDAO;
    private ClassTypeDAO classTypeDAO;
    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        userDAO = new UserDAO(dbHelper.getWritableDatabase(), this);
        classTypeDAO = new ClassTypeDAO(dbHelper.getWritableDatabase(), this);

        if (isNetworkAvailable()) {
            classTypeDAO.initializeDefaultClassTypes();
            userDAO.syncUsersFromFirestoreToSQLite();
        } else {
            Toast.makeText(this, "Internet is not available, account sync from cloud will be delayed", Toast.LENGTH_LONG).show();
        }

        networkChangeReceiver = new NetworkChangeReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, filter);

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        LoginPagerAdapter adapter = new LoginPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                switch (position) {
                    case 0:
                        tab.setText("Login");
                        break;
                    case 1:
                        tab.setText("Register");
                        break;
                }
            }
        }).attach();
    }

    public void setCurrentTab(int tabIndex) {
        if (viewPager != null) {
            viewPager.setCurrentItem(tabIndex, true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkChangeReceiver != null) {
            unregisterReceiver(networkChangeReceiver);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private class NetworkChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isNetworkAvailable()) {
                Toast.makeText(context, "Network is available. Syncing users.", Toast.LENGTH_SHORT).show();
                userDAO.syncUsersFromFirestoreToSQLite();
                classTypeDAO.initializeDefaultClassTypes();
            }
        }
    }
}
