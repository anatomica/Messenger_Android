package ru.anatomica.messenger;

import android.content.Context;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ChatHistory {

    private static int howManyMsgLoad = 100;
    private MainActivity mainActivity;

    public ChatHistory(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    void createFile() {
        try {
            String str = "\n";
            mainActivity.fos.write(str.getBytes());
            mainActivity.fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void outputPath(String addNew, String name, String fileHistory, String from, String message) throws FileNotFoundException {
        mainActivity.fos = mainActivity.openFileOutput( addNew + name + fileHistory, Context.MODE_APPEND);
        writeChatHistory(from + " : " + message);
    }

    void writeChatHistory(String messageText) {
        try {
            String str = messageText + "\n";
            mainActivity.fos.write(str.getBytes());
            // mainActivity.fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void loadChatHistory(String fileToLoad) throws IOException {
        mainActivity.textArea.setText("");
        mainActivity.textArea.append("Последние " + howManyMsgLoad + " сообщений:\n");

        mainActivity.fis = mainActivity.openFileInput(fileToLoad);
        BufferedReader br = new BufferedReader(new InputStreamReader(mainActivity.fis, "UTF-8"));
        String text;
        while ((text = br.readLine()) != null) {
               mainActivity.textArea.append(text + "\n");
        }
        loadNewChatHistory(fileToLoad);

//        List<String> listHistory = new ArrayList<>();
//
//        String tmp;
//        while ((tmp = br.readLine()) != null) {
//            listHistory.add("\n" + tmp);
//        }
//        Collections.reverse(listHistory);
//
//        List<String> reverseListHistory = new ArrayList<>();
//        int count = 1;
//        for (int i = 0; i < listHistory.size(); i++) {
//            if (count <= howManyMsgLoad) {
//                reverseListHistory.add(listHistory.get(i));
//                count++;
//            }
//        }
//        Collections.reverse(reverseListHistory);
//
//        StringBuilder chatHistory = new StringBuilder();
//        for (String s : reverseListHistory) {
//            chatHistory.append(s);
//        }
//        MessageService.textArea.append(chatHistory + "\n");
    }

    private void loadNewChatHistory(String fileToLoad) throws IOException {
        mainActivity.fos = mainActivity.openFileOutput("New" + fileToLoad, Context.MODE_APPEND);
        mainActivity.fis = mainActivity.openFileInput("New" + fileToLoad);
        mainActivity.fos = mainActivity.openFileOutput(fileToLoad, Context.MODE_APPEND);
        if (mainActivity.fis.available() > 0) {
            BufferedReader br = new BufferedReader(new InputStreamReader(mainActivity.fis, "UTF-8"));
            String text;
            int count = 0;
            while ((text = br.readLine()) != null) {
                if (count == 0) {
                    mainActivity.textArea.append("Новые сообщения: \n");
                    count++;
                }
                mainActivity.textArea.append(text + "\n");
                writeChatHistory(text);
            }
            mainActivity.fos = mainActivity.openFileOutput("New" + fileToLoad, Context.MODE_PRIVATE);
            String str = "";
            mainActivity.fos.write(str.getBytes());
        }
    }

    public void sendDataToServerGroup (String nameGroupe) {

    }
}
