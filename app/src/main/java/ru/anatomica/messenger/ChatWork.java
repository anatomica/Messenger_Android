package ru.anatomica.messenger;

import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import ru.anatomica.messenger.gson.Message;
import ru.anatomica.messenger.gson.WorkWithGroup;

public class ChatWork {

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
            String nameGroup = input.getText().toString();
            if (nameGroup.compareTo("") > 0) {
                groupExist(nameGroup);
            }
        });
        alertDialog.setNegativeButton("Отменить!", (dialog, which) -> dialog.cancel());
        alertDialog.show();
    }

    void deleteGroup() {

    }

    private void groupExist(String nameGroup) {
        Message msg = buildWorkWithGroup(nameGroup, "2");
        messageService.sendMessage(msg.toJson());
    }

    void createGroupChat(WorkWithGroup workWithGroup) {
        if (workWithGroup.trueOrFalse.equals("true")) {
            Toast.makeText(mainActivity.getApplicationContext(), "Группа уже существует!", Toast.LENGTH_SHORT).show();
        } else {
            Message msg = buildWorkWithGroup(workWithGroup.nameGroup, "1");
            messageService.sendMessage(msg.toJson());

            Toast.makeText(mainActivity.getApplicationContext(), "Группа создана!", Toast.LENGTH_SHORT).show();
        }
    }

    void deleteGroupChat(WorkWithGroup workWithGroup) {

    }

    private Message buildWorkWithGroup(String nameGroup, String identify) {
        WorkWithGroup msg = new WorkWithGroup();
        msg.identify = identify;
        msg.nameGroup = nameGroup;
        return Message.workWithGroup(msg);
    }
}
