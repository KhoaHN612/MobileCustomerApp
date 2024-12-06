package com.example.coursework.Adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coursework.DAO.DatabaseHelper;
import com.example.coursework.DAO.UserDAO;
import com.example.coursework.Model.YogaClass;
import com.example.coursework.R;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ClassViewHolder> {

    private List<YogaClass> classList;
    private FragmentManager fragmentManager;
    private String courseDayOfWeek;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private DatabaseHelper dbHelper;

    public ClassAdapter(List<YogaClass> classList, FragmentManager fragmentManager, String courseDayOfWeek, DatabaseHelper dbHelper) {
        this.classList = classList;
        this.fragmentManager = fragmentManager;
        this.courseDayOfWeek = courseDayOfWeek;
        this.dbHelper = dbHelper;
    }

    @NonNull
    @Override
    public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.class_of_course_item, parent, false);
        return new ClassViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
        YogaClass yogaClass = classList.get(position);

        UserDAO userDAO = new UserDAO(dbHelper.getWritableDatabase(), holder.itemView.getContext());
        List<String> userNames = userDAO.getAllAdminUserNames();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(holder.itemView.getContext(), android.R.layout.simple_dropdown_item_1line, userNames);
        holder.inputClassTeacher.setAdapter(adapter);
        holder.inputClassTeacher.setThreshold(1);
        holder.inputClassTeacher.setText(yogaClass.getTeacher());

        holder.inputClassTeacher.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                holder.inputClassTeacher.showDropDown();
            }
        });

        Log.e("AllUserName", userNames.toString());

        holder.inputClassDate.setText(yogaClass.getDate());
        holder.inputClassComments.setText(yogaClass.getComments());

        holder.inputClassDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            String currentInputDate = holder.inputClassDate.getText().toString();

            if (!currentInputDate.isEmpty()) {
                try {
                    Date selectedDate = dateFormat.parse(currentInputDate);
                    calendar.setTime(selectedDate);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            long utcTimeInMillis = calendar.getTimeInMillis() + TimeZone.getDefault().getOffset(calendar.getTimeInMillis());

            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Date for Class")
                    .setSelection(utcTimeInMillis)
                    .build();

            datePicker.show(fragmentManager, "DATE_PICKER");

            datePicker.addOnPositiveButtonClickListener(selection -> {
                Calendar selectedCalendar = Calendar.getInstance(TimeZone.getDefault());
                selectedCalendar.setTimeInMillis(selection);

                selectedCalendar.set(Calendar.HOUR_OF_DAY, 0);
                selectedCalendar.set(Calendar.MINUTE, 0);
                selectedCalendar.set(Calendar.SECOND, 0);
                selectedCalendar.set(Calendar.MILLISECOND, 0);

                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);

                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
                String selectedDate = dateFormat.format(selectedCalendar.getTime());
                String selectedDayOfWeek = dayFormat.format(selectedCalendar.getTime());

                if (selectedCalendar.before(today)) {
                    Toast.makeText(holder.itemView.getContext(), "Selected date cannot be before today.", Toast.LENGTH_SHORT).show();
                } else if (selectedDayOfWeek.equalsIgnoreCase(courseDayOfWeek)) {
                    holder.inputClassDate.setText(selectedDate);
                    yogaClass.setDate(selectedDate);
                } else {
                    Toast.makeText(holder.itemView.getContext(), "Selected date does not match the course day: " + courseDayOfWeek, Toast.LENGTH_SHORT).show();
                }
            });
        });




        holder.inputClassTeacher.addTextChangedListener(new CustomTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                yogaClass.setTeacher(s.toString());
            }
        });

        holder.inputClassComments.addTextChangedListener(new CustomTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                yogaClass.setComments(s.toString());
            }
        });
    }

    @Override
    public int getItemCount() {
        return classList.size();
    }

    static class ClassViewHolder extends RecyclerView.ViewHolder {
        TextInputEditText inputClassDate, inputClassComments;
        AutoCompleteTextView inputClassTeacher;

        public ClassViewHolder(@NonNull View itemView) {
            super(itemView);
            inputClassDate = itemView.findViewById(R.id.inputClassDate);
            inputClassTeacher = itemView.findViewById(R.id.inputClassTeacher);
            inputClassComments = itemView.findViewById(R.id.inputClassComments);
        }
    }

    private abstract class CustomTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void afterTextChanged(Editable s) {}
    }
}
