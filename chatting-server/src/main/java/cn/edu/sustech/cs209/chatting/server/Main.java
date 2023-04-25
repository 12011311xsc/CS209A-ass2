package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Chat;
import cn.edu.sustech.cs209.chatting.common.Communication;
import cn.edu.sustech.cs209.chatting.common.User;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    public static class Server{

        private Map<String, User> userList;

        private Map<Integer, Chat> chatList;

        private int port;

        private ServerSocket serverSocket;

        private Map<String, ObjectOutputStream> clientOutputs;

        public Map<String, User> getUserList() {
            return userList;
        }

        public Map<Integer, Chat> getChatList() {
            return chatList;
        }

        public ServerSocket getServerSocket() {
            return serverSocket;
        }

        private Communication handleCommunication(Communication communication){
            Communication response = new Communication(0,"");
            String sendFrom = communication.getSendFrom();
            switch (communication.getRid()){
                case 1:
                    response.setRid(1);
                    if(userList.containsKey(sendFrom)){
                        if(!userList.get(sendFrom).isOnline()){
                            response.getRelatedUsers().add(userList.get(sendFrom));
                            System.out.println(sendFrom+" login success");
                        }
                        else {
                            break;
                        }
                    }
                    else {
                        User user = new User(sendFrom);
                        user.setOnline(true);
                        userList.put(sendFrom,user);
                        response.getRelatedUsers().add(userList.get(sendFrom));
                        System.out.println(sendFrom+" register and login success");
                    }
                    clientOutputs.forEach((key, value) -> {
                        if(!Objects.equals(key, sendFrom)){
                            try {
                                value.writeObject(response);
                                System.out.println("send login to "+key);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    break;
                case 2:
                    //TODO:log out
                    response.setRid(2);
                    userList.get(sendFrom).setOnline(false);
                    response.getRelatedUsers().add(userList.get(sendFrom));

                    clientOutputs.forEach((key, value) -> {
                        if(!Objects.equals(key, sendFrom)){
                            try {
                                value.writeObject(response);
                                System.out.println("send log out to "+key);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    clientOutputs.remove(sendFrom);
                    break;
                case 3:
                    response.setRid(3);
                    userList.forEach((key, value) -> {
                        if(value.isOnline() && !Objects.equals(key, sendFrom)){
                            response.getRelatedUsers().add(value);
                        }
                    });
                    // TODO:send chat information
                    chatList.forEach((key,value) -> {
                        if(value.getUserList().contains(sendFrom)){
                            response.getRelatedChats().add(value);
                        }
                    });
                    System.out.println("send initial information to "+sendFrom);
                    break;
                case 4:
                    response.setRid(4);
                    int id = chatList.size();
                    Chat chat = communication.getRelatedChats().get(0);
                    chat.setId(id);
                    chat.setHasRead(true);
                    chatList.put(id,chat);
                    response.getRelatedChats().add(chat);
                    clientOutputs.forEach((key, value) -> {
                        if(chat.getUserList().contains(key) && !Objects.equals(key, sendFrom)){
                            try {
                                value.writeObject(response);
                                System.out.println("send chat to "+key);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    break;
                case 5:
                    response.setRid(5);
                    response.setSendFrom(sendFrom);
                    Chat chat2 = communication.getRelatedChats().get(0);
                    int id2 = chat2.getId();
                    chat2.setHasRead(false);
                    chatList.put(id2,chat2);
                    response.getRelatedChats().add(chat2);
                    clientOutputs.forEach((key, value) -> {
                        if(chat2.getUserList().contains(key) && !Objects.equals(key, sendFrom)){
                            try {
                                value.writeObject(response);
                                System.out.println("send message to "+key);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
            }
            return response;
        }

        public Server(int port) throws Exception{
            this.userList = new ConcurrentHashMap<>();
            this.chatList = new ConcurrentHashMap<>();
            this.port = port;
            this.serverSocket = new ServerSocket(port);
            this.clientOutputs = new ConcurrentHashMap<>();
        }
    }

    public static void main(String[] args) throws Exception {

        System.out.println("Starting server");

        Server server = new Server(9999);


        while (true){
            Socket socket = server.getServerSocket().accept();
            Thread userThread = new Thread(() -> {
                try {
                    ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream input = new ObjectInputStream(socket.getInputStream());

                    Communication iniCommunication = (Communication) input.readObject();
                    String sendFrom = iniCommunication.getSendFrom();
                    server.clientOutputs.put(sendFrom, output);

                    Communication iniResponse = server.handleCommunication(iniCommunication);
                    output.writeObject(iniResponse);

                    while (true) {
                        Communication communication = (Communication) input.readObject();

                        Communication response = server.handleCommunication(communication);
                        output.writeObject(response);
                    }
                } catch (ClassNotFoundException | IOException e) {
                    System.out.println("one user log out.");
                } finally {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            userThread.start();


        }
    }
}
