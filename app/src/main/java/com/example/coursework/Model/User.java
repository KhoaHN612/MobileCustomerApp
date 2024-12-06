package com.example.coursework.Model;

public class User {
    private String id;
    private String email;
    private String password;
    private String role;
    private String userName;

    public User(String id, String email, String password, String role, String userName) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.userName = userName;
    }

    public String getId() { return id; }
    public String getUserName() { return userName; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getRole() { return role; }

    public void setId(String id) { this.id = id; }
    public void setRole(String role) {this.role = role; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setEmail(String email) {this.email = email;}
    public void setPassword(String password) {this.password = password;}
}
