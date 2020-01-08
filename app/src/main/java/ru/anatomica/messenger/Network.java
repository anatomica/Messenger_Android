package ru.anatomica.messenger;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Network implements Closeable {

    private final MessageService messageService;
    private final String serverAddress;
    private final int port;

    private Socket socket;
    private Thread readServer;
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
                String copyMessage = message;
                readServer = new Thread(() -> {
                    try {
                        if (copyMessage != null) messageService.processRetrievedMessage(copyMessage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            readServer.setDaemon(true);
            readServer.start();
            } catch (IOException e) {
                System.out.println("Соединение с сервером было разорвано!");
                break;
            }
        }
    }

    void send (String message) {
        try {
            if (outputStream == null) {
                initNetworkState(serverAddress, port);
            }
            if (outputStream != null)
            outputStream.writeUTF(message);
        } catch (IOException e) {
            throw new RuntimeException("Failed to send message: " + message);
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}