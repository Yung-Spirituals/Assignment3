package no.ntnu.datakomm.chat;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import static java.lang.Thread.sleep;

import javafx.collections.ObservableList;

/**
 * The graphical interface containing all the user interface controls: buttons, inputs, etc.
 * It implements the "interface logic" and sends commands to a TcpClient. To get server
 * response back from the TcpClient, this class implements ChatListener interface - it can
 * react on every incoming event.
 */
public class GUIController implements ChatListener {

    // The following variables are bound to GUI controls. For example, submitBtn is bound to the
    // "Submit" button in the GUI. The binding is done by JavaFX, by using correct attributes in the FXML layout file.

    @FXML
    private Button submitBtn;

    @FXML
    private Button connectBtn;

    @FXML
    private Button loginBtn;

    @FXML
    private Button helpBtn;

    @FXML
    private VBox userList;

    @FXML
    private VBox textOutput;

    @FXML
    private TextArea textInput;

    @FXML
    private TextField hostInput;

    @FXML
    private TextField portInput;

    @FXML
    private TextField loginInput;

    @FXML
    private TitledPane serverStatus;

    @FXML
    private ScrollPane outputScroll;

    // The TCP client that can connect, send commands, etc.
    private TCPClient tcpClient;

    // Active user list will be refreshed periodically. This will be done on a separate CPU Thread to avoid blocking
    // the GUI.
    private Thread userPollThread;

    /**
     * Called by the FXML loader after the labels declared above are injected:
     */
    public void initialize() {
        tcpClient = new TCPClient();
        hostInput.setText("datakomm.work");
        portInput.setText("1300");
        textOutput.heightProperty().addListener((observable, oldValue, newValue)
                -> outputScroll.setVvalue(1.0));
        setKeyAndClickListeners();
    }

    /**
     * Initialize handling for all GUI events: clicking on buttons, and key presses
     */
    private void setKeyAndClickListeners() {
        connectBtn.setOnMouseClicked(event -> {
            // Mouse clicked on "Connect" button
            if (tcpClient.isConnectionActive()) {
                tcpClient.disconnect();
                updateButtons(false);
            } else {
                setupConnection(hostInput.getText(), portInput.getText());
            }
        });
        loginBtn.setOnMouseClicked(event -> {
            // Mouse clicked on "Login" button
            tcpClient.tryLogin(loginInput.getText());
            loginInput.setText("");
        });
        textInput.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER) && event.isShiftDown()) {
                // When Shift+"Enter" is pressed in the message input box: start a new line in the message
                textInput.setText(textInput.getText() + "\n");
                textInput.requestFocus();
                textInput.end();
            } else if (event.getCode().equals(KeyCode.ENTER)) {
                // When "Enter" is pressed in the message input box: submit the message
                inputSubmit();
                event.consume(); // This is needed to disable beeping sound
            }
        });
        submitBtn.setOnMouseClicked(event -> {
            // Mouse clicked on "Submit" button
            inputSubmit();
            textInput.requestFocus();
        });
        // Mouse clicked on "Help" button
        helpBtn.setOnMouseClicked(event -> tcpClient.askSupportedCommands());
    }

    /**
     * Take the message from the text input box, send it to the server
     */
    private void inputSubmit() {
        String msgToSend = textInput.getText();
        if (!msgToSend.isEmpty()) {
            TextMessage msg;
            if (tcpClient.isConnectionActive()) {
                // Split the message in max 3 parts. If the first one is "/privmsg", then recipient is the second
                // part and the text to send is the third. Otherwise, the whole message is sent as a public message.
                String[] msgParts = msgToSend.split(" ", 3);
                if (msgParts.length == 3 && msgParts[0].equals("/privmsg")) {
                    String recipient = msgParts[1];
                    String message = msgParts[2];
                    tcpClient.sendPrivateMessage(recipient, message);
                } else {
                    tcpClient.sendPublicMessage(msgToSend);
                }
                msg = new TextMessage("", false, msgToSend);
            } else {
                msg = new TextMessage("you", false, msgToSend);
            }
            addMsgToGui(true, msg, false);
            textInput.setText("");
        }
    }

    /**
     * Add a message to the GUI chat window
     *
     * @param local   When true, this message was sent by us. When false -
     *                received from another user
     * @param msg     The message to be displayed
     * @param warning When true, this message is a warning that must be displayed to the user
     */
    private void addMsgToGui(boolean local, TextMessage msg, boolean warning) {
        // Create GUI elements, set their text and style according to what 
        // type of message this is

        HBox message = new HBox();
        VBox messageContent = new VBox();
        String senderText;
        Label messageSender = new Label();
        messageSender.getStyleClass().add("sender");
        if (msg.isPrivate()) {
            senderText = "Private from " + msg.getSender() + ":";
            messageSender.getStyleClass().add("private");
        } else {
            senderText = msg.getSender() + ":";
        }
        messageSender.setText(senderText);
        Label messageText = new Label(msg.getText());
        ObservableList<String> textStyle = messageText.getStyleClass();
        textStyle.add("message");
        messageText.setWrapText(true);
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        spacer.setMinSize(10, 1);
        if (warning) {
            // This message is a warning/info, add specific style to it
            messageContent.getChildren().addAll(messageText);
            message.getChildren().addAll(messageContent);
            if (msg.getSender().equals("err")) {
                textStyle.add("warning");
            } else {
                textStyle.add("info");
            }
        } else {
            // Regular message
            if (local) {
                if (tcpClient.isConnectionActive()) {
                    textStyle.add("sentMessage");
                } else {
                    // Trying to send a message without an active connection
                    serverStatus.setText("Please login to send messages to server");
                    textStyle.add("failedMessage");
                }
                // Add empty space first (left), then the message (right)
                messageContent.getChildren().addAll(messageText);
                message.getChildren().addAll(spacer, messageContent);
            } else {
                textStyle.add("otherMessage");
                // Add message first (left), then empty space (right)
                messageContent.getChildren().addAll(messageSender, messageText);
                message.getChildren().addAll(messageContent, spacer);
            }
        }
        textOutput.getChildren().add(message);
    }

    /**
     * Start a connection to the server: try to connect Socket, log in and start
     * listening for incoming messages
     *
     * @param host The host to connect to (domain name or IP address)
     * @param port Remote TCP port
     */
    private void setupConnection(String host, String port) {
        serverStatus.setText("Trying to connect...");
        connectBtn.setText("Connecting...");
        connectBtn.setDisable(true);

        // Run the connection in a new background thread to avoid GUI freeze
        Thread connThread = new Thread(() -> {
            boolean connected = tcpClient.connect(host, Integer.parseInt(port));
            if (connected) {
                // Connection established, start listening processes
                tcpClient.addListener(this);
                tcpClient.startListenThread();
                startUserPolling();
            }
            updateButtons(connected);
        });
        connThread.start();
    }

    /**
     * Update texts and enabled/disabled state of GUI buttons according to
     * connection success.
     *
     * @param connected When true, client has successfully connected to the
     *                  server. When false, connection failed
     */
    private void updateButtons(boolean connected) {
        String status;
        String connBtnText;
        if (connected) {
            status = "Connection to server established";
            connBtnText = "Disconnect";
        } else {
            status = "Not connected: " + tcpClient.getLastError();
            connBtnText = "Connect";
        }
        // Make sure this will be executed on GUI thread
        Platform.runLater(() -> {
            // Update button texts
            serverStatus.setText(status);
            connectBtn.setText(connBtnText);
            // Connection button was disabled while connection was in progress, 
            // now we enable it
            connectBtn.setDisable(false);

            // Enable/disable buttons: Login, help, submit
            loginBtn.setDisable(!connected);
            submitBtn.setDisable(!connected);
            helpBtn.setDisable(!connected);
        });

    }

    ///////////////////////////////////////////////////////////////////////
    // The methods below are called by the associated TcpClient (facade 
    // object) in another background thread when messages are received
    // from the server.
    ///////////////////////////////////////////////////////////////////////

    /**
     * Start a new thread that will poll the server for currently active users
     */
    private void startUserPolling() {
        // Make sure we have just one polling thread, not duplicates
        if (userPollThread == null) {

            userPollThread = new Thread(() -> {
                ////////////////////////////////////////////////////////////////
                // This block of code will run in the polling thread
                ////////////////////////////////////////////////////////////////
                long threadId = Thread.currentThread().getId();
                System.out.println("Started user polling in Thread "
                        + threadId);
                while (tcpClient.isConnectionActive()) {
                    // TcpClient will ask server to send the latest user list. The response from the server will
                    // not be handled here! Here we only ask for update and go to sleep. Then repeat.
                    tcpClient.refreshUserList();
                    try {
                        sleep(3000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                System.out.println("User polling thread " + threadId + " exiting...");
                // Make sure we start the thread again next time
                userPollThread = null;
                ////////////////////////////////////////////////////////////////
                // EOF polling thread code
                ////////////////////////////////////////////////////////////////
            });

            userPollThread.start();
        }
    }

    /**
     * This method is called (by the TcpClient) when a login procedure is done: either it succeeded
     * or failed.
     *
     * @param success when true, the client has logged in, when false, login
     *                failed
     * @param errMsg  Error message in case of failure, or null on successful
     *                login
     */
    @Override
    public void onLoginResult(boolean success, String errMsg) {
        // Update the GUI. Do it on the GUI thread with Platform.runLater()
        Platform.runLater(() -> {
            if (success) {
                serverStatus.setText("Server - login successful");
            } else {
                serverStatus.setText("Server - login failed");
                addMsgToGui(true, new TextMessage("err", false, errMsg), true);
            }
        });
    }

    /**
     * This method is called when an incoming text message is received
     *
     * @param message Message that from another (chat server has forwarded it)
     */
    @Override
    public void onMessageReceived(TextMessage message) {
        // Show the message in the GUI. Do it on the GUI thread.
        Platform.runLater(() -> addMsgToGui(false, message, false));
    }

    /**
     * This method is called when an error happened when we tried to send
     * message to the server (the message was not sent to necessary recipients)
     *
     * @param errMsg Error message
     */
    @Override
    public void onMessageError(String errMsg) {
        // Show error message in the GUI. Do it on the GUI thread.
        Platform.runLater(() -> addMsgToGui(true, new TextMessage("err", false,
                "Error: " + errMsg), true));
    }

    /**
     * This method is called when a list of currently connected users is
     * received
     *
     * @param usernames Array with usernames of currently connected users
     */
    @Override
    public void onUserList(String[] usernames) {
        // Update the user list. Do it on the GUI thread.
        Platform.runLater(() -> {
            userList.getChildren().clear();
            for (String user : usernames) {
                Label text = new Label(user);
                text.getStyleClass().add("user");
                // Set an "on-click" listener for the item in the user list - allow to send a private message
                text.setOnMouseClicked(event -> {
                    textInput.setText("/privmsg " + user + " ");
                    textInput.requestFocus();
                    textInput.end();
                });
                userList.getChildren().add(text);
            }
        });
    }

    /**
     * This method is called when a list of currently supported commands is
     * received
     *
     * @param commands Array with supported commands
     */
    @Override
    public void onSupportedCommands(String[] commands) {
        // Show the commands in the GUI. Do it on the GUI thread.
        Platform.runLater(() -> {
            StringBuilder listOfCommands = new StringBuilder(
                    "Commands available: ");
            for (String c : commands) {
                listOfCommands.append(c).append(" ");
            }
            listOfCommands.append(
                    "\nNB! These are chat protocol commands and won't work by just typing them");
            addMsgToGui(true, new TextMessage("info", false, "Info: "
                    + listOfCommands.toString()), true);
        });
    }

    /**
     * This method is called when the server did not understand the last command
     *
     * @param errMsg Error message
     */
    @Override
    public void onCommandError(String errMsg) {
        // Shoe error message. Do it on the GUI thread.
        Platform.runLater(() -> {
            TextMessage msg = new TextMessage("err", false, "Error: " + errMsg);
            addMsgToGui(true, msg, true);
        });
    }

    /**
     * This method is called when connection (socket) is closed by the remote
     * end (server).
     */
    @Override
    public void onDisconnect() {
        System.out.println("Socket closed by the remote end");
        updateButtons(false);
    }
}
