package com.example.coursework;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.coursework.DAO.DatabaseHelper;
import com.example.coursework.Model.YogaClass;
import com.example.coursework.Model.YogaCourse;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class MainActivity extends BaseActivity {

    private DrawerLayout drawerLayout;
    private ImageButton menuIcon;
    private DatabaseHelper dbHelper;
    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        checkLoginSession();

        drawerLayout = findViewById(R.id.drawerLayout);
        menuIcon = findViewById(R.id.menuIcon);

        menuIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerOpen(findViewById(R.id.navigation_view))) {
                    drawerLayout.closeDrawer(findViewById(R.id.navigation_view));
                } else {
                    drawerLayout.openDrawer(findViewById(R.id.navigation_view));
                }
            }
        });

        setupBottomNavigationView();

        loadFragment(new CourseListFragment());
    }

    private void setupBottomNavigationView() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        switch (getClass().getSimpleName()) {
            case "HomeActivity":
                bottomNavigationView.setSelectedItemId(R.id.nav_home);
                break;
            case "ClassesActivity":
                bottomNavigationView.setSelectedItemId(R.id.nav_classes);
                break;
            case "ProfileActivity":
                bottomNavigationView.setSelectedItemId(R.id.nav_profile);
                break;
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = null;

            if (itemId == R.id.nav_home) {
                selectedFragment = new CourseListFragment();
            } else if (itemId == R.id.nav_classes) {
                selectedFragment = new AllClassesFragment();
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });
    }

    private void hideBottomNavigationView(){
        bottomNavigationView.setVisibility(View.INVISIBLE);
    }
    private void showBottomNavigationView(){
        bottomNavigationView.setVisibility(View.VISIBLE);
    }

    public void openCourseListFragment() {
        loadFragment(new CourseListFragment());
    }

    public void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.addToBackStack(null);
        transaction.commit();

        Log.d("FragmentTransaction", "Fragment loaded: " + fragment.getClass().getSimpleName());
    }

    public void receiveCourse(YogaCourse course) {
        openEditClassForCourseFragment(course);
    }

    public void openNewCourseFragment(){
        loadFragment(new AddEditCourseFragment());
    }

    public void openEditClassForCourseFragment(YogaCourse course) {
        AddEditClassFragment fragment = new AddEditClassFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("course", course);
        fragment.setArguments(bundle);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.addToBackStack(null);
        transaction.commit();

        Log.d("FragmentTransaction", "Fragment loaded: AddEditClass");
    }


    public void receiveClasses(List<YogaClass> classList) {
    }

    public void openEditCourseFragment(YogaCourse course) {
        AddEditCourseFragment fragment = new AddEditCourseFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("course", course);
        fragment.setArguments(bundle);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.addToBackStack(null);
        transaction.commit();

        Log.d("FragmentTransaction", "Fragment loaded: AddEditCourse for editing");
    }

    private void checkLoginSession() {
        SharedPreferences sharedPreferences = getSharedPreferences("LoginSession", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);

        if (!isLoggedIn) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    public void logout() {
        SharedPreferences sharedPref = getSharedPreferences("LoginSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.apply();

        Toast.makeText(this, "You have been logged out successfully.", Toast.LENGTH_SHORT).show();

        checkLoginSession();
    }
}