package ru.anatomica.messenger;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import ru.anatomica.messenger.gson.*;
import java.io.*;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class MessageService extends IntentService {

    public static String MESSAGING_SERVICE_NAME = "ru.anatomica.Messenger";
    private static final String HOST_ADDRESS_PROP = "server.address";
    private static final String HOST_PORT_PROP = "server.port";
    private static final String STOP_SERVER_COMMAND = "/end";

    private String hostAddress;
    private int hostPort;

    public ClientListMessage clientListMessage;
    private boolean needStopServerOnClosed;
    private MainActivity mainActivity;
    public EditText textMessage;
    private ChatHistory chatHistory;
    private Network network;
    private String nickname;

    MessageService(MainActivity mainActivity, boolean needStopServerOnClosed) {
        super(MESSAGING_SERVICE_NAME);
        this.textMessage = mainActivity.textMessage;
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
            serverProperties.load(mainActivity.serverAddressProp);
            hostAddress = serverProperties.getProperty(HOST_ADDRESS_PROP);
            hostPort = Integer.parseInt(serverProperties.getProperty(HOST_PORT_PROP));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read application.properties file", e);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port value", e);
        }
    }

    public void startConnectionToServer() {
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
            mainActivity.runOnUiThread(this::loginToChat);
            chatHistory.loadChatHistory();
            mainActivity.runOnUiThread(mainActivity::loadAllContacts);
            mainActivity.runOnUiThread(mainActivity::createBtn);
            Activity activity;
            checkChange();
        } else {
            if (message.startsWith("{") && message.endsWith("}")) {
                Message msg = Message.fromJson(message);
                clientListMessage = msg.clientListMessage;
                mainActivity.runOnUiThread(this::clientList);
            } else {
                // String messageText = mainActivity.intent.getStringExtra(message);
                if (message.equals("Неверные логин/пароль!")) {
                    mainActivity.runOnUiThread(() -> serviceMessage(message));
                }
                if (message.equals("Учетная запись уже используется!")) {
                    mainActivity.runOnUiThread(() -> serviceMessage(message));
                }
                if (message.endsWith("выберите другой Логин!")) {
                    mainActivity.runOnUiThread(() -> serviceMessage(message));
                }
                if (message.endsWith("Пожалуйста, войдите в\nприложение заного!")) {
                    mainActivity.runOnUiThread(() -> serviceMessage(message));
                    logoutAfterReg();
                }
                if (!message.equals("")) {
                    if (!message.endsWith("лайн!")) {
                        if (!message.equals("Неверные логин/пароль!")) {
                            if (!message.equals("Новые сообщения:")) {
                                if (!message.equals("Учетная запись уже используется!")) {
                                    if (!message.endsWith("выберите другой Логин!")) {
                                        if (!message.endsWith("Пожалуйста, войдите в\nприложение заного!"))
                                            chatHistory.writeChatHistory(message);
                                    }
                                }
                            }
                        }
                    }
                }
                mainActivity.textArea.append(message + System.lineSeparator());
            }
        }
    }

    void clientList(){
        mainActivity.clientList();
    }

    public void changeToChoose() {
        mainActivity.changeLayout.setVisibility(View.VISIBLE);
        mainActivity.registerLayout.setVisibility(View.INVISIBLE);
        mainActivity.loginLayout.setVisibility(View.INVISIBLE);
        mainActivity.chatLayout.setVisibility(View.INVISIBLE);
        mainActivity.messageLayout.setVisibility(View.INVISIBLE);
    }

    public void changeToReg() {
        mainActivity.changeLayout.setVisibility(View.INVISIBLE);
        mainActivity.registerLayout.setVisibility(View.VISIBLE);
        mainActivity.loginLayout.setVisibility(View.INVISIBLE);
        mainActivity.chatLayout.setVisibility(View.INVISIBLE);
        mainActivity.messageLayout.setVisibility(View.INVISIBLE);
    }

    public void changeToLogin() {
        mainActivity.changeLayout.setVisibility(View.INVISIBLE);
        mainActivity.registerLayout.setVisibility(View.INVISIBLE);
        mainActivity.loginLayout.setVisibility(View.VISIBLE);
        mainActivity.chatLayout.setVisibility(View.INVISIBLE);
        mainActivity.messageLayout.setVisibility(View.INVISIBLE);
    }

    public void loginToChat() {
        mainActivity.changeLayout.setVisibility(View.INVISIBLE);
        mainActivity.registerLayout.setVisibility(View.INVISIBLE);
        mainActivity.loginLayout.setVisibility(View.INVISIBLE);
        mainActivity.chatLayout.setVisibility(View.VISIBLE);
        mainActivity.messageLayout.setVisibility(View.INVISIBLE);
    }

    public void loginToMessage() {
        mainActivity.changeLayout.setVisibility(View.INVISIBLE);
        mainActivity.registerLayout.setVisibility(View.INVISIBLE);
        mainActivity.loginLayout.setVisibility(View.INVISIBLE);
        mainActivity.chatLayout.setVisibility(View.INVISIBLE);
        mainActivity.messageLayout.setVisibility(View.VISIBLE);
    }

    public void checkChange() {
        try {
            TimeUnit.SECONDS.sleep(1);
            if (mainActivity.loginLayout.getVisibility() == View.VISIBLE)
                mainActivity.runOnUiThread(() -> serviceMessage("Ошибка аутентификации!"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void logoutAfterReg() {
        try {
            TimeUnit.SECONDS.sleep(5);
            mainActivity.logout();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void serviceMessage(String message) {
        Toast.makeText(mainActivity, message, Toast.LENGTH_LONG).show();
    }

    void close() throws IOException {
        if (needStopServerOnClosed) {
            sendMessage(STOP_SERVER_COMMAND);
        }
        network.close();
        System.exit(0);
    }
}