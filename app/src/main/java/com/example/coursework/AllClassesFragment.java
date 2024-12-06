package com.example.coursework;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.text.TextUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coursework.DAO.ClassDAO;
import com.example.coursework.DAO.ClassTypeDAO;
import com.example.coursework.DAO.CourseDAO;
import com.example.coursework.Adapter.AllClassesAdapter;
import com.example.coursework.DAO.DatabaseHelper;
import com.example.coursework.DAO.OrderDAO;
import com.example.coursework.Model.ClassType;
import com.example.coursework.Model.YogaClass;
import com.example.coursework.Model.YogaCourse;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AllClassesFragment extends Fragment {

    private DatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private AllClassesAdapter adapter;
    private ClassDAO classDAO;
    private CourseDAO courseDAO;
    private ClassTypeDAO classTypeDAO;
    private List<YogaClass> yogaClasses;
    private List<YogaClass> filteredYogaClasses;

    private String currentQuery = "";
    private String selectedDayOfWeek = "All";
    private String selectedYogaType = "All";
    private Long startDate = null;
    private Long endDate = null;
    private Integer startTime = null;
    private Integer endTime = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_classes, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        dbHelper = new DatabaseHelper(requireContext());

        classDAO = new ClassDAO(dbHelper.getWritableDatabase(), requireContext());
        courseDAO = new CourseDAO(dbHelper.getWritableDatabase(), requireContext());
        classTypeDAO = new ClassTypeDAO(dbHelper.getWritableDatabase(), requireContext());

        OrderDAO orderDAO = new OrderDAO(dbHelper.getWritableDatabase(),requireContext());
        orderDAO.syncOrdersFromFirestoreToSQLite();

        yogaClasses = classDAO.getAllClasses();

        filteredYogaClasses = new ArrayList<>(yogaClasses);

        adapter = new AllClassesAdapter(requireContext(), filteredYogaClasses   , courseDAO, classTypeDAO);
        adapter.setOnItemClickListener(yogaClass -> {
            YogaClassDetailFragment fragment = new YogaClassDetailFragment();
            Bundle args = new Bundle();
            args.putSerializable("yoga_class", yogaClass);
            fragment.setArguments(args);
            ((MainActivity) requireActivity()).loadFragment(fragment);
        });

        recyclerView.setAdapter(adapter);

        classDAO.syncClassesFromFirestoreToSQLite(v -> {
            filteredYogaClasses = null;
            yogaClasses = classDAO.getAllClasses();
            filteredYogaClasses = new ArrayList<>(yogaClasses);
            adapter = new AllClassesAdapter(requireContext(), filteredYogaClasses   , courseDAO, classTypeDAO);

            adapter.setOnItemClickListener(yogaClass -> {
                YogaClassDetailFragment fragment = new YogaClassDetailFragment();
                Bundle args = new Bundle();
                args.putSerializable("yoga_class", yogaClass);
                fragment.setArguments(args);
                ((MainActivity) requireActivity()).loadFragment(fragment);
            });

            recyclerView.setAdapter(adapter);

            adapter.notifyDataSetChanged();
        });

        setupFilters(view);
        setupSearchView(view);
        setupDayOfWeekSpinner(view);
        setupYogaTypeSpinner(view);
        setupDateRangePicker(view);
        setupTimePickers(view);

        return view;
    }

    private void setupFilters(View view) {
        Button btnToggleFilters = view.findViewById(R.id.btn_toggle_filters);
        LinearLayout filtersLayout = view.findViewById(R.id.filters_layout);

        btnToggleFilters.setOnClickListener(v -> {
            if (filtersLayout.getVisibility() == View.GONE) {
                filtersLayout.setVisibility(View.VISIBLE);
                btnToggleFilters.setText("Hide Filters");
            } else {
                filtersLayout.setVisibility(View.GONE);
                btnToggleFilters.setText("Show Filters");
            }
        });

        Button btnResetFilters = view.findViewById(R.id.btn_reset_filters);
        btnResetFilters.setOnClickListener(v -> resetFilters());
    }

    private void resetFilters() {
        // Reset search query
        SearchView searchView = requireView().findViewById(R.id.search_view);
        searchView.setQuery("", false);
        currentQuery = "";

        // Reset spinners
        Spinner spinnerDayOfWeek = requireView().findViewById(R.id.spinner_day_of_week);
        Spinner spinnerYogaType = requireView().findViewById(R.id.spinner_yoga_type);
        spinnerDayOfWeek.setSelection(0);
        spinnerYogaType.setSelection(0);
        selectedDayOfWeek = "All";
        selectedYogaType = "All";

        startDate = null;
        endDate = null;
        Button btnDateRange = requireView().findViewById(R.id.btn_date_range);
        btnDateRange.setText("Select Date Range");

        startTime = null;
        endTime = null;
        Button btnStartTime = requireView().findViewById(R.id.btn_start_time);
        Button btnEndTime = requireView().findViewById(R.id.btn_end_time);
        btnStartTime.setText("Select Start Time");
        btnEndTime.setText("Select End Time");

        filterClasses();
    }

    private void setupSearchView(View view) {
        androidx.appcompat.widget.SearchView searchView = view.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentQuery = query;
                filterClasses();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText;
                filterClasses();
                return false;
            }
        });
    }

    private void setupDayOfWeekSpinner(View view) {
        Spinner spinnerDayOfWeek = view.findViewById(R.id.spinner_day_of_week);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.days_of_week, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDayOfWeek.setAdapter(adapter);
        spinnerDayOfWeek.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDayOfWeek = parent.getItemAtPosition(position).toString();
                filterClasses();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedDayOfWeek = "All";
                filterClasses();
            }
        });
    }

    private void setupYogaTypeSpinner(View view) {
        Spinner spinnerYogaType = view.findViewById(R.id.spinner_yoga_type);
        List<ClassType> classTypes = classTypeDAO.getAllClassTypes();
        List<String> yogaTypeNames = new ArrayList<>();
        yogaTypeNames.add("All");
        for (ClassType type : classTypes) {
            yogaTypeNames.add(type.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, yogaTypeNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYogaType.setAdapter(adapter);
        spinnerYogaType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedYogaType = parent.getItemAtPosition(position).toString();
                filterClasses();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedYogaType = "All";
                filterClasses();
            }
        });
    }

    private void setupDateRangePicker(View view) {
        Button btnDateRange = view.findViewById(R.id.btn_date_range);
        btnDateRange.setOnClickListener(v -> {
            MaterialDatePicker<Pair<Long, Long>> dateRangePicker = MaterialDatePicker.Builder
                    .dateRangePicker()
                    .setTitleText("Select Date Range")
                    .build();

            dateRangePicker.addOnPositiveButtonClickListener(selection -> {
                if (selection != null) {
                    startDate = selection.first;
                    endDate = selection.second;

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    String startDateText = sdf.format(new Date(startDate));
                    String endDateText = sdf.format(new Date(endDate));
                    btnDateRange.setText(String.format("From: %s To: %s", startDateText, endDateText));

                    filterClasses();
                }
            });

            dateRangePicker.show(getParentFragmentManager(), "DATE_RANGE_PICKER");
        });
    }

    private void setupTimePickers(View view) {
        Button btnStartTime = view.findViewById(R.id.btn_start_time);
        btnStartTime.setOnClickListener(v -> {
            MaterialTimePicker picker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setTitleText("Select Start Time")
                    .build();

            picker.addOnPositiveButtonClickListener(dialog -> {
                startTime = picker.getHour() * 60 + picker.getMinute();
                String formattedStartTime = String.format(Locale.getDefault(), "%02d:%02d", picker.getHour(), picker.getMinute());
                btnStartTime.setText(String.format("Start: %s", formattedStartTime));
                filterClasses();
            });

            picker.show(getParentFragmentManager(), "START_TIME_PICKER");
        });

        Button btnEndTime = view.findViewById(R.id.btn_end_time);
        btnEndTime.setOnClickListener(v -> {
            MaterialTimePicker picker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setTitleText("Select End Time")
                    .build();

            picker.addOnPositiveButtonClickListener(dialog -> {
                endTime = picker.getHour() * 60 + picker.getMinute();
                String formattedEndTime = String.format(Locale.getDefault(), "%02d:%02d", picker.getHour(), picker.getMinute());
                btnEndTime.setText(String.format("End: %s", formattedEndTime));
                filterClasses();
            });

            picker.show(getParentFragmentManager(), "END_TIME_PICKER");
        });
    }

    private void filterClasses() {
        filteredYogaClasses.clear();
        for (YogaClass yogaClass : yogaClasses) {
            YogaCourse course = courseDAO.getCourseById(yogaClass.getCourseId());
            String courseTime = course != null ? course.getTime() : null;

            boolean matchesTeacher = TextUtils.isEmpty(currentQuery) || yogaClass.getTeacher().toLowerCase().contains(currentQuery.toLowerCase());
            boolean matchesDayOfWeek = selectedDayOfWeek.equals("All") || (courseDAO.getCourseById(yogaClass.getCourseId()) != null && courseDAO.getCourseById(yogaClass.getCourseId()).getDayOfWeek().equalsIgnoreCase(selectedDayOfWeek));
            boolean matchesYogaType = selectedYogaType.equals("All") || (courseDAO.getCourseById(yogaClass.getCourseId()) != null && classTypeDAO.getClassTypeNameById(courseDAO.getCourseById(yogaClass.getCourseId()).getClassTypeId()).equals(selectedYogaType));
            boolean matchesDateRange = isWithinDateRange(yogaClass.getDate());
            boolean matchesTimeRange = courseTime != null && isWithinTimeRange(courseTime);

            if (matchesTeacher && matchesDayOfWeek && matchesYogaType && matchesDateRange && matchesTimeRange) {
                filteredYogaClasses.add(yogaClass);
            }
        }
        Log.e("Classes", filteredYogaClasses.toString());
        adapter.notifyDataSetChanged();
    }

    private boolean isWithinDateRange(String date) {
        if (startDate == null || endDate == null) return true;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            long dateMillis = sdf.parse(date).getTime();
            return dateMillis >= startDate && dateMillis <= endDate;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isWithinTimeRange(String time) {
        if (startTime == null || endTime == null){
            return true;
        }
        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date parsedCourseTime = timeFormat.parse(time);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsedCourseTime);
            int courseTimeInMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

            return startTime <= courseTimeInMinutes && courseTimeInMinutes <= endTime;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
