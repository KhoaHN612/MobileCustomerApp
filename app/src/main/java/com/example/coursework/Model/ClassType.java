package com.example.coursework.Model;

public class ClassType {
    private String id;
    private String name;

    public ClassType(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(String id) { this.id = id; }

    public void setName(String name) {
        this.name = name;
    }
}
