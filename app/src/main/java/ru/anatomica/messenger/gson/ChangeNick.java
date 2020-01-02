package ru.anatomica.messenger.gson;
import com.google.gson.Gson;

public class ChangeNick {

    public String nick;

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static ChangeNick fromJson(String json) {
        return new Gson().fromJson(json, ChangeNick.class);
    }
}
