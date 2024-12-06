package com.example.coursework;

import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.coursework.Adapter.CourseAdapter;
import com.example.coursework.DAO.ClassTypeDAO;
import com.example.coursework.DAO.CourseDAO;
import com.example.coursework.DAO.DatabaseHelper;
import com.example.coursework.Model.ClassType;
import com.example.coursework.Model.YogaCourse;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class CourseListFragment extends Fragment {
    private Spinner spinnerFilterDay, spinnerFilterYogaType;
    private SearchView searchView;
    private List<YogaCourse> allCourses;
    private RecyclerView recyclerViewCourses;
    private CourseAdapter courseAdapter;
    private ArrayList<YogaCourse> courseList;

    private DatabaseHelper dbHelper;
    private CourseDAO courseDAO;
    private ClassTypeDAO classTypeDAO;
    private SwipeRefreshLayout swipeRefreshLayout;

    private Handler handler = new Handler(Looper.getMainLooper());;
    private Runnable periodicRefreshRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_course_list, container, false);

        spinnerFilterDay = view.findViewById(R.id.spinnerFilterDay);
        spinnerFilterYogaType = view.findViewById(R.id.spinnerFilterYogaType);
        searchView = view.findViewById(R.id.searchView);

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        recyclerViewCourses = view.findViewById(R.id.recyclerViewCourses);
        recyclerViewCourses.setLayoutManager(new LinearLayoutManager(getContext()));

        dbHelper = new DatabaseHelper(requireContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        courseDAO = new CourseDAO(db, requireContext());

        allCourses = courseDAO.getAllCourses();
        classTypeDAO = new ClassTypeDAO(db, requireContext());
        classTypeDAO.initializeDefaultClassTypes();
        courseList = new ArrayList<>(allCourses);

        courseAdapter = new CourseAdapter(requireContext(), courseList, classTypeDAO);
        courseAdapter.setOnCourseClickListener(course -> {
            ((MainActivity) requireActivity()).openEditCourseFragment(course);
        });
        recyclerViewCourses.setAdapter(courseAdapter);

        courseDAO.syncCoursesFromFirestoreToSQLite(v -> {
            Log.e("Sync", "Sync success");

            allCourses = courseDAO.getAllCourses();
            courseList = new ArrayList<>(allCourses);
            Log.e("Sync", allCourses.toArray().toString());
            courseAdapter = new CourseAdapter(requireContext(), courseList, classTypeDAO);
            courseAdapter.setOnCourseClickListener(course -> {
                ((MainActivity) requireActivity()).openEditCourseFragment(course);
            });
            recyclerViewCourses.setAdapter(courseAdapter);
            courseAdapter.notifyDataSetChanged();
        });

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            private Drawable icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_delete);
            private final ColorDrawable background = new ColorDrawable(ContextCompat.getColor(getContext(), R.color.md_theme_error));
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                YogaCourse courseToDelete = courseList.get(position);

                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Course")
                        .setMessage("Are you sure you want to delete this course? It and its child classes, as well as related orders, will be deleted.")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            courseList.remove(position);
                            courseAdapter.notifyItemRemoved(position);
                            courseDAO.deleteCourse(courseToDelete.getId());

                            Toast.makeText(requireContext(), "Course deleted successfully", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("No", (dialog, which) -> {
                            courseAdapter.notifyItemChanged(position);
                            dialog.dismiss();
                        })
                        .setCancelable(false)
                        .show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                View itemView = viewHolder.itemView;
                int backgroundCornerOffset = 20;

                if (dX > 0) { // Swiping to the right
                    int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                    int iconTop = itemView.getTop() + iconMargin;
                    int iconBottom = iconTop + icon.getIntrinsicHeight();
                    int iconLeft = itemView.getLeft() + iconMargin;
                    int iconRight = iconLeft + icon.getIntrinsicWidth();

                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    background.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + ((int) dX) + backgroundCornerOffset, itemView.getBottom());
                } else {
                    background.setBounds(0, 0, 0, 0);
                }

                background.draw(c);
                icon.draw(c);
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerViewCourses);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshCourses();
        });

        FloatingActionButton fabAddCourse = view.findViewById(R.id.fabAddCourse);
        fabAddCourse.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).openNewCourseFragment();
        });

        setupDaySpinner();
        setupYogaTypeSpinner();
        setupSearchView();

        return view;
    }

    private void setupDaySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.days_of_week, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterDay.setAdapter(adapter);
        spinnerFilterDay.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterCourses();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupYogaTypeSpinner() {
        List<ClassType> classTypes = classTypeDAO.getAllClassTypes();
        List<String> yogaTypeNames = new ArrayList<>();
        yogaTypeNames.add("All");
        for (ClassType type : classTypes) {
            yogaTypeNames.add(type.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, yogaTypeNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterYogaType.setAdapter(adapter);
        spinnerFilterYogaType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterCourses();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterCourses();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterCourses();
                return false;
            }
        });
    }

    private void filterCourses() {
        String selectedDay = spinnerFilterDay.getSelectedItem().toString();
        String selectedYogaType = spinnerFilterYogaType.getSelectedItem().toString();
        String query = searchView.getQuery().toString().toLowerCase();

        courseList.clear();
        for (YogaCourse course : allCourses) {
            boolean matchesDay = selectedDay.equals("All") || selectedDay.equals(course.getDayOfWeek());
            boolean matchesYogaType = selectedYogaType.equals("All") || selectedYogaType.equals(classTypeDAO.getClassTypeNameById(course.getClassTypeId()));
            boolean matchesQuery = course.getTime().toLowerCase().contains(query) ||
                    course.getStartDay().toLowerCase().contains(query) ||
                    String.valueOf(course.getPrice()).contains(query) || String.valueOf(course.getDuration()).contains(query);

            if (matchesDay && matchesYogaType && matchesQuery) {
                courseList.add(course);
            }
        }
        courseAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        courseList.clear();
        courseList = courseDAO.getAllCourses();
        courseAdapter = new CourseAdapter(requireContext(), courseList, classTypeDAO);
        courseAdapter.setOnCourseClickListener(course -> {
            ((MainActivity) requireActivity()).openEditCourseFragment(course);
        });
        recyclerViewCourses.setAdapter(courseAdapter);
    }

    private void refreshCourses() {
        courseList.clear();
        courseList = courseDAO.getAllCourses();
        courseAdapter = new CourseAdapter(requireContext(), courseList, classTypeDAO);
        courseAdapter.setOnCourseClickListener(course -> {
            ((MainActivity) requireActivity()).openEditCourseFragment(course);
        });
        recyclerViewCourses.setAdapter(courseAdapter);
        swipeRefreshLayout.setRefreshing(false);
    }
}
