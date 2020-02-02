package ru.anatomica.messenger;

import android.app.AlertDialog;
import android.content.Context;
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

public class ChatWork {

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

    private void deleteGroupChat(String nameGroup) {
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
//        Message msg = buildWorkWithGroup(nameGroup, "0");
//        messageService.sendMessage(msg.toJson());
    }

    void createChatButton(WorkWithGroup workWithGroup) {
        if (mainActivity.addNewContact("1 ", workWithGroup.nameGroup)) {
            mainActivity.runOnUiThread(mainActivity::loadAllContacts);
            mainActivity.runOnUiThread(mainActivity::createBtn);
        }
    }

    void deleteChatButton(WorkWithGroup workWithGroup) {

    }

    private Message buildWorkWithGroup(String nameGroup, String identify) {
        WorkWithGroup msg = new WorkWithGroup();
        msg.identify = identify;
        msg.nameGroup = nameGroup;
        return Message.workWithGroup(msg);
    }
}
