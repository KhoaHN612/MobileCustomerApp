package com.example.coursework.Model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class YogaCourse implements Serializable {
    private String id;
    private String dayOfWeek;
    private String startDay;
    private String time;
    private int capacity;
    private int duration;
    private double price;
    private String classTypeId;
    private String description;
    private String imageUrl;
    private String localImageUri;

    public YogaCourse(String id, String dayOfWeek, String startDay, String time, int capacity, int duration, double price, String classTypeId, String description, String imageUrl, String localImageUri) {
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.startDay = startDay;
        this.time = time;
        this.capacity = capacity;
        this.duration = duration;
        this.price = price;
        this.classTypeId = classTypeId;
        this.description = description;
        this.imageUrl = imageUrl;
        this.localImageUri = localImageUri;
    }

    public String getId() { return id; }
    public String getDayOfWeek() { return dayOfWeek; }
    public String getStartDay() { return startDay; }
    public String getTime() { return time; }
    public int getCapacity() { return capacity; }
    public int getDuration() { return duration; }
    public double getPrice() { return price; }
    public String getClassTypeId() { return classTypeId; }  // Sử dụng classTypeId thay vì classType
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getLocalImageUri() { return localImageUri; }

    public void setId(String firestoreId) { this.id = firestoreId; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public void setStartDay(String startDay) { this.startDay = startDay; }
    public void setTime(String time) { this.time = time; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public void setDuration(int duration) { this.duration = duration; }
    public void setPrice(double price) { this.price = price; }
    public void setClassTypeId(String classTypeId) { this.classTypeId = classTypeId; }
    public void setDescription(String description) { this.description = description; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setLocalImageUri(String localImageUri) { this.localImageUri = localImageUri; }

    public Map<String, Object> toMap() {
        Map<String, Object> courseData = new HashMap<>();
        courseData.put("id", id);
        courseData.put("day_of_week", dayOfWeek);
        courseData.put("start_day", startDay);
        courseData.put("time", time);
        courseData.put("capacity", capacity);
        courseData.put("duration", duration);
        courseData.put("price", price);
        courseData.put("class_type_id", classTypeId);
        courseData.put("description", description);
        courseData.put("image_url", imageUrl);
        courseData.put("local_image_uri", localImageUri);
        return courseData;
    }
}