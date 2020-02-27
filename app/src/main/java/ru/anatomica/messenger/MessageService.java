package ru.anatomica.messenger;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
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
    private WorkWithGroup workWithGroup;
    private MainActivity mainActivity;
    public EditText textMessage;
    public EditText textArea;
    private ChatWork chatWork;
    private ChatHistory chatHistory;
    private Network network;
    public String nickname;

    MessageService(MainActivity mainActivity, boolean needStopServerOnClosed) {
        super(MESSAGING_SERVICE_NAME);
        this.textArea = mainActivity.textArea;
        this.textMessage = mainActivity.textMessage;
        this.mainActivity = mainActivity;
        this.chatWork = new ChatWork(mainActivity, this);
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
        if (network.socket != null) {
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
    }

    public void processRetrievedMessage(String message) throws IOException {
        if (message.startsWith("/authOk")) {
            nickname = message.split("\\s+")[1];
            mainActivity.nickName = nickname;
            // if (mainActivity.chatLayout.getVisibility() == View.INVISIBLE)
            mainActivity.runOnUiThread(this::loginToChat);
            mainActivity.runOnUiThread(mainActivity::loadAllContacts);
            mainActivity.runOnUiThread(mainActivity::createBtn);
            mainActivity.saveLoginPass();
            checkChange();
            return;
        }
        if (message.equals("") || message.endsWith("лайн!") || message.equals("Неверные логин/пароль!") ||
                message.equals("Учетная запись уже используется!") || message.endsWith("выберите другой Логин!") ||
                message.equals("Сервер: Этот клиент не подключен!") || message.equals("Сервер: Этот клиент не зарегистрирован!") ||
                message.equals("Группа с данным именем существует!") || message.equals("Группа успешно создана!") ||
                message.equals("Вы отписаны от рассылки из данной группы!") || message.equals("Неверный пароль!") ||
                message.equals("Пароль задан!") || message.endsWith("зарегистрировался в Чате!") ||
                message.startsWith("Данный Ник занят!")) {
            mainActivity.runOnUiThread(() -> serviceMessage(message));
            return;
        }
        if (message.startsWith("Новые сообщения")) {
            String newMessage = chatWork.checkNewName(message.split("\\s+", 4)[3]);
            mainActivity.runOnUiThread(() -> serviceMessage("Новые сообщения от "+ newMessage));
            return;
        }
        if (message.equals(" ")) {
            mainActivity.runOnUiThread(() -> serviceMessage(message));
            return;
        }
        if (message.endsWith("Пожалуйста, войдите в\nприложение заного!") || message.startsWith("Вы успешно сменили Ник на:")) {
            mainActivity.runOnUiThread(() -> serviceMessage(message));
            logoutAfterReg();
            return;
        } else {
            Message m = Message.fromJson(message);
            switch (m.command) {
                case CLIENT_LIST:
                    clientListMessage = m.clientListMessage;
                    if (clientListMessage.from.equals("Сервер")) {
                        chatWork.clientListOnline(clientListMessage);
                        break;
                    }
                    else mainActivity.runOnUiThread(this::clientList);
                    break;
                case CHANGE_NICK:
                    ChangeNick changeNick = m.changeNick;
                    nickname = changeNick.nick;
                    break;
                case WORK_WITH_GROUP:
                    workWithGroup = m.workWithGroup;
                    switch (workWithGroup.identify) {
                        case "0":
                            chatWork.deleteChatButton(workWithGroup.nameGroup);
                            break;
                        case "1":
                            chatWork.createChatButton(workWithGroup);
                            break;
                        case "2":
                            mainActivity.runOnUiThread(this::openFile);
                            break;
                        case "3":
                            chatWork.deletePass(workWithGroup);
                            break;
                        case "4":
                            mainActivity.runOnUiThread(this::addNewContact);
                            break;
                    }
                    break;
                case PUBLIC_MESSAGE:
                    PublicMessage publicMessage = m.publicMessage;
                    String newPublicVisualName = chatWork.checkNewName(publicMessage.from);
                    if (mainActivity.messageLayout.getVisibility() == View.VISIBLE && mainActivity.selectedButton.equals(publicMessage.from)) {
                        mainActivity.textArea.append(newPublicVisualName + " : " + publicMessage.message + System.lineSeparator());
                        chatHistory.outputPath("", publicMessage.from, mainActivity.fileHistory, newPublicVisualName, publicMessage.message);
                    }
                    else if (mainActivity.addNewContact("0 ", publicMessage.from) && publicMessage.from != null) {
                        mainActivity.runOnUiThread(mainActivity::loadAllContacts);
                        mainActivity.runOnUiThread(mainActivity::createBtn);
                        chatHistory.outputPath("", publicMessage.from, mainActivity.fileHistory, newPublicVisualName, publicMessage.message);
                    }
                    else {
                        chatHistory.outputPath("New", publicMessage.from, mainActivity.fileHistory, newPublicVisualName, publicMessage.message);
                    }
                    break;
                case GROUP_MESSAGE:
                    GroupMessage groupMessage = m.groupMessage;
                    String newGroupVisualName = chatWork.checkNewName(groupMessage.from);
                    if (mainActivity.messageLayout.getVisibility() == View.VISIBLE && mainActivity.selectedButton.equals(groupMessage.nameGroup)) {
                        mainActivity.textArea.append(newGroupVisualName + " : " + groupMessage.message + System.lineSeparator());
                        chatHistory.outputPath("", groupMessage.nameGroup, mainActivity.fileHistory, newGroupVisualName, groupMessage.message);
                    }
                    else if (mainActivity.addNewContact("1 ", groupMessage.nameGroup) && groupMessage.from != null && groupMessage.nameGroup != null) {
                        mainActivity.runOnUiThread(mainActivity::loadAllContacts);
                        mainActivity.runOnUiThread(mainActivity::createBtn);
                        chatHistory.outputPath("", groupMessage.nameGroup, mainActivity.fileHistory, newGroupVisualName, groupMessage.message);
                    }
                    else {
                        chatHistory.outputPath("New", groupMessage.nameGroup, mainActivity.fileHistory, newGroupVisualName, groupMessage.message);
                    }
                    break;
                case PRIVATE_MESSAGE:
                    PrivateMessage privateMessage = m.privateMessage;
                    String newVisualName = chatWork.checkNewName(privateMessage.from);
                    if (mainActivity.messageLayout.getVisibility() == View.VISIBLE && mainActivity.selectedNickname.equals(privateMessage.from)) {
                        mainActivity.textArea.append(newVisualName + " : " + privateMessage.message + System.lineSeparator());
                        chatHistory.outputPath("", privateMessage.from, mainActivity.fileHistory, newVisualName, privateMessage.message);
                    }
                    else if (mainActivity.addNewContact("0 ", privateMessage.from)) {
                        mainActivity.runOnUiThread(mainActivity::loadAllContacts);
                        mainActivity.runOnUiThread(mainActivity::createBtn);
                        chatHistory.outputPath("", privateMessage.from, mainActivity.fileHistory, newVisualName, privateMessage.message);
                    }
                    else {
                        chatHistory.outputPath("New", privateMessage.from, mainActivity.fileHistory, newVisualName, privateMessage.message);
                    }
                    break;
                case END:
                    break;
            }
        }
    }

    private void addNewContact() {
        mainActivity.addNewContact("1 ", workWithGroup.nameGroup);
    }

    void clientList(){
        mainActivity.clientList();
    }

    void auth() throws IOException {
        mainActivity.auth();
    }

    public void loadOffline() {
        mainActivity.loadButtonOffline();
    }

    public void addToChatWindow(String message) {
        if (network.socket != null) textArea.append("Я: " + message + System.lineSeparator());
    }

    void openFile() {
        mainActivity.openFile("1 ", workWithGroup.nameGroup);
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
            if (mainActivity.loginLayout.getVisibility() == View.VISIBLE) messageToService("Ошибка аутентификации!");
            else messageToService("Соединение с сервером установлено!");
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

    public void messageToService(String message) {
        mainActivity.runOnUiThread(() -> serviceMessage(message));
    }

    public void serviceMessage(String message) {
        Toast.makeText(mainActivity, message, Toast.LENGTH_LONG).show();
    }

    void close() throws IOException {
        if (needStopServerOnClosed) {
            sendMessage(STOP_SERVER_COMMAND);
        }
        if (network.socket != null) network.close();
        System.exit(0);
    }

    public boolean checkNetwork() {
        if (network.socket != null) return true;
        else {
            mainActivity.openFile("1 ", mainActivity.selectedButton);
            mainActivity.spinner.setVisibility(View.INVISIBLE);
            mainActivity.contact.setVisibility(View.VISIBLE);
            mainActivity.contact.setText(mainActivity.selectedButton);
            messageToService("Нет соединения с сервером!");
            return false;
        }
    }
}