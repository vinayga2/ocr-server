package com.optum.ocr.util;

import java.util.Locale;
import java.util.ResourceBundle;

public class Messages {
    static ResourceBundle messages;

    public static void init() {
        if (messages==null) {
            messages = ResourceBundle.getBundle("message", Locale.ENGLISH);
        }
    }

    public static String getMessage(String code) {
        init();
        String str = messages.getString(code);
        return str;
    }
}