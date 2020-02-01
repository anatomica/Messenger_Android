package ru.anatomica.messenger.gson;
import com.google.gson.Gson;

public class PrivateMessage {

    public String from;
    public String to;
    public String message;

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static PrivateMessage fromJson(String json) {
        return new Gson().fromJson(json, PrivateMessage.class);
    }

}
