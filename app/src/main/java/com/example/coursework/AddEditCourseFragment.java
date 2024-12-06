package com.example.coursework;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import com.example.coursework.DAO.ClassDAO;
import com.example.coursework.DAO.ClassTypeDAO;
import com.example.coursework.DAO.CourseDAO;
import com.example.coursework.DAO.DatabaseHelper;
import com.example.coursework.Model.ClassType;
import com.example.coursework.Model.YogaCourse;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddEditCourseFragment extends Fragment {
    MaterialToolbar toolbar;

    private TextInputEditText startDayInput, timeOfCourseInput, capacityInput, durationInput, priceInput, typeOfClassInput, descriptionInput;
    private MaterialButton submitButton, skipButton;
    private TextInputLayout startDayInputLayout;

    private Spinner dayOfWeekSpinner;
    private final String[] daysOfWeek = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    private RadioGroup classTypeRadioGroup;
    private String selectedClassTypeId = null;
    private String selectedClassTypeName = "";
    private TextView classTypeErrorText;

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private ImageView selectedImageView;
    private MaterialButton selectImageButton;

    private static final int REQUEST_CODE_STORAGE_PERMISSION = 100;

    private static final String READ_EXTERNAL_STORAGE_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE;
    private static final String WRITE_EXTERNAL_STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    private DatabaseHelper dbHelper;
    private ClassTypeDAO classTypeDAO;
    private CourseDAO courseDAO;
    private ClassDAO classDAO;

    private YogaCourse courseToEdit;
    private boolean isEditMode = false;
    private String previousDayOfWeek = null;
    private boolean isImageUpdated = false;

    private final AdapterView.OnItemSelectedListener dayOfWeekListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String selectedDay = dayOfWeekSpinner.getSelectedItem().toString();

            if (!selectedDay.equals(previousDayOfWeek)) {
                startDayInput.setText(null);
                startDayInputLayout.setError(null);

                previousDayOfWeek = selectedDay;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {}
    };


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_edit_course, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        classTypeDAO = new ClassTypeDAO(db, requireContext());
        classTypeDAO.initializeDefaultClassTypes();
        courseDAO = new CourseDAO(db, requireContext());
        classDAO = new ClassDAO(db, requireContext());

        initializeViews(view);

        checkAndRequestPermissions();

        Bundle args = getArguments();
        if (args != null) {
            courseToEdit = (YogaCourse) args.getSerializable("course");
        } else {
            courseToEdit = null;
        }
        if (courseToEdit != null && courseToEdit.getId() != null) {
            isEditMode = true;
            toolbar.setTitle("Edit Course");
            populateFieldsWithCourseData(courseToEdit);
        }

        return view;
    }


    private void initializeViews(View view) {
        toolbar = view.findViewById(R.id.topAppBarCourse);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        selectImageButton = view.findViewById(R.id.selectImageButton);
        selectedImageView = view.findViewById(R.id.selectedImageView);
        selectImageButton.setOnClickListener(v -> openImagePicker());
        if (!isNetworkAvailable()){
            selectImageButton.setEnabled(false);
        }

        dayOfWeekSpinner = view.findViewById(R.id.dayOfWeekSpinner);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, daysOfWeek);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dayOfWeekSpinner.setAdapter(adapter);

        dayOfWeekSpinner.setOnItemSelectedListener(dayOfWeekListener);

        startDayInput = view.findViewById(R.id.startDayInput);
        startDayInputLayout = view.findViewById(R.id.startDayInputLayout);

        timeOfCourseInput = view.findViewById(R.id.timeOfCourseInput);
        capacityInput = view.findViewById(R.id.capacityInput);
        durationInput = view.findViewById(R.id.durationInput);
        priceInput = view.findViewById(R.id.priceInput);

        classTypeRadioGroup = view.findViewById(R.id.classTypeRadioGroup);
        classTypeErrorText = view.findViewById(R.id.classTypeErrorText);
        populateClassTypeRadioGroup();

        descriptionInput = view.findViewById(R.id.descriptionInput);
        submitButton = view.findViewById(R.id.submitButton);
        skipButton = view.findViewById(R.id.skipButton);

        startDayInput.setOnClickListener(v -> showDatePicker());

        timeOfCourseInput.setOnClickListener(v -> showTimePicker());

        submitButton.setOnClickListener(v -> validateAndSubmit());
    }

    private void populateClassTypeRadioGroup() {
        List<ClassType> classTypes = classTypeDAO.getAllClassTypes();
        for (ClassType classType : classTypes) {
            RadioButton radioButton = new RadioButton(requireContext());
            radioButton.setText(classType.getName());
            radioButton.setTag(classType.getId());

            classTypeRadioGroup.addView(radioButton);

            radioButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedClassTypeId = buttonView.getTag().toString(); // Lấy id từ tag
                    selectedClassTypeName = classType.getName();
                    classTypeErrorText.setVisibility(View.GONE);
                }
            });
        }
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), READ_EXTERNAL_STORAGE_PERMISSION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), WRITE_EXTERNAL_STORAGE_PERMISSION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(), new String[]{
                    READ_EXTERNAL_STORAGE_PERMISSION,
                    WRITE_EXTERNAL_STORAGE_PERMISSION
            }, REQUEST_CODE_STORAGE_PERMISSION);
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_PICK);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            selectedImageView.setImageURI(imageUri);
            selectedImageView.setVisibility(View.VISIBLE);
            isImageUpdated = true;
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                    String selectedDayOfWeek = getDayOfWeek(selectedDate);

                    Calendar today = Calendar.getInstance();
                    today.set(Calendar.HOUR_OF_DAY, 0);
                    today.set(Calendar.MINUTE, 0);
                    today.set(Calendar.SECOND, 0);
                    today.set(Calendar.MILLISECOND, 0);

                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth, 0, 0, 0);

                    String dayOfWeekFromSpinner = dayOfWeekSpinner.getSelectedItem().toString();

                    if (dayOfWeekFromSpinner.isEmpty()) {
                        Toast.makeText(getContext(), "Please select a day of the week first", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (selectedCalendar.before(today)) {
                        startDayInputLayout.setError("Selected date cannot be before today.");
                        startDayInput.setText(null);
                        return;
                    }

                    if (selectedDayOfWeek.equalsIgnoreCase(dayOfWeekFromSpinner)) {
                        startDayInput.setText(selectedDate);
                        startDayInputLayout.setError(null);
                    } else {
                        startDayInputLayout.setError("Selected date does not match. Need " + dayOfWeekFromSpinner);
                        startDayInput.setText(null);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                getContext(),
                (view, hourOfDay, minute) -> {
                    String time = String.format("%02d:%02d", hourOfDay, minute);
                    timeOfCourseInput.setText(time);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.show();
    }

    private String getDayOfWeek(String date) {
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Date parsedDate = format.parse(date);
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
            return dayFormat.format(parsedDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }

    private void validateAndSubmit() {
        if (dayOfWeekSpinner.getSelectedItem() == null || dayOfWeekSpinner.getSelectedItem().toString().isEmpty()) {
            Toast.makeText(getContext(), "Please select a day of the week", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isEmpty(startDayInput)) {
            startDayInput.setError("Required field");
            return;
        } else {
            startDayInput.setError(null);
        }

        if (isEmpty(timeOfCourseInput)) {
            timeOfCourseInput.setError("Required field");
            return;
        } else {
            timeOfCourseInput.setError(null);
        }

        if (isEmpty(capacityInput)) {
            capacityInput.setError("Required field");
            return;
        } else {
            capacityInput.setError(null);
        }

        if (isEmpty(durationInput)) {
            durationInput.setError("Required field");
            return;
        } else {
            durationInput.setError(null);
        }

        if (isEmpty(priceInput)) {
            priceInput.setError("Required field");
            return;
        } else {
            priceInput.setError(null);
        }

        if (selectedClassTypeId == null) {
            classTypeErrorText.setVisibility(View.VISIBLE);
            return;
        } else {
            classTypeErrorText.setVisibility(View.INVISIBLE);
        }

        String courseId = isEditMode ? courseToEdit.getId() : null;
        String dayOfWeek = dayOfWeekSpinner.getSelectedItem().toString();
        String startDay = startDayInput.getText().toString();
        String timeOfCourse = timeOfCourseInput.getText().toString();
        int capacity = Integer.parseInt(capacityInput.getText().toString());
        int duration = Integer.parseInt(durationInput.getText().toString());
        double price = Double.parseDouble(priceInput.getText().toString());
        String localImagePath = isImageUpdated
                ? courseDAO.saveImageToAppStorage(imageUri)
                : (courseToEdit != null && courseToEdit.getLocalImageUri() != null ? courseToEdit.getLocalImageUri() : null);
        String description = descriptionInput.getText().toString();

        String confirmationMessage = "Day of Week: " + dayOfWeek + "\n" +
                "Start Day: " + startDay + "\n" +
                "Time: " + timeOfCourse + "\n" +
                "Capacity: " + capacity + " people\n" +
                "Duration: " + duration + " minutes\n" +
                "Price: £" + price + "\n" +
                "Class Type: " + selectedClassTypeName + "\n" +
                "Description: " + description;

        if (isEditMode && !dayOfWeek.equals(courseToEdit.getDayOfWeek())) {
            confirmationMessage += "\n\nNote: You have changed the Day of Week. All existing classes for this course will be deleted.";
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Yoga Course")
                .setMessage(confirmationMessage)
                .setPositiveButton("Confirm", (dialog, which) -> {

                    YogaCourse course = new YogaCourse(
                            courseId, dayOfWeek, startDay, timeOfCourse, capacity, duration,
                            price, selectedClassTypeId, description,
                            null,
                            localImagePath
                    );

                    if (isEditMode && !dayOfWeek.equals(courseToEdit.getDayOfWeek())) {
                        Log.e("Delete", "Deleted");
                        classDAO.deleteClassesByCourseId(courseToEdit.getId());
                    }

                    courseDAO.insertOrUpdateCourse(course);

                    Toast.makeText(getContext(), isEditMode ? "Course updated successfully!" : "Course added successfully!", Toast.LENGTH_SHORT).show();

                    ((MainActivity) requireActivity()).receiveCourse(course);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private boolean isEmpty(TextInputEditText input) {
        return input.getText() == null || input.getText().toString().trim().isEmpty();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void populateFieldsWithCourseData(YogaCourse course) {
        dayOfWeekSpinner.setSelection(getIndexForDayOfWeek(course.getDayOfWeek()));
        previousDayOfWeek = course.getDayOfWeek();
        startDayInput.setText(course.getStartDay());
        timeOfCourseInput.setText(course.getTime());
        capacityInput.setText(String.valueOf(course.getCapacity()));
        durationInput.setText(String.valueOf(course.getDuration()));
        priceInput.setText(String.valueOf(course.getPrice()));
        descriptionInput.setText(course.getDescription());

        for (int i = 0; i < classTypeRadioGroup.getChildCount(); i++) {
            RadioButton radioButton = (RadioButton) classTypeRadioGroup.getChildAt(i);
            if (radioButton.getTag().toString().equals(course.getClassTypeId())) {
                radioButton.setChecked(true);
                break;
            }
        }

        if (course.getLocalImageUri() != null) {
            imageUri = Uri.parse(course.getLocalImageUri());
            selectedImageView.setImageURI(imageUri);
            selectedImageView.setVisibility(View.VISIBLE);
        }

        skipButton.setVisibility(View.VISIBLE);

        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String courseId = isEditMode ? courseToEdit.getId() : null;
                String dayOfWeek = dayOfWeekSpinner.getSelectedItem().toString();
                String startDay = startDayInput.getText().toString();
                String timeOfCourse = timeOfCourseInput.getText().toString();
                int capacity = Integer.parseInt(capacityInput.getText().toString());
                int duration = Integer.parseInt(durationInput.getText().toString());
                double price = Double.parseDouble(priceInput.getText().toString());
                String localImagePath = isImageUpdated
                        ? courseDAO.saveImageToAppStorage(imageUri)
                        : (isEditMode ? courseToEdit.getLocalImageUri() : null);
                String description = descriptionInput.getText().toString();

                YogaCourse course = new YogaCourse(
                        courseId, dayOfWeek, startDay, timeOfCourse, capacity, duration,
                        price, selectedClassTypeId, description,
                        null,
                        localImagePath
                );

                Toast.makeText(getContext(), "Skip update course", Toast.LENGTH_SHORT).show();


                ((MainActivity) requireActivity()).receiveCourse(course);
            }
        });
        submitButton.setText("Update Yoga Course");
    }

    private int getIndexForDayOfWeek(String dayOfWeek) {
        for (int i = 0; i < daysOfWeek.length; i++) {
            if (daysOfWeek[i].equalsIgnoreCase(dayOfWeek)) {
                return i;
            }
        }
        return 0;
    }
}