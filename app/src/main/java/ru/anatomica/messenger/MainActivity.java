package ru.anatomica.messenger;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import ru.anatomica.messenger.gson.AuthMessage;
import ru.anatomica.messenger.gson.ChangeNick;
import ru.anatomica.messenger.gson.GroupMessage;
import ru.anatomica.messenger.gson.Message;
import ru.anatomica.messenger.gson.PrivateMessage;
import ru.anatomica.messenger.gson.RegisterMessage;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;

    public CoordinatorLayout mainLayout;
    public ConstraintLayout changeLayout;
    public ConstraintLayout registerLayout;
    public ConstraintLayout loginLayout;
    public ConstraintLayout chatLayout;
    public ConstraintLayout chatLayoutInto;
    public ConstraintLayout messageLayout;
    public TextView contact;

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
    protected String selectedNickname;
    protected String selectedButton;
    protected String identifyButton;
    protected String loginTextOnLogin;
    protected String passwordTextOnLogin;

    protected AlertDialog.Builder alertDialog;
    private MessageService messageService;
    private ChatHistory chatHistory;
    private ChatWork chatWork;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy threadPolicy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.VmPolicy vmPolicy = new StrictMode.VmPolicy.Builder().build();
        StrictMode.setThreadPolicy(threadPolicy);
        StrictMode.setVmPolicy(vmPolicy);
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
        contact = findViewById(R.id.contact);

        textMessage = findViewById(R.id.textSend);
        textArea = findViewById(R.id.textView);

        loginField = findViewById(R.id.login);
        loginField.setTextColor(Color.WHITE);
        passField = findViewById(R.id.password);
        passField.setTextColor(Color.WHITE);

        nicknameReg = findViewById(R.id.nicknameReg);
        nicknameReg.setTextColor(Color.WHITE);
        loginReg = findViewById(R.id.loginReg);
        loginReg.setTextColor(Color.WHITE);
        passReg = findViewById(R.id.passwordReg);
        passReg.setTextColor(Color.WHITE);

        exit = findViewById(R.id.btn_exit);
        sendAuth = findViewById(R.id.btn_auth);
        sendMessageButton = findViewById(R.id.btn_send);
        sendMessageButton.setTextColor(Color.WHITE);
        sendMessageButton.setBackgroundColor(Color.DKGRAY);
        sendMessageButton.getBackground().setAlpha(200);

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

        checkNotificationPermission();

        refreshToken = getFCMToken(login, password);

        spinner = findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                selectedNickname = spinner.getSelectedItem().toString();
                if (!selectedNickname.equals("Пользователи группы:") && !selectedNickname.startsWith("Вы (")) {
                    selectedButton = selectedNickname;
                    identifyButton = "0";
                    openFile("0 ", selectedNickname);
                    setNameOnChat();
                    requestClientOnline(selectedNickname);
                }
                if (selectedNickname.startsWith("Вы (")) {
                    messageService.serviceMessage("Это Вы, " + nickName);
                    clientList();
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
        auth();
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
            return;
        }
        if (chatLayout.getVisibility() == View.VISIBLE) {
            shutdown();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem createGroup = menu.findItem(R.id.btn_create_group);
        MenuItem joinGroup = menu.findItem(R.id.btn_join_group);
        MenuItem leaveGroup = menu.findItem(R.id.btn_delete_group);
        MenuItem deleteNicks = menu.findItem(R.id.btn_clear_nicks);
        MenuItem restoreContacts = menu.findItem(R.id.btn_restore_contacts);
        MenuItem clearALLChats = menu.findItem(R.id.btn_clear_all);
        MenuItem clearChat = menu.findItem(R.id.btn_clear);
        MenuItem changeNick = menu.findItem(R.id.btn_changeNick);
        MenuItem logout = menu.findItem(R.id.btn_logout);

        if (changeLayout.getVisibility() == View.VISIBLE || registerLayout.getVisibility() == View.VISIBLE ||
                loginLayout.getVisibility() == View.VISIBLE) {
            createGroup.setVisible(false);
            joinGroup.setVisible(false);
            leaveGroup.setVisible(false);
            deleteNicks.setVisible(false);
            restoreContacts.setVisible(false);
            clearALLChats.setVisible(false);
            clearChat.setVisible(false);
            changeNick.setVisible(false);
            logout.setVisible(false);
        }
        if (chatLayout.getVisibility() == View.VISIBLE) {
            createGroup.setVisible(true);
            joinGroup.setVisible(true);
            leaveGroup.setVisible(true);
            deleteNicks.setVisible(true);
            restoreContacts.setVisible(true);
            clearALLChats.setVisible(false);
            clearChat.setVisible(false);
            changeNick.setVisible(true);
            logout.setVisible(true);
        }
        if (messageLayout.getVisibility() == View.VISIBLE) {
            createGroup.setVisible(false);
            joinGroup.setVisible(false);
            leaveGroup.setVisible(false);
            deleteNicks.setVisible(false);
            restoreContacts.setVisible(false);
            clearALLChats.setVisible(true);
            clearChat.setVisible(true);
            changeNick.setVisible(false);
            logout.setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.btn_about) {
            about();
        } else if (id == R.id.btn_create_group) {
            chatWork.createGroup();
        } else if (id == R.id.btn_join_group) {
            chatWork.joinToGroup();
        } else if (id == R.id.btn_delete_group) {
            chatWork.deleteGroup();
        } else if (id == R.id.btn_clear_nicks) {
            clearSavedNick();
        } else if (id == R.id.btn_restore_contacts) {
            chatWork.restoreContacts();
        } else if (id == R.id.btn_clear_all) {
            clearAllChat();
        } else if (id == R.id.btn_clear) {
            clearChat();
        } else if (id == R.id.btn_changeNick) {
            changeNick();
        } else if (id == R.id.btn_logout) {
            logout();
        } else if (id == R.id.btn_exit) {
            shutdown();
        }
        return super.onOptionsItemSelected(item);
    }

    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_register) {
            messageService.changeToReg();
        } else if (id == R.id.btn_login) {
            messageService.changeToLogin();
        } else if (id == R.id.btn_reg) {
            register();
            register();
        } else if (id == R.id.btn_auth) {
            sendAuth();
        } else if (id == R.id.btn_send) {
            sendMessage();
        }
    }

    private void initialize() {
        try {
            serverAddressProp = getAssets().open("application.properties");
            messageService = new MessageService(this, true);
            chatHistory = new ChatHistory(this);
            chatWork = new ChatWork(this, messageService);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void openFile(String identifyButOnCreate, String nameFile) {
        try {
            fos = openFileOutput(nameFile + fileHistory, Context.MODE_APPEND);
            fis = openFileInput(nameFile + fileHistory);
            if (fis.available() >= 0 && !nameFile.isEmpty()) {
                chatHistory.loadChatHistory(nameFile + fileHistory);
                messageService.loginToMessage();
            }
            if (addNewContact(identifyButOnCreate, nameFile)) {
                loadAllContacts();
                createBtn();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public boolean addNewContact(String identifyButOnCreate, String nameFile) {
        try {
            fos = openFileOutput("AllContacts.txt", Context.MODE_APPEND);
            fis = openFileInput("AllContacts.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            String tmp;
            while ((tmp = br.readLine()) != null) {
                if (tmp.equals(identifyButOnCreate + nameFile)) return false;
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        if (!identifyButOnCreate.isEmpty() && !nameFile.isEmpty())
            chatHistory.writeChatHistory(identifyButOnCreate + nameFile);
        return true;
    }

    public void loadAllContacts() {
        buttonName.clear();
        try {
            fos = openFileOutput("AllContacts.txt", Context.MODE_APPEND);
            fis = openFileInput("AllContacts.txt");
            if (fis.available() == 0) {
                chatHistory.writeChatHistory("1 " + "Family");
                chatHistory.writeChatHistory("1 " + "Hospital 68");
                chatHistory.writeChatHistory("1 " + "GeekBrains");
                chatHistory.writeChatHistory("1 " + "Общий Чат");
                chatWork.savePass("Общий Чат", "0");
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            String tmp;
            while ((tmp = br.readLine()) != null) {
                buttonName.add(tmp);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void createBtn() {
        removeButton();
        for (int i = 0; i < buttonName.size(); i++) {
            buttons.add(new Button(this));
        }
        viewButton();
    }

    public void removeButton() {
        if (!buttons.isEmpty())
            for (int i = 0; i < buttons.size(); i++) {
                ((ViewManager) buttons.get(i).getParent()).removeView(buttons.get(i));
            }
        buttons.clear();
    }

    public void viewButton() {
        String newVisualName;
        LinearLayout.MarginLayoutParams params = new LinearLayout.MarginLayoutParams(
                LinearLayout.MarginLayoutParams.MATCH_PARENT,
                LinearLayout.MarginLayoutParams.WRAP_CONTENT);

        int btnMargin = 10;
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setId(i);
            final int id_ = buttons.get(i).getId();
            String nameButton = buttonName.get(i).split("\\s+", 2)[1];
            newVisualName = chatWork.checkNewName(nameButton);
            if (newVisualName.equals(nameButton)) buttons.get(i).setText(nameButton);
            else buttons.get(i).setText(newVisualName);
            buttons.get(i).getBackground().setAlpha(100);
            buttons.get(i).setTextColor(Color.WHITE);
            params.height = 270;
            buttons.get(i).setLayoutParams(params);
            buttons.get(i).setY(btnMargin);
            btnMargin = btnMargin + 270;
            chatLayoutInto.addView(buttons.get(i), params);

            int finalI = i;
            buttons.get(i).setOnClickListener(v -> {
                String textButton = buttonName.get(finalI);
                identifyButton = textButton.split("\\s+")[0];
                selectedButton = textButton.split("\\s+", 2)[1];
                selectedNickname = textButton.split("\\s+", 2)[1];
                if (identifyButton.equals("1")) {
                    if (messageService.checkNetwork(identifyButton)) {
                        if (!chatWork.autoLoginToGroup(selectedButton)) {
                            openDialogPass();
                            contact.setVisibility(View.INVISIBLE);
                            spinner.setVisibility(View.VISIBLE);
                            requestClientsList(selectedButton);
                        }
                    }
                }
                if (identifyButton.equals("0")) {
                    if (messageService.checkNetwork(identifyButton)) {
                        openFile(identifyButton + " ", selectedButton);
                        requestClientOnline(selectedButton);
                        setNameOnChat();
                    }
                }
            });
            buttons.get(i).setOnLongClickListener(v -> {
                identifyButton = buttonName.get(finalI).split("\\s+")[0];
                selectedButton = buttonName.get(finalI).split("\\s+", 2)[1];
                menuContact();
                return true;
            });
        }
    }

    public void setNameOnChat() {
        String newVisualName = chatWork.checkNewName(selectedButton);
        if (newVisualName.equals(selectedButton)) contact.setText(selectedButton);
        else contact.setText(newVisualName);
        spinner.setVisibility(View.INVISIBLE);
        contact.setVisibility(View.VISIBLE);
    }

    private void menuContact() {
        AtomicInteger number = new AtomicInteger();
        String[] choose = {"Переименовать контакт ?", "Удалить данный чат ?"};
        if (identifyButton.equals("0"))
            choose[1] = String.format("Удалить чат с '%s' ?", selectedButton);
        if (identifyButton.equals("1"))
            choose[1] = String.format("Выйти из группы '%s' ?", selectedButton);
        alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Выбор действия:");
        alertDialog.setSingleChoiceItems(choose, -1, (dialog, item) -> {
            if (choose[item].equals("Переименовать контакт ?")) number.set(0);
            if (choose[item].equals(String.format("Удалить чат с '%s' ?", selectedButton)) ||
                    choose[item].equals(String.format("Выйти из группы '%s' ?", selectedButton)))
                number.set(1);
        });
        alertDialog.setPositiveButton("OK", (dialog, item) -> {
            if (number.get() == 0 && identifyButton.equals("0"))
                chatWork.makeNewName(selectedButton);
            if (number.get() == 0 && identifyButton.equals("1"))
                messageService.serviceMessage("Вы не можете сменить имя группового чата!");
            if (number.get() == 1) chatWork.deleteGroupChat(selectedButton);
        });
        alertDialog.setNegativeButton("Отмена", (dialog, item) ->
                Toast.makeText(getApplicationContext(), "Отмена действия ...", Toast.LENGTH_SHORT).show());
        alertDialog.setCancelable(true);
        alertDialog.show();
    }

    private void openDialogPass() {
        alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("Группа " + selectedButton);
        alertDialog.setMessage("Введите пароль:");

        final EditText input = new EditText(MainActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alertDialog.setView(input);
        // alertDialog.setIcon(R.drawable.key);
        alertDialog.setPositiveButton("Верно!", (dialog, which) -> {
            String passGroup = input.getText().toString();
            if (passGroup.compareTo("") > 0) {
                chatWork.checkPassGroup(selectedButton, passGroup);
                chatWork.savePass(selectedButton, passGroup);
            }
        });
        alertDialog.setNegativeButton("Отменить!", (dialog, which) -> dialog.cancel());
        alertDialog.show();
    }

    private void sendMessage() {
        sendMessageAction();
    }

    public void requestClientsList(String nameButton) {
        List<String> nameGroup = new ArrayList<>();
        nameGroup.add(nameButton);
        Message msg = Message.createClientList(nameGroup, nickName);
        messageService.sendMessage(msg.toJson());
    }

    public void requestClientOnline(String nameButton) {
        List<String> nameClient = new ArrayList<>();
        nameClient.add(nameButton);
        Message msg = Message.createClientList(nameClient, "Request");
        messageService.sendMessage(msg.toJson());
    }

    private void sendMessageAction() {
        String message = textMessage.getText().toString();
        Message msg = buildMessage(message);
        messageService.sendMessage(msg.toJson());
        textMessage.setText("");
        messageService.addToChatWindow(message);
    }

    private Message buildMessage(String message) {
        if (identifyButton.equals("1")) {
            return buildGroupMessage(message);
        }
        if (identifyButton.equals("0")) {
            PrivateMessage msg = new PrivateMessage();
            msg.from = nickName;
            msg.to = selectedButton;
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

    private Message buildChangeNick(String newNick) {
        ChangeNick msg = new ChangeNick();
        msg.nick = newNick;
        return Message.createNick(msg);
    }

    public void logout() {
        String log = "";
        String pass = "";
        try {
            fosLogin = openFileOutput(login, Context.MODE_PRIVATE);
            fosPasswd = openFileOutput(password, Context.MODE_PRIVATE);
            fosLogin.write(log.getBytes());
            fosPasswd.write(pass.getBytes());
            fosLogin.close();
            fosPasswd.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        shutdown();
    }

    public void shutdown() {
        try {
            messageService.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void register() {
        String loginText = loginReg.getText().toString();
        String passwordText = passReg.getText().toString();
        String nicknameText = nicknameReg.getText().toString();
        if (nicknameText.isEmpty() || passwordText.isEmpty() ||
                passwordText.equals(" ") || loginText.isEmpty() || loginText.equals(" ")) {
            messageService.serviceMessage("Никнейм / Логин / Пароль не могут быть пустыми!");
            return;
        }
        if (nicknameText.split("\\s+").length > 1 || nicknameText.equals(" ") ||
                nicknameText.startsWith(" ") || nicknameText.endsWith(" ")) {
            messageService.serviceMessage("Никнейм должен быть без пробелов!");
            return;
        }
        RegisterMessage msg = new RegisterMessage();
        msg.nickname = nicknameText;
        msg.login = loginText;
        msg.password = passwordText;
        Message regMsg = Message.createRegister(msg);
        messageService.sendMessage(regMsg.toJson());
    }

    public void sendAuth() {
        loginTextOnLogin = loginField.getText().toString();
        passwordTextOnLogin = passField.getText().toString();

        AuthMessage msg = new AuthMessage();
        msg.login = loginTextOnLogin;
        msg.password = passwordTextOnLogin;
        msg.token = refreshToken;
        Message authMsg = Message.createAuth(msg);
        messageService.sendMessage(authMsg.toJson());
    }

    public void saveLoginPass() {
        try {
            fosLogin = openFileOutput(login, Context.MODE_APPEND);
            fosPasswd = openFileOutput(password, Context.MODE_APPEND);
            fisLogin = openFileInput(login);
            if (fisLogin.available() == 0 && loginTextOnLogin != null && !loginTextOnLogin.isEmpty()) {
                fosLogin = openFileOutput(login, Context.MODE_PRIVATE);
                fosPasswd = openFileOutput(password, Context.MODE_PRIVATE);
                fosLogin.write(loginTextOnLogin.getBytes());
                fosPasswd.write(passwordTextOnLogin.getBytes());
            }
            fosLogin.close();
            fosPasswd.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    protected void loadButtonOffline() {
        try {
            fosLogin = openFileOutput(login, Context.MODE_APPEND);
            fisLogin = openFileInput(login);
            if (fisLogin.available() != 0 && fisLogin != null) {
                runOnUiThread(messageService::loginToChat);
                runOnUiThread(this::loadAllContacts);
                runOnUiThread(this::createBtn);
            }
            fosLogin.close();
            fisLogin.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void auth() {
        try {
            fosLogin = openFileOutput(login, Context.MODE_APPEND);
            fosPasswd = openFileOutput(password, Context.MODE_APPEND);
            fisLogin = openFileInput(login);
            fisPasswd = openFileInput(password);

            if (fisLogin.available() != 0 && fisLogin != null) {
                findViewById(R.id.btn_register).setVisibility(View.INVISIBLE);
                findViewById(R.id.btn_login).setVisibility(View.INVISIBLE);
                // TimeUnit.MILLISECONDS.sleep(700);

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
            fosLogin.close();
            fosPasswd.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void clearChat() {
        String str = "";
        textArea.setText(str);
        try {
            fosClear = openFileOutput(selectedButton + fileHistory, Context.MODE_PRIVATE);
            fosClear.write(str.getBytes());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void clearAllChat() {
        String str = "";
        textArea.setText(str);
        try {
            fosClear = openFileOutput(fileHistory, Context.MODE_PRIVATE);
            fosClear.write(str.getBytes());
            fos = openFileOutput("AllContacts.txt", Context.MODE_APPEND);
            fis = openFileInput("AllContacts.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            String tmp;
            while ((tmp = br.readLine()) != null) {
                String name = tmp.split("\\s+", 2)[1];
                fos = openFileOutput(name + fileHistory, Context.MODE_PRIVATE);
                fos.write(str.getBytes());
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void clearSavedNick() {
        String str = "";
        textArea.setText(str);
        try {
            fosClear = openFileOutput("AllContacts.txt", Context.MODE_PRIVATE);
            fosClear.write(str.getBytes());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        loadAllContacts();
        createBtn();
    }

    private void changeNick() {
        alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("Смена никнейма");
        alertDialog.setMessage("Введите новый ник:");

        final EditText input = new EditText(MainActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alertDialog.setView(input);
        alertDialog.setPositiveButton("Сменить ник!", (dialog, which) -> {
            String newNick = input.getText().toString();
            if (newNick.split("\\s+").length > 1 || newNick.equals(" ") ||
                    newNick.startsWith(" ") || newNick.endsWith(" ")) {
                messageService.serviceMessage("Новый никнейм должен быть без пробелов!");
                return;
            } else {
                Message msg = buildChangeNick(newNick);
                messageService.sendMessage(msg.toJson());
            }
        });
        alertDialog.setNegativeButton("Отменить!", (dialog, which) -> dialog.cancel());
        alertDialog.show();
    }

    private void about() {
        alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("О приложении:");
        alertDialog.setMessage("Версия 5.0 \nMaxim Fomin © 2019 - 2025 \nВсе права защищены.");
        alertDialog.setIcon(R.drawable.ic_stat_notification);
        alertDialog.setPositiveButton("ОК", (dialog, which) -> {
            dialog.cancel();
        });
        alertDialog.show();
    }

    public void clientList() {
        messageService.clientListMessage.online.remove(nickName);
        messageService.clientListMessage.online.remove(String.format("Вы (%s)", nickName));
        messageService.clientListMessage.online.add(String.format("Вы (%s)", nickName));
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, messageService.clientListMessage.online);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);
    }

    private String getFCMToken(String login, String password) {
        AtomicReference<String> token = new AtomicReference<>("");

        try {
            fis = openFileInput(login);
            if (fis.available() >= 0 && !login.isEmpty()) {
                Thread thread = new Thread(() -> {
                    try {
                        String result = Tasks.await(FirebaseMessaging.getInstance().getToken(), 10, TimeUnit.SECONDS);
                        token.set(result != null ? result : "");
                    } catch (Exception e) {
                        Log.w(TAG, "Fetching FCM registration token failed", e);
                    }
                });

                thread.start();

                try {
                    thread.join(15000);
                    if (thread.isAlive()) {
                        Log.w(TAG, "Timeout waiting for FCM token");
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interrupted while waiting for FCM token", e);
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "This is the first time the application is launched, there is no login data");
        }

        String result = token.get();
        Log.d(TAG, "FCM Token: " + result);
        return result;
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE);
            }
        }
    }
}
