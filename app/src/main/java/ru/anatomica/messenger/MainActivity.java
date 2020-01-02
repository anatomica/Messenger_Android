package ru.anatomica.messenger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import java.io.*;
import ru.anatomica.messenger.gson.*;

public class MainActivity extends AppCompatActivity {

    public ConstraintLayout changeLayout;
    public ConstraintLayout registerLayout;
    public ConstraintLayout loginLayout;
    public ConstraintLayout chatLayout;
    public Switch aSwitch;
    public Button exit;
    public Button sendAuth;
    public Button sendMessageButton;
    public Spinner spinner;
    public EditText textMessage;
    public EditText textArea;
    public EditText loginField;
    public EditText passField;
    public EditText nicknameReg;
    public EditText loginReg;
    public EditText passReg;
    public String nickName;
    public FileOutputStream fosClear;
    public FileOutputStream fos;
    public FileInputStream fis;
    public FileOutputStream fosLogin;
    public FileInputStream fisLogin;
    public FileOutputStream fosPasswd;
    public FileInputStream fisPasswd;
    public InputStream serverAddress;
    private MessageService messageService;
    public String fileHistory = "History.txt";
    protected String login = "Login.txt";
    protected String password = "Password.txt";
    protected Network network;
    protected Intent intent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        changeLayout = findViewById(R.id.activity_change);
        registerLayout = findViewById(R.id.activity_register);
        loginLayout = findViewById(R.id.activity_login);
        chatLayout = findViewById(R.id.activity_chat);

        textMessage = findViewById(R.id.textSend);
        textArea = findViewById(R.id.textView);
        spinner = findViewById(R.id.spinner);

        loginField = findViewById(R.id.login);
        passField = findViewById(R.id.password);

        nicknameReg = findViewById(R.id.nicknameReg);
        loginReg = findViewById(R.id.loginReg);
        passReg = findViewById(R.id.passwordReg);

        aSwitch= findViewById(R.id.switch1);
        exit = findViewById(R.id.btn_exit);
        sendAuth = findViewById(R.id.btn_auth);
        sendMessageButton = findViewById(R.id.btn_send);
        // intent = new Intent(this, MainActivity.class);
        initialize();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Thread sleep = new Thread();
        try {
            sleep.sleep(500);
            if (fisLogin.available() != 0) auth();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        shutdown();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        shutdown();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.btn_settings) return true;
        if (id == R.id.btn_clear) clearChat();
        if (id == R.id.btn_logout) logout();
        if (id == R.id.btn_exit) shutdown();
        return super.onOptionsItemSelected(item);
    }

    public void onClick(View view) {
        if (view == null) return;
        int id = view.getId();

        if (id == R.id.switch1) messageService.loginToChat();
        if (id == R.id.btn_register) messageService.changeToReg();
        if (id == R.id.btn_login) messageService.changeToLogin();

        if (id == R.id.btn_reg) {
            register();
            messageService.changeToLogin();
        }
        if (id == R.id.btn_auth) sendAuth();
        if (id == R.id.btn_send) sendText();
    }

    private void initialize() {
        try {
            fos = openFileOutput(fileHistory, Context.MODE_APPEND);
            fis = openFileInput(fileHistory);
            serverAddress = getAssets().open("application.properties");
            messageService = new MessageService(this, true);
            fisLogin = openFileInput(login);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage () {
        sendMessageAction();
    }

    private void sendText () {
        sendMessageAction();
    }

    private void sendMessageAction() {
        String message = textMessage.getText().toString();
        Message msg = buildMessage(message);
        messageService.sendMessage(msg.toJson());
        textMessage.setText("");
        // selectedNickname = clientList.getSelectionModel().getSelectedItem();
        textArea.append("Я: " + prepareToView(message) + System.lineSeparator());
    }

    private Message buildMessage(String message) {
        // selectedNickname = clientList.getSelectionModel().getSelectedItem();
        return buildPublicMessage(message);
    }

    private Message buildPublicMessage(String message) {
        PublicMessage publicMsg = new PublicMessage();
        publicMsg.from = nickName;
        publicMsg.message = message;
        Message msg = new Message();
        msg.command = Command.PUBLIC_MESSAGE;
        msg.publicMessage = publicMsg;
        return msg;
    }

    private String prepareToView(String message) {
        return message.replaceAll("/w\\s+", "[private]: ");
    }

    private void logout() {
        String log = "";
        String pass = "";
        try {
            fosLogin = openFileOutput(login, Context.MODE_PRIVATE);
            fosPasswd = openFileOutput(password, Context.MODE_PRIVATE);
            fosLogin.write(log.getBytes());
            fosPasswd.write(pass.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        try {
            messageService.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void register() {
        String loginText = loginReg.getText().toString();
        String passwordText = passReg.getText().toString();
        String nicknameText = nicknameReg.getText().toString();
        RegisterMessage msg = new RegisterMessage();
        msg.nickname = loginText;
        msg.login = nicknameText;
        msg.password = passwordText;
        Message regMsg = Message.createRegister(msg);
        messageService.sendMessage(regMsg.toJson());
    }

    public void sendAuth() {
        String loginText = loginField.getText().toString();
        String passwordText = passField.getText().toString();
        try {
            fosLogin = openFileOutput(login, Context.MODE_PRIVATE);
            fosPasswd = openFileOutput(password, Context.MODE_PRIVATE);
            fosLogin.write(loginText.getBytes());
            fosPasswd.write(passwordText.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        AuthMessage msg = new AuthMessage();
        msg.login = loginText;
        msg.password = passwordText;
        Message authMsg = Message.createAuth(msg);
        messageService.sendMessage(authMsg.toJson());
    }

    public void auth() throws IOException {
        fisLogin = openFileInput(login);
        fisPasswd = openFileInput(password);

        int available = fisLogin.available();
        byte[] buffer = new byte[available];
        fisLogin.read(buffer);
        String loginText = new String(buffer);
        fisLogin.close();

        int available1 = fisPasswd.available();
        byte[] buffer1 = new byte[available1];
        fisPasswd.read(buffer1);
        String passwdText = new String(buffer);
        fisPasswd.close();

        AuthMessage msg = new AuthMessage();
        msg.login = loginText;
        msg.password = passwdText;
        Message authMsg = Message.createAuth(msg);
        messageService.sendMessage(authMsg.toJson());
    }

    private void clearChat() {
        String str = "";
        textArea.setText(str);
        try {
            fosClear = openFileOutput(fileHistory, Context.MODE_PRIVATE);
            fosClear.write(str.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void authError() {
        Toast.makeText(MainActivity.this, R.string.msgAuth, Toast.LENGTH_LONG).show();
    }

    public void wrongPass() {
        Toast.makeText(MainActivity.this, R.string.msgPass, Toast.LENGTH_LONG).show();
    }
}
