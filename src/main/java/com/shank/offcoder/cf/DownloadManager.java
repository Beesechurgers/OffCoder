/*
 * Copyright (c) 2021, Shashank Verma <shashank.verma2002@gmail.com>(shank03)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 */

package com.shank.offcoder.cf;

import com.shank.offcoder.app.AppData;
import com.shank.offcoder.app.AppThreader;
import com.shank.offcoder.controllers.Controller;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.util.List;

public class DownloadManager {

    private DownloadManager() {
    }

    /**
     * Method to download the questions.
     *
     * @param list       List of questions to download
     * @param controller The controller to access UI elements
     * @param listener   Callback when download is complete
     */
    public static void downloadQuestion(Controller controller, final List<ProblemParser.Problem> list, AppThreader.EventCallback<Integer> listener) {
        new Thread(() -> {
            JSONArray arr = AppData.get().getData(AppData.DOWNLOADED_QUES, new JSONArray());
            double counter = 0;
            int failedCount = 0;
            for (ProblemParser.Problem p : list) {
                ++counter;
                controller.downloadProgress.setProgress(counter / list.size());

                // Skip download if question of this `p.code` already exists
                if (arr.toString().contains(p.code)) continue;

                String html = ProblemParser.getQuestion(Codeforces.HOST + p.url, null);
                if (ProblemParser.hasError(Jsoup.parse(html))) {
                    failedCount++;
                    continue;
                }

                arr.put(new JSONObject().put(AppData.P_HTML_KEY, html).put(AppData.P_CODE_KEY, p.code).put(AppData.P_NAME_KEY, p.name).put(AppData.P_URL_KEY, p.url).put(AppData.P_ACCEPTED_KEY, p.accepted).put(AppData.P_RATING_KEY, p.rating));
            }
            AppData.get().writeData(AppData.DOWNLOADED_QUES, arr);
            listener.onEvent(failedCount);
        }).start();
    }

    public static boolean isQuestionDownloaded(String code) {
        JSONArray arr = AppData.get().getData(AppData.DOWNLOADED_QUES, new JSONArray());
        if (arr.isEmpty()) return false;

        for (Object obj : arr) {
            JSONObject jObj = (JSONObject) obj;
            if (jObj.getString(AppData.P_CODE_KEY).equals(code)) return true;
        }
        return false;
    }

    public static boolean areQuestionsDownloaded(List<ProblemParser.Problem> list) {
        JSONArray arr = AppData.get().getData(AppData.DOWNLOADED_QUES, new JSONArray());
        if (arr.isEmpty()) return false;

        int count = 0;
        for (ProblemParser.Problem p : list) {
            for (Object obj : arr) {
                JSONObject jObj = (JSONObject) obj;
                if (jObj.getString(AppData.P_CODE_KEY).equals(p.code)) {
                    count++;
                    break;
                }
            }
        }
        return count == list.size();
    }
}
