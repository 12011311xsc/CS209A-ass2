package cn.edu.sustech.cs209.chatting.common;

import java.io.Serializable;

public class User implements Serializable {


    private String username;

    private int id;

    private boolean isOnline = false;

    public User(String username){
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

}
