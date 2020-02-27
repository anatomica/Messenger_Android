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

    Socket socket;
    private Thread readServerThread;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    Network(String serverAddress, int port, MessageService messageService) throws IOException {
        this.serverAddress = serverAddress;
        this.port = port;
        this.messageService = messageService;
        initNetworkState(serverAddress, port);
    }

    private void initNetworkState(String serverAddress, int port) {
        Thread thread = new Thread(() -> {
            try {
                this.socket = new Socket(serverAddress, port);
                this.inputStream = new DataInputStream(socket.getInputStream());
                this.outputStream = new DataOutputStream(socket.getOutputStream());
            } catch (SocketException | UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();

        readServerThread = new Thread(this::readMessagesFromServer);
        readServerThread.setDaemon(true);
        readServerThread.start();
    }

    private void readMessagesFromServer() {
        String message = null;
        while (true) {
            try {
                if (inputStream != null) message = inputStream.readUTF();
                if (socket != null && message != null){
                    String copyMessage = message;
                    Thread readServer = new Thread(() -> {
                        try {
                            messageService.processRetrievedMessage(copyMessage);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                    readServer.setDaemon(true);
                    readServer.start();
                }
                if (socket == null && countLoad == 0) {
                    countLoad++;
                    Thread offlineRead = new Thread(() -> {
                        try {
                            TimeUnit.SECONDS.sleep(3);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (socket == null) {
                            messageService.loadOffline();
                            messageService.messageToService("На данный момент сервер не доступен!");
                        }
                    });
                    // offlineRead.setDaemon(true);
                    offlineRead.start();
                }
            } catch (IOException e) {
                System.out.println("Соединение с сервером было разорвано!");
                break;
            }
        }
    }

    void send (String message) {
        try {
            if (socket == null) {
                initNetworkState(serverAddress, port);
                Thread wait = new Thread(() -> {
                    try {
                        TimeUnit.MILLISECONDS.sleep(1000);
                        if (socket != null) messageService.auth();
                        else messageService.messageToService("Нет соединения с сервером!");
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                    }
                });
                // wait.setDaemon(true);
                wait.start();
        }
            if (outputStream != null) outputStream.writeUTF(message);
        } catch (IOException e) {
            throw new RuntimeException("Failed to send message: " + message);
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}