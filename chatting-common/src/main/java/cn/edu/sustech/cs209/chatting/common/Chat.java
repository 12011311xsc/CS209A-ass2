package cn.edu.sustech.cs209.chatting.common;

import java.io.Serializable;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Chat implements Serializable {
    private boolean isPrivate;

    private Deque<Message> messageDeque;

    private List<String> userList;

    private int id;

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public Deque<Message> getMessageDeque() {
        return messageDeque;
    }

    public void setMessageDeque(Deque<Message> messageDeque) {
        this.messageDeque = messageDeque;
    }

    public List<String> getUserList() {
        return userList;
    }

    public void setUserList(List<String> userList) {
        this.userList = userList;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Chat(boolean isPrivate, int id, List<String> userList){
        this.isPrivate = isPrivate;
        this.id = id;
        this.userList = userList;
        this.messageDeque = new ConcurrentLinkedDeque<>();
    }
}
