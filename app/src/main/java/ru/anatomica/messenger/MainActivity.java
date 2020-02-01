package ru.anatomica.messenger;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.google.firebase.iid.FirebaseInstanceId;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import ru.anatomica.messenger.gson.*;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public LinearLayout buttonLayout;
    public CoordinatorLayout mainLayout;
    public ConstraintLayout changeLayout;
    public ConstraintLayout registerLayout;
    public ConstraintLayout loginLayout;
    public ConstraintLayout chatLayout;
    public ConstraintLayout chatLayoutInto;
    public ConstraintLayout messageLayout;

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

    protected ArrayList<Button> buttons = new ArrayList<>();
    protected ArrayList<String> buttonName = new ArrayList<>();
    public InputStream serverAddressProp;

    public String fileHistory = "History.txt";
    protected String login = "Login.txt";
    protected String password = "Password.txt";
    protected String refreshToken;
    protected Intent intent;
    protected String selectedNickname;
    protected String selectedButton;

    private MessageService messageService;
    private ChatHistory chatHistory;
    private ChatWork chatWork;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mainLayout = findViewById(R.id.activity_main);
        changeLayout = findViewById(R.id.activity_change);
        registerLayout = findViewById(R.id.activity_register);
        loginLayout = findViewById(R.id.activity_login);
        chatLayout = findViewById(R.id.activity_chat);
        chatLayoutInto = findViewById(R.id.activity_chat_layout);
        messageLayout = findViewById(R.id.activity_message);
        buttonLayout = findViewById(R.id.ButtonContainer);

        textMessage = findViewById(R.id.textSend);
        textArea = findViewById(R.id.textView);

        loginField = findViewById(R.id.login);
        passField = findViewById(R.id.password);

        nicknameReg = findViewById(R.id.nicknameReg);
        loginReg = findViewById(R.id.loginReg);
        passReg = findViewById(R.id.passwordReg);

        exit = findViewById(R.id.btn_exit);
        sendAuth = findViewById(R.id.btn_auth);
        sendMessageButton = findViewById(R.id.btn_send);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = getString(R.string.default_notification_channel_id);
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

        spinner = findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                Object item = parent.getItemAtPosition(pos);
                selectedNickname = spinner.getSelectedItem().toString();
                if (!selectedNickname.equals("Пользователи группы:")) {
                    selectedButton = selectedNickname;
                    openFile(selectedNickname);
                }
            }
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

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
        // shutdown();
    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed();
        if (messageLayout.getVisibility() == View.VISIBLE) {
            messageService.loginToChat();
            return;
        }
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
        } else shutdown();
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
        if (id == R.id.btn_create_group) chatWork.createGroup();
        if (id == R.id.btn_delete_group) chatWork.deleteGroup();
        if (id == R.id.btn_clear_nick) clearSavedNick();
        if (id == R.id.btn_clear_all) clearAllChat();
        if (id == R.id.btn_clear) clearChat();
        if (id == R.id.btn_logout) logout();
        if (id == R.id.btn_exit) shutdown();
        return super.onOptionsItemSelected(item);
    }

    public void onClick(View view) {
        if (view == null) return;
        int id = view.getId();

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
            serverAddressProp = getAssets().open("application.properties");
            messageService = new MessageService(this, true);
            chatHistory = new ChatHistory(this);
            chatWork = new ChatWork(this, messageService);
        } catch (Exception e) {
            e.printStackTrace();
        }
        openFile("");
    }

    private void openFile(String nameFile) {
        try {
            fos = openFileOutput(nameFile + fileHistory, Context.MODE_APPEND);
            fis = openFileInput(nameFile + fileHistory);
            if (fis.available() >= 0 && !nameFile.equals("")) {
                if (nameFile.equals("Семья") || nameFile.equals("Работа") || nameFile.equals("GeekBrains")) {
                    chatHistory.loadChatHistory(nameFile + fileHistory);
                    messageService.loginToMessage();
                } else {
                    chatHistory.loadChatHistory(nameFile + fileHistory);
                    messageService.loginToMessage();
                }
            }
            if (!nameFile.equals("") && !nameFile.equals("Семья") &&
                    !nameFile.equals("Работа") && !nameFile.equals("GeekBrains")) {
                if (addNewContact(nameFile)) {
                    loadAllContacts();
                    createBtn();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean addNewContact(String nameFile) {
        if (nameFile.equals("Семья") || nameFile.equals("Работа") ||
                nameFile.equals("GeekBrains")) return false;
        try {
            fos = openFileOutput("AllContacts.txt", Context.MODE_APPEND);
            fis = openFileInput("AllContacts.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            String tmp;
            while ((tmp = br.readLine()) != null) {
                if (tmp.equals(nameFile)) return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        chatHistory.writeChatHistory(nameFile);
        return true;
    }

    public void loadAllContacts() {
        buttonName.clear();
        buttonName.add("Семья");
        buttonName.add("Работа");
        buttonName.add("GeekBrains");

        try {
            fis = openFileInput("AllContacts.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            String tmp;
            while ((tmp = br.readLine()) != null) {
                buttonName.add(tmp);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createBtn() {
        buttons.clear();
        for (int i = 0; i < buttonName.size(); i++) {
            buttons.add(new Button(this));
            buttons.get(i).setText(String.valueOf(i));
        }
        viewButton();
    }

    public void viewButton() {
        LinearLayout.MarginLayoutParams params = new LinearLayout.MarginLayoutParams(
                LinearLayout.MarginLayoutParams.MATCH_PARENT,
                LinearLayout.MarginLayoutParams.WRAP_CONTENT);

        int btnMargin = 10;
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setId(i + 1);
            final int id_ = buttons.get(i).getId();
            buttons.get(i).setText(buttonName.get(i));
            buttons.get(i).getBackground().setAlpha(100);
            params.height = 270;
            buttons.get(i).setLayoutParams(params);
            buttons.get(i).setY(btnMargin);
            btnMargin = btnMargin + 270;
            chatLayoutInto.addView(buttons.get(i), params);

            int finalI = i;
            buttons.get(i).setOnClickListener(view -> {
                selectedButton = buttons.get(finalI).getText().toString();
                selectedNickname = buttons.get(finalI).getText().toString();
                openFile(selectedButton);
                requestClientsList(selectedButton);
                openDialogPass();
//                    Toast.makeText(view.getContext(),"Button index = " + id_ + "\nName: " +
//                            buttons.get(finalI).getText().toString(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void openDialogPass() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("Группа ...");
        alertDialog.setMessage("Введите пароль:");

        final EditText input = new EditText(MainActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alertDialog.setView(input);
        // alertDialog.setIcon(R.drawable.key);

        alertDialog.setPositiveButton("Отправить!", (dialog, which) -> {
                    password = input.getText().toString();
                    if (password.compareTo("") == 0) {
                        if ("".equals(password)) {
                            Toast.makeText(getApplicationContext(),"Password Matched", Toast.LENGTH_SHORT).show();
//                            Intent myIntent = new Intent(chatLayout.getContext(), MainActivity.class);
//                            startActivityForResult(myIntent, 0);
                        } else {
                            Toast.makeText(getApplicationContext(),"Wrong Password!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        alertDialog.setNegativeButton("Отменить!", (dialog, which) -> dialog.cancel());
        alertDialog.show();
    }

    private void sendMessage () {
        sendMessageAction();
    }

    private void requestClientsList(String nameButton) {
        if (nameButton.equals("Семья") || nameButton.equals("Работа") || nameButton.equals("GeekBrains")) {
            List<String> nameGroup = new ArrayList<>();
            nameGroup.add(nameButton);
            Message msg = Message.createClientList(nameGroup, nickName);
            messageService.sendMessage(msg.toJson());
        } else {

        }
    }

    private void sendMessageAction() {
        String message = textMessage.getText().toString();
        Message msg = buildMessage(message);
        messageService.sendMessage(msg.toJson());
        textMessage.setText("");
        textArea.append("Я: " + prepareToView(message) + System.lineSeparator());
    }

    private Message buildMessage(String message) {
        if (selectedButton.equals("Семья") || selectedButton.equals("Работа") || selectedButton.equals("GeekBrains")) {
            return buildGroupMessage(message);
        }
        if (!selectedNickname.equals("Пользователи группы:")) {
            PrivateMessage msg = new PrivateMessage();
            msg.from = nickName;
            msg.to = selectedNickname;
            msg.message = message;
            return Message.createPrivate(msg);
        }
        return buildGroupMessage(message);
    }

    private Message buildGroupMessage(String message) {
        GroupMessage msg = new GroupMessage();
        msg.from = nickName;
        msg.message = message;
        msg.nameGroup = selectedButton;
        return Message.createGroup(msg);
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
        byte[] bufLogin = new byte[available];
        fisLogin.read(bufLogin);
        String loginText = new String(bufLogin);
        fisLogin.close();

        int available1 = fisPasswd.available();
        byte[] bufPass = new byte[available1];
        fisPasswd.read(bufPass);
        String passwdText = new String(bufPass);
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

    private void clearAllChat() {
        String str = "";
        textArea.setText(str);
        try {
            fosClear = openFileOutput(fileHistory, Context.MODE_PRIVATE);
            fosClear.write(str.getBytes());
            fosClear = openFileOutput("Макс" + fileHistory, Context.MODE_PRIVATE);
            fosClear.write(str.getBytes());
            fosClear = openFileOutput("Оля" + fileHistory, Context.MODE_PRIVATE);
            fosClear.write(str.getBytes());
            fosClear = openFileOutput("Ника" + fileHistory, Context.MODE_PRIVATE);
            fosClear.write(str.getBytes());
            fosClear = openFileOutput("Олег" + fileHistory, Context.MODE_PRIVATE);
            fosClear.write(str.getBytes());
            fosClear = openFileOutput("Макс:" + fileHistory, Context.MODE_PRIVATE);
            fosClear.write(str.getBytes());
            fosClear = openFileOutput("Оля:" + fileHistory, Context.MODE_PRIVATE);
            fosClear.write(str.getBytes());
            fosClear = openFileOutput("Ника:" + fileHistory, Context.MODE_PRIVATE);
            fosClear.write(str.getBytes());
            fosClear = openFileOutput("Семья" + fileHistory, Context.MODE_PRIVATE);
            fosClear.write(str.getBytes());
            fosClear = openFileOutput("Работа" + fileHistory, Context.MODE_PRIVATE);
            fosClear.write(str.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearSavedNick() {
        String str = "";
        textArea.setText(str);
        try {
            fosClear = openFileOutput("AllContacts.txt", Context.MODE_PRIVATE);
            fosClear.write(str.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clientList() {
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, messageService.clientListMessage.online);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);
    }
}
