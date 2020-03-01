package ru.anatomica.messenger;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

public class Network implements Closeable {

    private final MessageService messageService;
    private final String serverAddress;
    private final int port;
    private int countLoad = 0;

    protected Socket socket;
    private Thread readServerThread;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    Network(String serverAddress, int port, MessageService messageService) throws IOException {
        this.serverAddress = serverAddress;
        this.port = port;
        this.messageService = messageService;
        initNetworkState(serverAddress, port);
    }

    private void initNetworkState(String serverAddress, int port) throws IOException {
        Thread initConnection = new Thread(() -> {
            try {
                this.socket = new Socket(serverAddress, port);
                this.inputStream = new DataInputStream(socket.getInputStream());
                this.outputStream = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        initConnection.start();

        readServerThread = new Thread(this::readMessagesFromServer);
        readServerThread.setDaemon(true);
        readServerThread.start();
    }

    private void readMessagesFromServer() {

        outbroke:
        if (inputStream == null) {
            try {
            Thread offlineRead = new Thread(() -> {
                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (inputStream == null && countLoad == 0) {
                    countLoad++;
                    messageService.loadOffline();
                    messageService.messageToService("На данный момент сервер не доступен!");
                    readServerThread.interrupt();
                }
            });
            offlineRead.start();
            offlineRead.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (inputStream == null) break outbroke;
        }

        outbroke2:
        if (inputStream != null) {
            while (true) {
                try {
                    String message = inputStream.readUTF();
                    if (message != null) {
                        Thread readServer = new Thread(() -> {
                            try {
                                messageService.processRetrievedMessage(message);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                        readServer.setDaemon(true);
                        readServer.start();
                    }
                } catch (IOException e) {
                    System.out.println("Соединение с сервером было разорвано!");
                    break;
                }
                if (inputStream == null) break outbroke2;
            }
        }
    }

    void send (String message) {
        try {
            if (outputStream != null) outputStream.writeUTF(message);
            if (outputStream == null) {
                TimeUnit.MILLISECONDS.sleep(500);
                if (outputStream != null) outputStream.writeUTF(message);
                else {
                    Thread wait = new Thread(() -> {
                        try {
                            initNetworkState(serverAddress, port);
                            TimeUnit.MILLISECONDS.sleep(500);
                            if (outputStream != null) messageService.auth();
                            else {
                                if (!message.endsWith("\"CLIENT_LIST\"}")) messageService.messageToService("Нет соединения с сервером!");
                                readServerThread.interrupt();
                            }
                        } catch (InterruptedException | IOException e) {
                            e.printStackTrace();
                        }
                    });
                    wait.start();
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Ошибка отправки сообщения: " + message);
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}