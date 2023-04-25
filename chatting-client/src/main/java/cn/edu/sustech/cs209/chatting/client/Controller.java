package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Chat;
import cn.edu.sustech.cs209.chatting.common.Communication;
import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class Controller implements Initializable {

    @FXML
    public Label currentUsername;

    @FXML
    public Label currentOnlineCnt;

    @FXML
    public TextArea inputArea;

    @FXML
    ListView<Chat> chatList;

    @FXML
    ListView<Message> chatContentList;

    @FXML
    private Label displayWholeName;

    User user;

    private Socket socket;

    private String userAddress;

    private ObjectOutputStream output;

    private ObjectInputStream input;

    private ControllerThread controllerThread;

    private Map<String, User> onlineUsers;

    private Chat currentChat = null;

    private boolean isOnline;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        Dialog<String> dialog = new TextInputDialog();
        dialog.setTitle("Login");
        dialog.setHeaderText(null);
        dialog.setContentText("Username:");

        Optional<String> in = dialog.showAndWait();
        if (in.isPresent() && !in.get().isEmpty()) {
            /*
               TODO: Check if there is a user with the same name
                        among the currently logged-in users,
                     if so, ask the user to change the username
             */
            String username = in.get();
            isOnline = true;
            user = new User(username);
            user.setOnline(true);
            chatList.setCellFactory(userListView -> new ChatListCell());
            chatContentList.setCellFactory(new MessageCellFactory());
            chatList.getSelectionModel().selectedItemProperty().addListener(
                    (observable, oldValue, newValue) -> {
                if (newValue != null){
                    currentChat = newValue;
                    updateTitle();
                    chatContentList.getItems().clear();
                    for (Message message : currentChat.getMessageDeque()) {
                        chatContentList.getItems().add(message);
                    }
                }
                else {
                    currentChat = null;
                    updateTitle();
                    chatContentList.getItems().clear();
                }
            });
            onlineUsers = new ConcurrentHashMap<>();
            userAddress = "localhost";
            try {
                socket = new Socket(userAddress, 9999);
                output = new ObjectOutputStream(socket.getOutputStream());
                input = new ObjectInputStream(socket.getInputStream());
                Communication communication = new Communication(1, username);
                output.writeObject(communication);
                controllerThread = new ControllerThread(socket, this, output, input);
                controllerThread.start();
            } catch (IOException e) {
                System.out.println("The server is closed. Quit now.");
                Platform.exit();
            }
            currentUsername.setText("Current User: "+username);
        }
        else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Empty username");
            alert.setHeaderText(null);
            alert.setContentText("The username is empty.");

            alert.showAndWait();
            Platform.exit();
        }
    }

    @FXML
    public void createPrivateChat() {
        AtomicReference<String> temUser = new AtomicReference<>();

        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();

        // FIXME: get the user list from server, the current user's name should be filtered out
        userSel.getItems().addAll(onlineUsers.keySet());

        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            temUser.set(userSel.getSelectionModel().getSelectedItem());
            stage.close();
        });

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();

        // TODO: if the current user already chatted with the selected user, just open the chat with that user
        // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name
        if (temUser.get() == null){
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No user is chose");
            alert.setHeaderText(null);
            alert.setContentText("No user is chose. Please choose one user.");

            alert.showAndWait();
        }
        else if (chatExist(temUser.get()).getUserList().size() > 0){
            currentChat = chatExist(temUser.get());
            updateTitle();
            chatList.getSelectionModel().select(currentChat);
            chatContentList.getItems().clear();
            for (Message message : currentChat.getMessageDeque()) {
                chatContentList.getItems().add(message);
            }
        }
        else {
            ArrayList<String> userArrayList = new ArrayList<>();
            userArrayList.add(user.getUsername());
            userArrayList.add(temUser.get());
            Chat chat = new Chat(true, userArrayList);
            chat.setHasRead(true);
            Communication communication = new Communication(4, user.getUsername());
            communication.getRelatedChats().add(chat);
            try {
                output.writeObject(communication);
            }catch (IOException e){
                System.out.println("don't create chat");
            }
        }

//        chatList.getItems().add()
    }

    private Chat chatExist(String userName) {
        for (Chat chat:chatList.getItems()){
            if(chat.isPrivate() && chat.getUserList().contains(userName)){
                return chat;
            }
        }
        return new Chat(true,new ArrayList<>());
    }

    /**
     * A new dialog should contain a multi-select list, showing all user's name.
     * You can select several users that will be joined in the group chat, including yourself.
     * <p>
     * The naming rule for group chats is similar to WeChat:
     * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
     * UserA, UserB, UserC... (10)
     * If there are <= 3 users: do not display the ellipsis, for example:
     * UserA, UserB (2)
     */
    @FXML
    public void createGroupChat() {
        AtomicReference<ArrayList<String>> temUserList = new AtomicReference<>();
        temUserList.set(new ArrayList<>());

        Stage stage = new Stage();
        ArrayList<CheckBox> checkBoxes = new ArrayList<>();
        onlineUsers.forEach((key, value) -> {
            CheckBox temCheckBox = new CheckBox(key);
            checkBoxes.add(temCheckBox);
        });
        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            checkBoxes.forEach(checkBox -> {
                if(checkBox.isSelected()){
                    temUserList.get().add(checkBox.getText());
                }
            });
            stage.close();
        });

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(checkBoxes.toArray(new CheckBox[0]));
        box.getChildren().add(okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();

        if(temUserList.get().size() == 0){
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No user is chose");
            alert.setHeaderText(null);
            alert.setContentText("No user is chose. Please choose one user at least.");

            alert.showAndWait();
        }
        else {
            ArrayList<String> userArrayList = new ArrayList<>();
            userArrayList.add(user.getUsername());
            userArrayList.addAll(temUserList.get());
            Chat chat = new Chat(false,userArrayList);
            chat.setHasRead(true);
            Communication communication = new Communication(4,user.getUsername());
            communication.getRelatedChats().add(chat);
            try{
                output.writeObject(communication);
            }catch (IOException e){
                System.out.println("don't create chat");
            }
        }


    }

    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    public void doSendMessage() {
        // TODO
        if(inputArea.getText().length() == 0){
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Send empty message");
            alert.setHeaderText(null);
            alert.setContentText("Cannot send empty message");

            alert.showAndWait();
        }
        else if(currentChat == null){
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No chat is chose");
            alert.setHeaderText(null);
            alert.setContentText("No chat is chose. Please choose one chat.");

            alert.showAndWait();
        }
        else{
            Message message = new Message(System.currentTimeMillis()+28800000,user.getUsername(),"",inputArea.getText());
            currentChat.getMessageDeque().addLast(message);
            currentChat.setHasRead(false);
//            chatContentList.getItems().clear();
//            for (Message message1 : currentChat.getMessageDeque()) {
//                chatContentList.getItems().add(message1);
//            }
            Communication communication = new Communication(5,user.getUsername());
            communication.getRelatedChats().add(currentChat);
            try{
                output.writeObject(communication);
            }catch (IOException e){
                System.out.println("don't send message");
            }
//        chatContentList.getItems().add(new Message(System.currentTimeMillis()+28800000,"","",inputArea.getText()));
            inputArea.setText("");
        }

    }

    private void handleCommunication(Communication communication){
        switch (communication.getRid()){
            case 1:
                if(communication.getRelatedUsers().size() == 0){
                    //TODO:
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Online user");
                        alert.setHeaderText(null);
                        alert.setContentText("The user is online. You can't login now.");

                        alert.showAndWait();
                        Platform.exit();
                    });
                    try {
                        input.close();
                        output.close();
                        socket.close();
                        isOnline = false;
                    }catch (IOException e){
                        System.out.println("close user thread failed.");
                    }
                }
                else if(Objects.equals(communication.getRelatedUsers().get(0).getUsername(), user.getUsername())){
                    System.out.println("Login succeed.");
                    Communication response = new Communication(3,user.getUsername());
                    try{
                        output.writeObject(response);
                    }catch (IOException e){
                        System.out.println("don't get init information.");
                    }

                }
                else {
                    onlineUsers.put(communication.getRelatedUsers().get(0).getUsername(),communication.getRelatedUsers().get(0));
                    Platform.runLater(() -> {
                        StringBuilder s = new StringBuilder("Online user: ");
                        onlineUsers.forEach((key, value) -> s.append(key).append(", "));
                        s.delete(s.length()-2,s.length());
                        currentOnlineCnt.setText(s.toString());
                    });
                }
                break;
            case 2:
                //TODO:log out
                if(Objects.equals(communication.getRelatedUsers().get(0).getUsername(), user.getUsername())){
                    try {
                        input.close();
                        output.close();
                        socket.close();
                        isOnline = false;
                    }catch (IOException e){
                        System.out.println("close user thread failed.");
                    }
                }
                else {
                    onlineUsers.remove(communication.getRelatedUsers().get(0).getUsername());
                    Platform.runLater(() -> {
                        StringBuilder s = new StringBuilder("Online user: ");
                        onlineUsers.forEach((key, value) -> s.append(key).append(", "));
                        s.delete(s.length()-2,s.length());
                        currentOnlineCnt.setText(s.toString());
                    });
                }
                break;
            case 3:
                for(User user1: communication.getRelatedUsers()){
                    onlineUsers.put(user1.getUsername(),user1);
                }
                Platform.runLater(() -> {
                    for (Chat chat: communication.getRelatedChats()){
                        getDisplayName(chat);
                        chatList.getItems().add(0,chat);
                    }
                    StringBuilder s = new StringBuilder("Online user: ");
                    onlineUsers.forEach((key, value) -> s.append(key).append(", "));
                    s.delete(s.length()-2,s.length());
                    currentOnlineCnt.setText(s.toString());
                });
                break;
            case 4:
                Chat chat = communication.getRelatedChats().get(0);
                getDisplayName(chat);
                chatList.getItems().add(0,chat);
                break;
            case 5:
                Chat chat2 = communication.getRelatedChats().get(0);
                assert chat2.getMessageDeque().peekLast() != null;
                if(Objects.equals(chat2.getMessageDeque().peekLast().getSentBy(), user.getUsername())){
                    chat2.setHasRead(true);
                }
                getDisplayName(chat2);
                for (Chat temChat : chatList.getItems()){
                    if(chat2.getId() == temChat.getId()){
                        Platform.runLater(() -> {
                            boolean isCurrent = currentChat != null && currentChat.getId() == temChat.getId();
                            chatList.getItems().remove(temChat);
                            chatList.getItems().add(0,chat2);
                            if(isCurrent){
                                currentChat = chat2;
                                updateTitle();
                                chatList.getSelectionModel().select(currentChat);
                                chatContentList.getItems().clear();

                                for (Message message : currentChat.getMessageDeque()) {
                                    chatContentList.getItems().add(message);
                                }
                            }
                        });
                    }
                }
                break;
        }
    }

    private void getDisplayName(Chat chat2) {
        if(chat2.isPrivate()){
            if(!Objects.equals(chat2.getUserList().get(0), user.getUsername())){
                chat2.setDisplayName(chat2.getUserList().get(0));
            }
            else {
                chat2.setDisplayName(chat2.getUserList().get(1));
            }
        }
        else {
            Collections.sort(chat2.getUserList());
            StringBuilder displayName = new StringBuilder();
            for (int i = 0;i < Math.min(3,chat2.getUserList().size());i++){
                displayName.append(chat2.getUserList().get(i)).append(", ");
            }
            displayName.delete(displayName.length()-2,displayName.length());
            if(chat2.getUserList().size() <= 3){
                chat2.setDisplayName(displayName.toString());
            }
            else {
                displayName.append("... (");
                displayName.append(chat2.getUserList().size());
                displayName.append(")");
                chat2.setDisplayName(displayName.toString());
            }
        }
        if(!chat2.isHasRead()){
            String displayName = chat2.getDisplayName();
            displayName += " (new message)";
            chat2.setDisplayName(displayName);
        }
    }

    private void updateTitle(){
        if(currentChat != null){
            if(currentChat.isPrivate()){
                if(!Objects.equals(currentChat.getUserList().get(0), user.getUsername())){
                    displayWholeName.setText(currentChat.getUserList().get(0));
                }
                else {
                    displayWholeName.setText(currentChat.getUserList().get(1));
                }
            }
            else {
                Collections.sort(currentChat.getUserList());
                StringBuilder displayName = new StringBuilder();
                for (int i = 0;i < currentChat.getUserList().size();i++){
                    displayName.append(currentChat.getUserList().get(i)).append(", ");
                }
                displayName.delete(displayName.length()-2,displayName.length());
                displayWholeName.setText(displayName.toString());
            }
        }
        else {
            displayWholeName.setText("");
        }

    }

    private void handleServerClosed(){
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Server closed");
            alert.setHeaderText(null);
            alert.setContentText("The server is closed. You have to quit now.");

            alert.showAndWait();
            Platform.exit();
        });
    }

    public void onClose(WindowEvent event) {
        System.out.println("VBox's stage is closing");
        Communication communication = new Communication(2,user.getUsername());
        try{
            output.writeObject(communication);
        }catch (IOException e){
            System.out.println("don't send log out");
        }
    }

    public void setStage(Stage stage) {
        stage.setOnCloseRequest(this::onClose);
    }

    /**
     * You may change the cell factory if you changed the design of {@code Message} model.
     * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
     */
    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
        @Override
        public ListCell<Message> call(ListView<Message> param) {
            return new ListCell<Message>() {

                @Override
                public void updateItem(Message msg, boolean empty) {
                    super.updateItem(msg, empty);
                    if (empty || Objects.isNull(msg)) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    HBox wrapper = new HBox();
                    Label nameLabel = new Label(msg.getSentBy());
                    Label msgLabel = new Label(msg.getData());

                    nameLabel.setPrefSize(50, 20);
                    nameLabel.setWrapText(true);
                    nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

                    if (user.getUsername().equals(msg.getSentBy())) {
                        wrapper.setAlignment(Pos.TOP_RIGHT);
                        wrapper.getChildren().addAll(msgLabel, nameLabel);
                        msgLabel.setPadding(new Insets(0, 20, 0, 0));
                    } else {
                        wrapper.setAlignment(Pos.TOP_LEFT);
                        wrapper.getChildren().addAll(nameLabel, msgLabel);
                        msgLabel.setPadding(new Insets(0, 0, 0, 20));
                    }

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);
                }
            };
        }
    }

    private static class ChatListCell extends ListCell<Chat> {
        @Override
        protected void updateItem(Chat chat, boolean empty) {
            super.updateItem(chat, empty);
            if (empty || chat == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(chat.getDisplayName());
            }
        }
    }

    private static class ControllerThread extends Thread{
        private Socket socket;

        private Controller controller;

        ObjectOutputStream output;

        ObjectInputStream input;

        public ControllerThread(Socket socket, Controller controller, ObjectOutputStream output, ObjectInputStream input){
            this.socket = socket;
            this.controller = controller;
            this.output = output;
            this.input = input;
        }

        public void run(){
            while (controller.isOnline){
                try {
                    Communication communication = (Communication) input.readObject();
                    controller.handleCommunication(communication);
                } catch (IOException | ClassNotFoundException e) {
                    controller.isOnline = false;
                    controller.handleServerClosed();
                    System.out.println("Thread end.");
                }
            }
        }
    }
}
