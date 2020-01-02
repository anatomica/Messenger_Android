package ru.anatomica.messenger;

class ServerConnectionException extends RuntimeException {

    ServerConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
