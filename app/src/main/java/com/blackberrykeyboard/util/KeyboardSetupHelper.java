package com.blackberrykeyboard.util;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import com.blackberrykeyboard.ime.KeyboardIME;

import java.util.List;

public final class KeyboardSetupHelper {
    private static final String IME_ID = "com.blackberrykeyboard/.ime.KeyboardIME";

    private KeyboardSetupHelper() {
    }

    public static boolean isEnabled(Context context) {
        InputMethodManager imm = inputMethodManager(context);
        if (imm == null) {
            return false;
        }
        List<InputMethodInfo> methods = imm.getEnabledInputMethodList();
        for (InputMethodInfo info : methods) {
            if (IME_ID.equals(info.getId())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isDefault(Context context) {
        String current = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        return IME_ID.equals(current);
    }

    public static boolean isReady(Context context) {
        return isEnabled(context) && isDefault(context);
    }

    public static void showInputMethodPicker(Context context) {
        InputMethodManager imm = inputMethodManager(context);
        if (imm != null) {
            imm.showInputMethodPicker();
        }
    }

    private static InputMethodManager inputMethodManager(Context context) {
        return (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    public static ComponentName imeComponent() {
        return new ComponentName("com.blackberrykeyboard", KeyboardIME.class.getName());
    }
}
