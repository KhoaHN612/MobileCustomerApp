package com.example.coursework.Model;

import java.io.Serializable;

public class Order implements Serializable {
    private String id;
    private String classId;
    private String orderDate;
    private String userId;

    public Order(String id, String classId, String orderDate, String userId) {
        this.id = id;
        this.classId = classId;
        this.orderDate = orderDate;
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
