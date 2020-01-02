package ru.anatomica.messenger;

import android.app.IntentService;
import android.content.Intent;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import ru.anatomica.messenger.gson.*;
import java.io.*;
import java.util.Properties;

public class MessageService extends IntentService {

    public static String MESSAGING_SERVICE_NAME = "ru.anatomica.Messenger";
    private static final String HOST_ADDRESS_PROP = "server.address";
    private static final String HOST_PORT_PROP = "server.port";
    private static final String STOP_SERVER_COMMAND = "/end";

    private String hostAddress;
    private int hostPort;

    public static EditText textArea;
    public static EditText textMessage;
    private MainActivity mainActivity;
    private boolean needStopServerOnClosed;
    private ChatHistory chatHistory;
    private Network network;
    private String nickname;

    MessageService(MainActivity mainActivity, boolean needStopServerOnClosed) {
        super(MESSAGING_SERVICE_NAME);
        this.textMessage = mainActivity.textMessage;
        this.textArea = mainActivity.textArea;
        this.mainActivity = mainActivity;
        this.chatHistory = new ChatHistory(mainActivity);
        this.needStopServerOnClosed = needStopServerOnClosed;
        initialize();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
    }

    private void initialize() {
        readProperties();
        startConnectionToServer();
    }

    private void readProperties() {
        Properties serverProperties = new Properties();
        try {
            serverProperties.load(mainActivity.serverAddress);
            hostAddress = serverProperties.getProperty(HOST_ADDRESS_PROP);
            hostPort = Integer.parseInt(serverProperties.getProperty(HOST_PORT_PROP));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read application.properties2 file", e);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port value", e);
        }
    }

    private void startConnectionToServer() {
        try {
            this.network = new Network(hostAddress, hostPort, this);
        } catch (IOException e) {
            throw new ServerConnectionException("Failed to connect to server", e);
        }
    }

    public void sendMessage(String message) {
        network.send(message);
        if (!textMessage.getText().toString().equals(""))
            chatHistory.writeChatHistory("Я: " + textMessage.getText().toString());
    }

    public void processRetrievedMessage(String message) throws IOException {
        if (message.startsWith("/authok")) {
            nickname = message.split("\\s+")[1];
            mainActivity.nickName = nickname;
            mainActivity.runOnUiThread(this::changeLayout);
            chatHistory.loadChatHistory();
            checkChange();
        } else {
            if (message.startsWith("{") && message.endsWith("}")) {
                Message msg = Message.fromJson(message);
                ClientListMessage clientListMessage = msg.clientListMessage;
            } else {
                // String messageText = mainActivity.intent.getStringExtra(message);
                textArea.append(message + System.lineSeparator());
                if (message.equals("Неверные логин/пароль!")) {
                    mainActivity.runOnUiThread(mainActivity::wrongPass);
                }
                if (!message.equals("")) {
                    if (!message.endsWith("лайн!")) {
                        if (!message.equals("Неверные логин/пароль!"))
                            chatHistory.writeChatHistory(message);
                    }
                }
            }
        }
    }

    public void changeLayout() {
//        mainActivity.background.setVisibility(View.INVISIBLE);
        mainActivity.loginField.setVisibility(View.INVISIBLE);
        mainActivity.passField.setVisibility(View.INVISIBLE);
        mainActivity.sendAuth.setVisibility(View.INVISIBLE);
        mainActivity.spinner.setVisibility(View.VISIBLE);
        mainActivity.textArea.setVisibility(View.VISIBLE);
        mainActivity.textMessage.setVisibility(View.VISIBLE);
        mainActivity.sendMessageButton.setVisibility(View.VISIBLE);
    }

    public void checkChange() {
        Thread sleep = new Thread();
        try {
            sleep.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (textArea.getVisibility() != View.VISIBLE) {
            mainActivity.runOnUiThread(mainActivity::authError);
        }
    }

    void close() throws IOException {
        if (needStopServerOnClosed) {
            sendMessage(STOP_SERVER_COMMAND);
        }
        network.close();
        System.exit(0);
    }
}