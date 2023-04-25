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

    private String displayName;

    public boolean isHasRead() {
        return hasRead;
    }

    public void setHasRead(boolean hasRead) {
        this.hasRead = hasRead;
    }

    private boolean hasRead;

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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Chat(boolean isPrivate, List<String> userList){
        this.isPrivate = isPrivate;
        this.userList = userList;
        this.messageDeque = new ConcurrentLinkedDeque<>();
    }
}
