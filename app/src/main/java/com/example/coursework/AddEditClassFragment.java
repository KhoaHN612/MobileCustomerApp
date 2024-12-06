package com.example.coursework;

import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.animation.ObjectAnimator;
import android.transition.TransitionManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.coursework.Adapter.ClassAdapter;
import com.example.coursework.DAO.ClassDAO;
import com.example.coursework.DAO.DatabaseHelper;
import com.example.coursework.Model.YogaClass;
import com.example.coursework.Model.YogaCourse;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddEditClassFragment extends Fragment {

    private TextInputEditText inputDate, inputNumberClasses;
    private MaterialButton btnAddClass, btnSubmitClasses;
    private RecyclerView recyclerViewClasses;
    private LinearLayout collapsibleSection;
    private LinearLayout toggleInputContainer;
    private ClassAdapter classAdapter;
    private List<YogaClass> classList;
    private List<YogaClass> originalClassList;
    private YogaCourse course;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private ImageView expandIcon;
    private boolean isSectionExpanded = false;
    private DatabaseHelper dbHelper;
    private ClassDAO classDAO;
    private boolean isEditMode = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_edit_class, container, false);

        course = (YogaCourse) getArguments().getSerializable("course");
        Log.e("Mode", course.toMap().toString());

        dbHelper = new DatabaseHelper(requireContext());
        classDAO = new ClassDAO(dbHelper.getWritableDatabase(), requireContext());
        classDAO.syncClassesFromFirestoreToSQLite();

        classList = new ArrayList<>();
        originalClassList = new ArrayList<>();
        initializeViews(view);


        loadExistingClasses();

        setupSwipeToDelete();

        return view;
    }

    private void addDefaultClass() {
        if (course != null && course.getId() != null) {
            classList.add(new YogaClass(null, course.getId(), course.getStartDay(), "", ""));
            classAdapter.notifyDataSetChanged();
            Log.e("AddDefaultClass", "Default class added for course ID: " + course.getId());
        }
    }
    private void loadExistingClasses() {
        if (course != null && course.getId() != null) {
            originalClassList = classDAO.getClassesByCourseId(course.getId());
            classList.addAll(originalClassList);
            if (classList.isEmpty()) {
                addDefaultClass();
            }
            classAdapter = new ClassAdapter(classList, getParentFragmentManager(), course.getDayOfWeek(), dbHelper);
            recyclerViewClasses.setAdapter(classAdapter);
        }
    }


    private void initializeViews(View view) {

        MaterialToolbar toolbar = view.findViewById(R.id.topAppBarClass);
        toolbar.setNavigationOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });

        inputDate = view.findViewById(R.id.inputDate);
        inputNumberClasses = view.findViewById(R.id.inputNumberClasses);
        btnAddClass = view.findViewById(R.id.btnAddClass);
        btnSubmitClasses = view.findViewById(R.id.btnSubmitClasses);
        recyclerViewClasses = view.findViewById(R.id.recyclerViewClasses);
        collapsibleSection = view.findViewById(R.id.collapsibleSection);
        toggleInputContainer = view.findViewById(R.id.toggleInputContainer);

        expandIcon = view.findViewById(R.id.expandIcon);
        toggleInputContainer.setOnClickListener(v -> toggleSectionVisibility());

        classAdapter = new ClassAdapter(classList, getParentFragmentManager(), course.getDayOfWeek(), dbHelper);
        recyclerViewClasses.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewClasses.setAdapter(classAdapter);

        inputDate.setOnClickListener(v -> showDatePicker());
        btnAddClass.setOnClickListener(v -> addClasses());
        btnSubmitClasses.setOnClickListener(v -> submitClasses());
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            private Drawable icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_delete);
            private final ColorDrawable background = new ColorDrawable(ContextCompat.getColor(getContext(), R.color.md_theme_error));

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                YogaClass deletedClass = classList.get(position);

                classList.remove(position);
                classAdapter.notifyItemRemoved(position);

                Snackbar.make(recyclerViewClasses, "Class deleted", Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.btnSubmitClasses)
                        .setAction("Undo", v -> {
                            classList.add(position, deletedClass);
                            classAdapter.notifyItemInserted(position);
                        }).show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                View itemView = viewHolder.itemView;
                int backgroundCornerOffset = 20;

                if (dX > 0) {
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
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerViewClasses);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date for Class")
                .setSelection(calendar.getTimeInMillis())
                .build();

        datePicker.show(getParentFragmentManager(), "MATERIAL_DATE_PICKER");

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.setTimeInMillis(selection);
            String selectedDate = dateFormat.format(selectedCalendar.getTime());

            String selectedDayOfWeek = new SimpleDateFormat("EEEE", Locale.getDefault()).format(selectedCalendar.getTime());
            String courseDayOfWeek = course.getDayOfWeek();

            if (!selectedDayOfWeek.equalsIgnoreCase(courseDayOfWeek)) {
                Toast.makeText(getContext(), "Selected date does not match the course day: " + courseDayOfWeek, Toast.LENGTH_SHORT).show();
            } else {
                inputDate.setText(selectedDate);
            }
        });
    }

    private void addClasses() {
        String numberOfClassesStr = inputNumberClasses.getText().toString();
        String date = inputDate.getText().toString();

        if (numberOfClassesStr.isEmpty() || date.isEmpty()) {
            Toast.makeText(getContext(), "Date and Number of Classes are required", Toast.LENGTH_SHORT).show();
            return;
        }

        int numberOfClasses = Integer.parseInt(numberOfClassesStr);

        try {
            Date startDate = dateFormat.parse(date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);

            int addedClasses = 0;
            for (int i = 0; i < numberOfClasses; i++) {
                String newDate = dateFormat.format(calendar.getTime());

                if (!isDateAlreadyInClassList(newDate)) {
                    classList.add(new YogaClass(null, course.getId(), newDate, "", ""));
                    addedClasses++;
                }

                calendar.add(Calendar.WEEK_OF_YEAR, 1);
            }

            if (addedClasses == 0) {
                Toast.makeText(getContext(), "All selected dates are already added.", Toast.LENGTH_SHORT).show();
            } else {
                Collections.sort(classList, (o1, o2) -> {
                    try {
                        Date date1 = dateFormat.parse(o1.getDate());
                        Date date2 = dateFormat.parse(o2.getDate());
                        return date1.compareTo(date2);
                    } catch (Exception e) {
                        return 0;
                    }
                });

                classAdapter.notifyDataSetChanged();
                inputDate.setText("");
                inputNumberClasses.setText("");
                Toast.makeText(getContext(), "Classes added successfully", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Invalid date format.", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitClasses() {
        boolean allFieldsValid = true;

        for (YogaClass yogaClass : classList) {
            if (yogaClass.getTeacher() == null || yogaClass.getTeacher().trim().isEmpty()) {
                Toast.makeText(getContext(), "Each class must have a teacher assigned", Toast.LENGTH_SHORT).show();
                allFieldsValid = false;
                break;
            }
        }

        if (allFieldsValid == false){
            return;
        }

            List<YogaClass> classesToDelete = new ArrayList<>(originalClassList);
            classesToDelete.removeAll(classList);

            for (YogaClass yogaClass : classesToDelete) {
                classDAO.deleteClassById(yogaClass.getId());
            }

        for (YogaClass yogaClass : classList) {
            classDAO.insertOrUpdateClass(yogaClass);
        }

        Toast.makeText(getContext(), "Classes submitted successfully!", Toast.LENGTH_SHORT).show();

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).receiveClasses(classList);
        }

        getParentFragmentManager().popBackStack();

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openCourseListFragment();
        }

    }

    private boolean isDateAlreadyInClassList(String date) {
        for (YogaClass yogaClass : classList) {
            if (yogaClass.getDate().equals(date)) {
                return true;
            }
        }
        return false;
    }

    private void toggleSectionVisibility() {
        TransitionManager.beginDelayedTransition((ViewGroup) collapsibleSection.getParent());

        if (isSectionExpanded) {
            collapsibleSection.setVisibility(View.GONE);
            rotateExpandIcon(false);
        } else {
            collapsibleSection.setVisibility(View.VISIBLE);
            rotateExpandIcon(true);
        }
        isSectionExpanded = !isSectionExpanded;
    }

    private void rotateExpandIcon(boolean isExpanding) {
        float fromRotation = isExpanding ? 0f : 180f;
        float toRotation = isExpanding ? 180f : 0f;

        ObjectAnimator rotateAnimator = ObjectAnimator.ofFloat(expandIcon, "rotation", fromRotation, toRotation);
        rotateAnimator.setDuration(300);
        rotateAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        rotateAnimator.start();
    }
}
