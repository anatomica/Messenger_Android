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

    void writeChatHistory(String messageText) {
        try {
            String str = messageText + "\n";
            mainActivity.fos.write(str.getBytes());
            // mainActivity.fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void loadChatHistory() throws IOException {
        mainActivity.textArea.append("Последние " + howManyMsgLoad + " сообщений:\n");
//        BufferedReader br = new BufferedReader(new InputStreamReader(
//                new FileInputStream(String.valueOf(mainActivity.fis)), "UTF-8"));

        try {
            int available = mainActivity.fis.available();
            byte[] buffer = new byte[available];
            mainActivity.fis.read(buffer);
            String text = new String(buffer);
            mainActivity.textArea.append(text);
            mainActivity.fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

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
}
