package com.example.coursework;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coursework.Adapter.OrderAdapter;
import com.example.coursework.DAO.ClassTypeDAO;
import com.example.coursework.DAO.CourseDAO;
import com.example.coursework.DAO.DatabaseHelper;
import com.example.coursework.DAO.OrderDAO;
import com.example.coursework.Model.Order;
import com.example.coursework.Model.YogaClass;
import com.example.coursework.Model.YogaCourse;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class YogaClassDetailFragment extends Fragment {

    private YogaClass yogaClass;
    private CourseDAO courseDAO;
    private ClassTypeDAO classTypeDAO;
    private OrderAdapter orderAdapter;
    private List<Order> orders;
    DatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_yoga_class_detail, container, false);

        MaterialToolbar toolbar = view.findViewById(R.id.topAppBarClassDetail);
        toolbar.setNavigationOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });
        TextView tvClassTeacher = view.findViewById(R.id.tv_class_teacher);
        TextView tvClassDate = view.findViewById(R.id.tv_class_date);
        TextView tvClassComments = view.findViewById(R.id.tv_class_comments);
        TextView tvCourseType = view.findViewById(R.id.tv_course_type);
        TextView tvCourseDayOfWeek = view.findViewById(R.id.tv_course_day_of_week);
        TextView tvCourseCapacity = view.findViewById(R.id.tv_course_capacity);
        TextView tvCourseTime = view.findViewById(R.id.tv_course_time);
        TextView tvCoursePrice = view.findViewById(R.id.tv_course_price);
        TextView tvCourseDescription = view.findViewById(R.id.tv_course_description);
        RecyclerView rvOrders = view.findViewById(R.id.recycler_view_orders);
        rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));


        if (getArguments() != null) {
            yogaClass = (YogaClass) getArguments().getSerializable("yoga_class");
        }

        dbHelper = new DatabaseHelper(requireContext());
        courseDAO = new CourseDAO(dbHelper.getWritableDatabase(), requireContext());
        classTypeDAO = new ClassTypeDAO(dbHelper.getWritableDatabase(), requireContext());;

        if (yogaClass != null) {
            YogaCourse course = courseDAO.getCourseById(yogaClass.getCourseId());
            tvClassTeacher.setText("Teacher: " + yogaClass.getTeacher());
            tvClassDate.setText("Date: " + yogaClass.getDate());
            tvClassComments.setText("Comments: " + yogaClass.getComments());

            if (course != null) {
                tvCourseType.setText("Course Type: " + classTypeDAO.getClassTypeNameById(course.getClassTypeId()));
                tvCourseDayOfWeek.setText("Day of Week: " + course.getDayOfWeek());
                tvCourseCapacity.setText("Capacity: " + course.getCapacity());
                tvCourseTime.setText("Time: " + course.getTime() + " -> " + CourseDAO.calculateTime(course.getTime(), course.getDuration()));
                tvCoursePrice.setText("Price: $" + course.getPrice());
                tvCourseDescription.setText("Description: " + course.getDescription());
            }

            orders = new ArrayList<>();
            orderAdapter = new OrderAdapter(dbHelper, requireContext(), orders);
            rvOrders.setAdapter(orderAdapter);

            loadOrders();
        }

        return view;
    }

    private void loadOrders() {
        if (yogaClass != null) {
            OrderDAO orderDAO = new OrderDAO(dbHelper.getWritableDatabase(),requireContext());
            orderDAO.syncOrdersFromFirestoreToSQLite();
            orders.clear();
            orders.addAll(orderDAO.getOrdersByClassId(yogaClass.getId()));

            Log.e("Order", orders.toString());
            orderAdapter.notifyDataSetChanged();
        }
    }
}
