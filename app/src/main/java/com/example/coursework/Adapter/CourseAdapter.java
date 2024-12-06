package com.example.coursework.Adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coursework.DAO.ClassTypeDAO;
import com.example.coursework.Model.YogaCourse;
import com.example.coursework.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {
    private final ArrayList<YogaCourse> courseList;
    private final Context context;
    private final ClassTypeDAO classTypeDAO;
    private OnCourseClickListener listener;

    public CourseAdapter(Context context, ArrayList<YogaCourse> courseList, ClassTypeDAO classTypeDAO) {
        this.context = context;
        this.courseList = courseList;
        this.classTypeDAO = classTypeDAO;
    }

    public void setOnCourseClickListener(OnCourseClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.course_item, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        YogaCourse course = courseList.get(position);

        String classTypeName = classTypeDAO.getClassTypeNameById(course.getClassTypeId());
        holder.textViewTitle.setText(classTypeName);
        holder.textViewDescription.setText(course.getDescription());
        holder.textViewStartDay.setText("Start Day: " + course.getStartDay());
        holder.textViewDayOfWeek.setText("Day of Week: " + course.getDayOfWeek());
        holder.textViewTime.setText("Time: " + course.getTime());
        holder.textViewDuration.setText("Duration: " + course.getDuration() + " mins");
        holder.textViewCapacity.setText("Capacity: " + course.getCapacity() + " people");
        holder.textViewPrice.setText("Price: £" + course.getPrice());

        String imageUri = "file://" + course.getLocalImageUri();

        Picasso.get()
                .load(imageUri)
                .placeholder(R.drawable.ic_aerial_yoga)
                .error(R.drawable.ic_aerial_yoga)
                .into(holder.imageViewCourse);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCourseClick(course);
            }
        });
    }

    @Override
    public int getItemCount() {
        return courseList.size();
    }

    public interface OnCourseClickListener {
        void onCourseClick(YogaCourse course);
    }

    static class CourseViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewCourse;
        TextView textViewTitle;
        TextView textViewDescription;
        TextView textViewStartDay;
        TextView textViewDayOfWeek;
        TextView textViewTime;
        TextView textViewDuration;
        TextView textViewCapacity;
        TextView textViewPrice;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewCourse = itemView.findViewById(R.id.imageViewCourse);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewDescription = itemView.findViewById(R.id.textViewDescription);
            textViewStartDay = itemView.findViewById(R.id.textViewStartDay);
            textViewDayOfWeek = itemView.findViewById(R.id.textViewDayOfWeek);
            textViewTime = itemView.findViewById(R.id.textViewTime);
            textViewDuration = itemView.findViewById(R.id.textViewDuration);
            textViewCapacity = itemView.findViewById(R.id.textViewCapacity);
            textViewPrice = itemView.findViewById(R.id.textViewPrice);
        }
    }
}
