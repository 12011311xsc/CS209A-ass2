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
import javafx.util.Callback;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
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

    User user;

    private Socket socket;

    private String userAddress;

    private ObjectOutputStream output;

    private ObjectInputStream input;

    private ControllerThread controllerThread;

    private Map<String, User> onlineUsers;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        Dialog<String> dialog = new TextInputDialog();
        dialog.setTitle("Login");
        dialog.setHeaderText(null);
        dialog.setContentText("Username:");

        Optional<String> in = dialog.showAndWait();
        if (in.isPresent() && !in.get().isEmpty()) {
            /*
               TODO: Check if there is a user with the same name among the currently logged-in users,
                     if so, ask the user to change the username
             */
            String username = in.get();
            user = new User(username);
            chatList.setCellFactory(userListView -> new ChatListCell());
            userAddress = "localhost";
            try {
                socket = new Socket(userAddress,9999);
                output = new ObjectOutputStream(socket.getOutputStream());
                input = new ObjectInputStream(socket.getInputStream());
                Communication communication = new Communication(1,username);
                output.writeObject(communication);
                controllerThread = new ControllerThread(socket,this,output,input);
                controllerThread.start();
            } catch (IOException e) {
                System.out.println("The server is closed. Quit now.");
                Platform.exit();
            }
            currentUsername.setText("Current User: "+username);
        } else {
            System.out.println("Invalid username " + in + ", exiting");
            Platform.exit();
        }

        chatContentList.setCellFactory(new MessageCellFactory());
        onlineUsers = new ConcurrentHashMap<>();

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

//        chatList.getItems().add()
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
        chatContentList.getItems().add(new Message(System.currentTimeMillis()+28800000,"","",inputArea.getText()));
        inputArea.setText("");
    }

    private void handleCommunication(Communication communication){
        switch (communication.getRid()){
            case 1:
                if(communication.getRelatedUsers().size() == 0){
                    //TODO:
                    System.out.println("The user is online.");
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
                        currentOnlineCnt.setText("Online: "+(onlineUsers.size()+1));
                    });
                }
                break;
            case 2:
                //TODO:log out
                break;
            case 3:
                for(User user1: communication.getRelatedUsers()){
                    onlineUsers.put(user1.getUsername(),user1);
                }
                Platform.runLater(() -> {
                    currentOnlineCnt.setText("Online: "+(onlineUsers.size()+1));
                });
                //TODO:get chat information
                break;
        }
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

    private class ChatListCell extends ListCell<Chat> {
        @Override
        protected void updateItem(Chat chat, boolean empty) {
            super.updateItem(chat, empty);
            if (empty || chat == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText("");
            }
        }
    }

    private class ControllerThread extends Thread{
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
            while (true){
                try {
                    Communication communication = (Communication) input.readObject();
                    controller.handleCommunication(communication);
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
