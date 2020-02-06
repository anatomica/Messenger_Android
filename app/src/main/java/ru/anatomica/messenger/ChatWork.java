package ru.anatomica.messenger;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import ru.anatomica.messenger.gson.Message;
import ru.anatomica.messenger.gson.WorkWithGroup;

class ChatWork {

    private String nameGroup;
    private MainActivity mainActivity;
    private MessageService messageService;
    private AlertDialog.Builder alertDialog;

    ChatWork(MainActivity mainActivity, MessageService messageService) {
        this.mainActivity = mainActivity;
        this.messageService = messageService;
    }

    void joinToGroup() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mainActivity);
        alertDialog.setTitle("Присоединение к группе ...");
        alertDialog.setMessage("Введите имя группы:");

        final EditText input = new EditText(mainActivity);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alertDialog.setView(input);

        alertDialog.setPositiveButton("Присоединиться!", (dialog, which) -> {
            nameGroup = input.getText().toString();
            if (nameGroup.compareTo("") > 0 && !nameGroup.equals("") && nameGroup != null) {
                createGroupChat(nameGroup, "","4");
            } else mainActivity.runOnUiThread(() -> messageService.serviceMessage("Введите, пожалуйста, название группы!"));
        });
        alertDialog.setNegativeButton("Отменить!", (dialog, which) -> dialog.cancel());
        alertDialog.show();
    }

    void createGroup() {
        alertDialog = new AlertDialog.Builder(mainActivity);
        alertDialog.setTitle("Создание группы ...");
        alertDialog.setMessage("Введите имя группы:");

        final EditText input = new EditText(mainActivity);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alertDialog.setView(input);

        alertDialog.setPositiveButton("Ввести пароль!", (dialog, which) -> {
            nameGroup = input.getText().toString();
            if (nameGroup.compareTo("") > 0 && !nameGroup.equals("") && !nameGroup.startsWith(" ") && nameGroup != null) {
                createPass();
            } else mainActivity.runOnUiThread(() -> messageService.serviceMessage("Пожалуйста, введите название группы!"));
        });
        alertDialog.setNegativeButton("Отмена!", (dialog, which) -> dialog.cancel());
        alertDialog.show();
    }

    private void createPass() {
        alertDialog = new AlertDialog.Builder(mainActivity);
        alertDialog.setTitle("Создание группы ...");
        alertDialog.setMessage("Введите пароль:");

        final EditText input = new EditText(mainActivity);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alertDialog.setView(input);

        alertDialog.setPositiveButton("Создать!", (dialog, which) -> {
            String password = input.getText().toString();
            if (password.compareTo("") > 0 && !password.equals("") && !password.startsWith(" ") && password != null) {
                createGroupChat(nameGroup, password, "1");
            } else mainActivity.runOnUiThread(() -> messageService.serviceMessage("Пожалуйста, введите пароль!"));
        });
        alertDialog.setNegativeButton("Отмена!", (dialog, which) -> dialog.cancel());
        alertDialog.show();
    }

    void deleteGroup() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mainActivity);
        alertDialog.setTitle("Удаление чата " + mainActivity.selectedButton);
        alertDialog.setMessage("Введите название чата:");

        final EditText input = new EditText(mainActivity);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alertDialog.setView(input);

        alertDialog.setPositiveButton("Удалить!", (dialog, which) -> {
            nameGroup = input.getText().toString();
            if (nameGroup.compareTo("") > 0) {
                deleteGroupChat(nameGroup);
            }
        });
        alertDialog.setNegativeButton("Отмена!", (dialog, which) -> dialog.cancel());
        alertDialog.show();
    }

    private void createGroupChat(String nameGroup, String password, String identify) {
        Message msg = buildWorkWithGroup(nameGroup, password, identify);
        messageService.sendMessage(msg.toJson());
    }

    void deleteGroupChat(String nameGroup) {
        Message msg = buildWorkWithGroup(nameGroup,"","0");
        messageService.sendMessage(msg.toJson());
        deleteChatButton(nameGroup);
    }

    void createChatButton(WorkWithGroup workWithGroup) {
        if (mainActivity.addNewContact("1 ", workWithGroup.nameGroup)) {
            mainActivity.runOnUiThread(mainActivity::loadAllContacts);
            mainActivity.runOnUiThread(mainActivity::createBtn);
        }
    }

    void deleteChatButton(String nameGroup) {
        try {
            mainActivity.fos = mainActivity.openFileOutput("AllContacts.txt", Context.MODE_APPEND);
            mainActivity.fis = mainActivity.openFileInput("AllContacts.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(mainActivity.fis, "UTF-8"));
            String contact;
            List<String> tmpContacts = new ArrayList<>();
            while ((contact = br.readLine()) != null) tmpContacts.add(contact);

            mainActivity.fos = mainActivity.openFileOutput("AllContacts.txt", Context.MODE_PRIVATE);
            String zero = "";
            mainActivity.fos.write(zero.getBytes());
            mainActivity.fos = mainActivity.openFileOutput("AllContacts.txt", Context.MODE_APPEND);

            for (int i = 0; i < tmpContacts.size(); i++) {
                if (!tmpContacts.get(i).equals("1 " + nameGroup)  && !tmpContacts.get(i).equals("0 " + nameGroup)) {
                    String newContact = tmpContacts.get(i) + "\n";
                    mainActivity.fos.write(newContact.getBytes());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(mainActivity, "Успешно удалено!", Toast.LENGTH_LONG).show();
        mainActivity.runOnUiThread(mainActivity::loadAllContacts);
        mainActivity.runOnUiThread(mainActivity::createBtn);
    }

    void checkPassGroup(String group, String pass) {
        Message msg = buildWorkWithGroup(group, pass, "2");
        messageService.sendMessage(msg.toJson());
    }

    void savePass(String selectedButton, String passGroup) {
        try {
            mainActivity.fos = mainActivity.openFileOutput("Pass" + selectedButton + ".txt", Context.MODE_PRIVATE);
            mainActivity.fos.write(passGroup.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void deletePass(WorkWithGroup workWithGroup) {
        String str = "";
        try {
            mainActivity.fos = mainActivity.openFileOutput("Pass" + workWithGroup.nameGroup + ".txt", Context.MODE_PRIVATE);
            mainActivity.fos.write(str.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Message buildWorkWithGroup(String nameGroup, String password, String identify) {
        WorkWithGroup msg = new WorkWithGroup();
        msg.nameGroup = nameGroup;
        msg.password = password;
        msg.identify = identify;
        return Message.workWithGroup(msg);
    }

    boolean autoLoginToGroup(String group) {
        try {
            mainActivity.fis = mainActivity.openFileInput("Pass" + group + ".txt");
            if (mainActivity.fis.available() > 0) {
                int available = mainActivity.fis.available();
                byte[] bufPass = new byte[available];
                mainActivity.fis.read(bufPass);
                String pass = new String(bufPass);
                mainActivity.fis.close();
                Message msg = buildWorkWithGroup(group, pass, "2");
                messageService.sendMessage(msg.toJson());
                mainActivity.requestClientsList(group);
                mainActivity.contact.setVisibility(View.INVISIBLE);
                mainActivity.spinner.setVisibility(View.VISIBLE);
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String checkNewName(String nickname) {
        try {
            mainActivity.fos = mainActivity.openFileOutput("NewName.txt", Context.MODE_APPEND);
            mainActivity.fis = mainActivity.openFileInput("NewName.txt");

            BufferedReader br = new BufferedReader(new InputStreamReader(mainActivity.fis, "UTF-8"));
            String contact;
            while ((contact = br.readLine()) != null) {
                String oldNickName = contact.split("\\s+", 2)[0];
                String newNickName = contact.split("\\s+", 2)[1];
                if (nickname.equals(oldNickName))
                    return newNickName;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nickname;
    }

    public void makeNewName(String selectedButton) {
            alertDialog = new AlertDialog.Builder(mainActivity);
            alertDialog.setTitle("Переименование контакта ...");
            alertDialog.setMessage("Введите новое имя:");

            final EditText input = new EditText(mainActivity);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            input.setLayoutParams(lp);
            alertDialog.setView(input);

            alertDialog.setPositiveButton("Переименовать!", (dialog, which) -> {
                String newName = input.getText().toString();
                if (newName.compareTo("") > 0 && !newName.equals("") && !newName.startsWith(" ") && newName != null) {
                    writeNewName(selectedButton, newName);
                } else mainActivity.runOnUiThread(() -> messageService.serviceMessage("Пожалуйста, введите пароль!"));
            });
            alertDialog.setNegativeButton("Отмена!", (dialog, which) -> dialog.cancel());
            alertDialog.show();
    }

    private void writeNewName(String selectedButton, String newName) {
        try {
            mainActivity.fos = mainActivity.openFileOutput("NewName.txt", Context.MODE_APPEND);
            mainActivity.fis = mainActivity.openFileInput("NewName.txt");
            String tmp = selectedButton + " " + newName + "\n";
            mainActivity.fos.write(tmp.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        mainActivity.runOnUiThread(mainActivity::loadAllContacts);
        mainActivity.runOnUiThread(mainActivity::createBtn);
    }

    public void restoreContacts() {
        String str = "";
        try {
            mainActivity.fos = mainActivity.openFileOutput("NewName.txt", Context.MODE_PRIVATE);
            mainActivity.fis = mainActivity.openFileInput("NewName.txt");
            mainActivity.fos.write(str.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        mainActivity.runOnUiThread(mainActivity::loadAllContacts);
        mainActivity.runOnUiThread(mainActivity::createBtn);
    }
}
