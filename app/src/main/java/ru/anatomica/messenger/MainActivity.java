package ru.anatomica.messenger;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.io.*;
import java.util.concurrent.TimeUnit;

import ru.anatomica.messenger.gson.*;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
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
    public InputStream serverAddressProp;
    private MessageService messageService;
    public String fileHistory = "History.txt";
    protected String login = "Login.txt";
    protected String password = "Password.txt";
    protected String refreshToken;
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId  = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW));
        }
        if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(key);
                Log.d(TAG, "Key: " + key + " Value: " + value);
            }
        }

        refreshToken = FirebaseInstanceId.getInstance().getToken();

//        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(this, instanceIdResult -> {
//            refreshToken = instanceIdResult.getToken();
//            Log.e("refreshToken", refreshToken);
//        });
//        intent = new Intent(this, MainActivity.class);

        initialize();
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            fisLogin = openFileInput(login);
            TimeUnit.MILLISECONDS.sleep(500);
            if (fisLogin.available() != 0 && fisLogin != null) auth();
        } catch (InterruptedException | IOException e) {
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
        // super.onBackPressed();
        if (registerLayout.getVisibility() == View.VISIBLE) {
            messageService.changeToChoose();
            return;
        }
        if (loginLayout.getVisibility() == View.VISIBLE) {
            messageService.changeToChoose();
            return;
        }
        if (changeLayout.getVisibility() == View.VISIBLE) {
            System.exit(0);
        }
        else shutdown();
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
            register();
        }
        if (id == R.id.btn_auth) sendAuth();
        if (id == R.id.btn_send) sendMessage();
    }

    private void initialize() {
        try {
            fos = openFileOutput(fileHistory, Context.MODE_APPEND);
            fis = openFileInput(fileHistory);
            serverAddressProp = getAssets().open("application.properties");
            messageService = new MessageService(this, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage () {
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

    public void logout() {
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
        shutdown();
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
        msg.nickname = nicknameText;
        msg.login = loginText;
        msg.password = passwordText;
        Message regMsg = Message.createRegister(msg);
        messageService.sendMessage(regMsg.toJson());
    }

    public void sendAuth() {
//        messageService.startConnectionToServer();
//        TimeUnit.SECONDS.sleep(1);

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
        msg.token = refreshToken;
        Message authMsg = Message.createAuth(msg);
        messageService.sendMessage(authMsg.toJson());
    }

    public void auth() throws IOException {
//        messageService.startConnectionToServer();
//        TimeUnit.SECONDS.sleep(1);

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
        msg.token = refreshToken;
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
}
