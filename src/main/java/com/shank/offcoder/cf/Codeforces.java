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
import com.shank.offcoder.app.NetworkClient;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;

import java.io.IOException;
import java.util.Random;

/**
 * Class for handling tasks for Codeforces
 */
public class Codeforces {

    public static final String HOST = "https://codeforces.com";
    private static final String CHAR_DAT = "abcdefghijklmnopqrstuvwxyz0123456789";

    // UID needed for logout
    private static String LOG_OUT_UID = AppData.NULL_STR;

    // --------- LOGIN/LOGOUT SPECIFIC CODE --------- //

    /**
     * Function to log in on codeforces
     *
     * @param handle   codeforces handle
     * @param password password for the handle
     */
    public static void login(String handle, String password, AppThreader.EventListener<String> listener) {
        NetworkClient.get().clearData();
        String url = HOST + "/enter";
        NetworkClient.get().ReqGet(url, body -> {
            FormElement formElement = (FormElement) body.select("form#enterForm").first();
            if (formElement == null) {
                System.out.println("Form NULL");
                listener.onEvent("Error");
                return;
            }
            String csrf = formElement.select("input[name=\"csrf_token\"]").val();
            NetworkClient.get().setParams(csrf, genFTAA(), handle, password);

            formElement.select("input#handleOrEmail").val(handle);
            formElement.select("input#password").val(password);
            try {
                Connection.Response response = formElement.submit().cookies(NetworkClient.get().getCookies()).data(NetworkClient.get().getParams())
                        .followRedirects(true).execute();
                NetworkClient.get().updateCookies(response.cookies());
                Document html = response.parse();

                LOG_OUT_UID = findLogOutUID(html.toString());
                String mHandle;
                listener.onEvent(response.statusCode() == 200 && !(mHandle = findHandle(html.toString())).isEmpty() ? mHandle : "Error");
            } catch (IOException e) {
                e.printStackTrace();
                listener.onEvent("Error");
            }
        });
    }

    /**
     * Function to log out from codeforces
     */
    public static void logout(AppThreader.EventListener<Boolean> listener) {
        if (LOG_OUT_UID.equals(AppData.NULL_STR)) {
            listener.onEvent(false);
            return;
        }
        NetworkClient.get().ReqGet(HOST + "/" + LOG_OUT_UID + "/logout", data -> {
            NetworkClient.get().clearData();
            listener.onEvent(!hasError(data));
        });
    }

    public static String genFTAA() {
        StringBuilder ftaa = new StringBuilder();
        for (int i = 0; i < 18; i++) ftaa.append(CHAR_DAT.charAt(new Random().nextInt(CHAR_DAT.length())));
        return ftaa.toString();
    }

    private static String findHandle(String body) {
        int index = body.indexOf("handle = ");
        if (index == -1) return "";
        int end = body.indexOf("\"", index + 10);
        return body.substring(index + 10, end);
    }

    private static String findLogOutUID(String body) {
        int index = body.indexOf("/logout");
        if (index == -1) return AppData.NULL_STR;
        int start = body.indexOf("/", index - 33);
        return body.substring(start + 1, index);
    }

    private static boolean hasError(Document doc) {
        try {
            Element ele = doc.select("p#OffError").first();
            return ele != null && ele.val().equals("Error");
        } catch (Exception e) {
            return false;
        }
    }
}
