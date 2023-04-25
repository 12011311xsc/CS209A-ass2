package cn.edu.sustech.cs209.chatting.common;

import java.io.Serializable;
import java.util.ArrayList;

public class Communication implements Serializable {

    private int Rid;

    private String sendFrom;


    private ArrayList<User> relatedUsers;

    private ArrayList<Chat> relatedChats;

    public int getRid() {
        return Rid;
    }

    public void setRid(int rid) {
        Rid = rid;
    }

    public String getSendFrom() {
        return sendFrom;
    }

    public void setSendFrom(String sendFrom) {
        this.sendFrom = sendFrom;
    }

    public ArrayList<User> getRelatedUsers() {
        return relatedUsers;
    }

    public ArrayList<Chat> getRelatedChats() {
        return relatedChats;
    }


    public Communication(int Rid, String sendFrom){
        this.Rid = Rid;
        this.sendFrom = sendFrom;
        this.relatedUsers = new ArrayList<>();
        this.relatedChats = new ArrayList<>();
    }
}
