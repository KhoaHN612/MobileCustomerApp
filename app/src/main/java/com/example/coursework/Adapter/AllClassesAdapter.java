package com.example.coursework.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coursework.DAO.ClassTypeDAO;
import com.example.coursework.Model.ClassType;
import com.example.coursework.Model.YogaClass;
import com.example.coursework.Model.YogaCourse;
import com.example.coursework.DAO.CourseDAO;
import com.example.coursework.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AllClassesAdapter extends RecyclerView.Adapter<AllClassesAdapter.YogaClassViewHolder>{

    private Context context;
    private List<YogaClass> yogaClasses;
    private CourseDAO courseDAO;
    private ClassTypeDAO classTypeDAO;
    private OnItemClickListener onItemClickListener;

    public AllClassesAdapter(Context context, List<YogaClass> yogaClasses, CourseDAO courseDAO, ClassTypeDAO classTypeDAO) {
        this.context = context;
        this.yogaClasses = yogaClasses;
        this.courseDAO = courseDAO;
        this.classTypeDAO = classTypeDAO;
    }

    public interface OnItemClickListener {
        void onItemClick(YogaClass yogaClass);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public YogaClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.class_item, parent, false);
        return new YogaClassViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull YogaClassViewHolder holder, int position) {
        YogaClass yogaClass = yogaClasses.get(position);
        YogaCourse course = courseDAO.getCourseById(yogaClass.getCourseId());

        holder.tvTeacher.setText(yogaClass.getTeacher());
        holder.tvDate.setText(yogaClass.getDate());
        holder.tvComment.setText(yogaClass.getComments());

        if (course != null) {
            holder.tvCapacity.setText("Capacity: " + course.getCapacity());
            holder.tvDayOfWeek.setText(course.getDayOfWeek());
            holder.tvTime.setText("Time: " + course.getTime() + " - " + CourseDAO.calculateTime(course.getTime(), course.getDuration()));
            holder.tvPrice.setText("Price: $" + course.getPrice());
            String classTypeName = classTypeDAO.getClassTypeNameById(course.getClassTypeId());
            holder.tvClassType.setText(classTypeName);
        }

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(yogaClass);
            }
        });
    }

    @Override
    public int getItemCount() {
        return yogaClasses.size();
    }

    public static class YogaClassViewHolder extends RecyclerView.ViewHolder {
        TextView tvTeacher, tvDate, tvComment, tvCapacity, tvDayOfWeek, tvTime, tvPrice, tvClassType;

        public YogaClassViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTeacher = itemView.findViewById(R.id.tv_teacher);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvComment = itemView.findViewById(R.id.tv_comment);
            tvCapacity = itemView.findViewById(R.id.tv_capacity);
            tvDayOfWeek = itemView.findViewById(R.id.tv_day_of_week);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvClassType = itemView.findViewById(R.id.tv_class_type);
        }
    }
}
