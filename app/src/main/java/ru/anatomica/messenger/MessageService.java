package ru.anatomica.messenger;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
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
    public PublicMessage publicMessage;
    private boolean needStopServerOnClosed;
    private MainActivity mainActivity;
    public EditText textMessage;
    private ChatHistory chatHistory;
    private Network network;
    private String nickname;
    private String whoWriteMe;

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
        try {
        if (!textMessage.getText().toString().equals("")) {
            if (mainActivity.selectedNickname == null || mainActivity.selectedNickname.equals("Пользователи группы:")) {
                mainActivity.fos = mainActivity.openFileOutput(mainActivity.selectedButton + mainActivity.fileHistory, Context.MODE_APPEND);
                chatHistory.writeChatHistory("Я: " + textMessage.getText().toString());
            }
            if (!mainActivity.selectedNickname.equals("Пользователи группы:")) {
                mainActivity.fos = mainActivity.openFileOutput(mainActivity.selectedNickname + mainActivity.fileHistory, Context.MODE_APPEND);
                chatHistory.writeChatHistory("Я: " + textMessage.getText().toString());
            }
        } } catch (FileNotFoundException e) {
                e.printStackTrace();
        }
    }

    public void processRetrievedMessage(String message) throws IOException {
        if (message.startsWith("/authok")) {
            nickname = message.split("\\s+")[1];
            mainActivity.nickName = nickname;
            mainActivity.runOnUiThread(this::loginToChat);
            mainActivity.runOnUiThread(mainActivity::loadAllContacts);
            mainActivity.runOnUiThread(mainActivity::createBtn);
            checkChange();
            return;
        }
        if (message.equals("") || message.endsWith("лайн!") || message.equals("Неверные логин/пароль!") ||
                message.startsWith("Новые сообщения") || message.equals("Учетная запись уже используется!") ||
                message.endsWith("выберите другой Логин!") || message.equals("Сервер: Этот клиент не подключен!")) {
            mainActivity.runOnUiThread(() -> serviceMessage(message));
            return;
        }
        if (message.endsWith("Пожалуйста, войдите в\nприложение заного!")) {
            mainActivity.runOnUiThread(() -> serviceMessage(message));
            logoutAfterReg();
            return;
        } else {
            Message m = Message.fromJson(message);
            switch (m.command) {
                case CLIENT_LIST:
                    clientListMessage = m.clientListMessage;
                    mainActivity.runOnUiThread(this::clientList);
                    break;
                case PUBLIC_MESSAGE:
                    PublicMessage publicMessage = m.publicMessage;
                    if (mainActivity.messageLayout.getVisibility() == View.VISIBLE && mainActivity.selectedButton.equals(publicMessage.from)) {
                        mainActivity.textArea.append(publicMessage.from + " : " + publicMessage.message + System.lineSeparator());
                        chatHistory.outputPath("", publicMessage.from, mainActivity.fileHistory, publicMessage.from, publicMessage.message);
                    }
                    else if (mainActivity.addNewContact(publicMessage.from) && publicMessage.from != null) {
                        mainActivity.runOnUiThread(mainActivity::loadAllContacts);
                        mainActivity.runOnUiThread(mainActivity::createBtn);
                        chatHistory.outputPath("", publicMessage.from, mainActivity.fileHistory, publicMessage.from, publicMessage.message);
                    }
                    else {
                        chatHistory.outputPath("New", publicMessage.from, mainActivity.fileHistory, publicMessage.from, publicMessage.message);
                    }
                    break;
                case GROUP_MESSAGE:
                    GroupMessage groupMessage = m.groupMessage;
                    if (mainActivity.messageLayout.getVisibility() == View.VISIBLE && mainActivity.selectedButton.equals(groupMessage.nameGroup)) {
                        mainActivity.textArea.append(groupMessage.from + " : " + groupMessage.message + System.lineSeparator());
                        chatHistory.outputPath("", groupMessage.nameGroup, mainActivity.fileHistory, groupMessage.from, groupMessage.message);
                    }
                    else if (mainActivity.addNewContact(groupMessage.nameGroup) && groupMessage.from != null && groupMessage.nameGroup != null) {
                        mainActivity.runOnUiThread(mainActivity::loadAllContacts);
                        mainActivity.runOnUiThread(mainActivity::createBtn);
                        chatHistory.outputPath("", groupMessage.nameGroup, mainActivity.fileHistory, groupMessage.from, groupMessage.message);
                    }
                    else {
                        chatHistory.outputPath("New", groupMessage.nameGroup, mainActivity.fileHistory, groupMessage.from, groupMessage.message);
                    }
                    break;
                case PRIVATE_MESSAGE:
                    PrivateMessage privateMessage = m.privateMessage;
                    if (mainActivity.messageLayout.getVisibility() == View.VISIBLE && mainActivity.selectedNickname.equals(privateMessage.from)) {
                        mainActivity.textArea.append(privateMessage.from + " : " + privateMessage.message + System.lineSeparator());
                        chatHistory.outputPath("", privateMessage.from, mainActivity.fileHistory, privateMessage.from, privateMessage.message);
                    }
                    else if (mainActivity.addNewContact(privateMessage.from)) {
                        mainActivity.runOnUiThread(mainActivity::loadAllContacts);
                        mainActivity.runOnUiThread(mainActivity::createBtn);
                        chatHistory.outputPath("", privateMessage.from, mainActivity.fileHistory, privateMessage.from, privateMessage.message);
                    }
                    else {
                        chatHistory.outputPath("New", privateMessage.from, mainActivity.fileHistory, privateMessage.from, privateMessage.message);
                    }
                    break;
                case END:
                    return;
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