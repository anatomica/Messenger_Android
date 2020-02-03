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

    ChatWork(MainActivity mainActivity, MessageService messageService) {
        this.mainActivity = mainActivity;
        this.messageService = messageService;
    }

    void createGroup() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mainActivity);
        alertDialog.setTitle("Создание группы ...");
        alertDialog.setMessage("Введите имя группы:");

        final EditText input = new EditText(mainActivity);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alertDialog.setView(input);

        alertDialog.setPositiveButton("Отправить!", (dialog, which) -> {
            nameGroup = input.getText().toString();
            if (nameGroup.compareTo("") > 0) {
                createGroupChat(nameGroup);
            }
        });
        alertDialog.setNegativeButton("Отменить!", (dialog, which) -> dialog.cancel());
        alertDialog.show();
    }

    void deleteGroup() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mainActivity);
        alertDialog.setTitle("Удаление чата ...");
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
        alertDialog.setNegativeButton("Нет!", (dialog, which) -> dialog.cancel());
        alertDialog.show();
    }

    private void createGroupChat(String nameGroup) {
        Message msg = buildWorkWithGroup(nameGroup, "1");
        messageService.sendMessage(msg.toJson());
    }

    void deleteGroupChat(String nameGroup) {
        Message msg = buildWorkWithGroup(nameGroup, "0");
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
        Message msg = buildCheckPass(group, pass, "2");
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

    private Message buildCheckPass(String nameGroup, String password, String identify) {
        WorkWithGroup msg = new WorkWithGroup();
        msg.nameGroup = nameGroup;
        msg.password = password;
        msg.identify = identify;
        return Message.workWithGroup(msg);
    }

    private Message buildWorkWithGroup(String nameGroup, String identify) {
        WorkWithGroup msg = new WorkWithGroup();
        msg.nameGroup = nameGroup;
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
                Message msg = buildCheckPass(group, pass, "2");
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

}
