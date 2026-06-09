package com.blackberrykeyboard.prefs;

import android.content.Context;
import android.content.SharedPreferences;

public final class KeyboardPreferences {
    private static final String PREFS = "bb_keyboard_prefs";

    private static final String KEY_THEME = "theme";
    private static final String KEY_SOUND = "sound_enabled";
    private static final String KEY_LEARN = "learn_enabled";
    private static final String KEY_KEY_SCALE = "key_scale";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_TWO_TYPE = "two_type_mode";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";

    private static KeyboardPreferences instance;

    private final SharedPreferences prefs;

    private KeyboardPreferences(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized KeyboardPreferences get(Context context) {
        if (instance == null) {
            instance = new KeyboardPreferences(context);
        }
        return instance;
    }

    public KeyboardTheme getTheme() {
        return KeyboardTheme.fromId(prefs.getString(KEY_THEME, KeyboardTheme.PEARL.id));
    }

    public void setTheme(KeyboardTheme theme) {
        prefs.edit().putString(KEY_THEME, theme.id).apply();
    }

    public boolean isSoundEnabled() {
        return prefs.getBoolean(KEY_SOUND, true);
    }

    public void setSoundEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply();
    }

    public boolean isLearnEnabled() {
        return prefs.getBoolean(KEY_LEARN, true);
    }

    public void setLearnEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_LEARN, enabled).apply();
    }

    public float getKeyScale() {
        return prefs.getFloat(KEY_KEY_SCALE, 1.15f);
    }

    public void setKeyScale(float scale) {
        prefs.edit().putFloat(KEY_KEY_SCALE, Math.max(1.0f, Math.min(1.35f, scale))).apply();
    }

    public boolean isOnboardingDone() {
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public void setOnboardingDone(boolean done) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply();
    }

    /** auto, es, or en */
    public String getLanguage() {
        return prefs.getString(KEY_LANGUAGE, "auto");
    }

    public void setLanguage(String code) {
        prefs.edit().putString(KEY_LANGUAGE, code).apply();
    }

    public String resolveLanguageCode() {
        String saved = getLanguage();
        if (!"auto".equals(saved)) {
            return saved;
        }
        String system = java.util.Locale.getDefault().getLanguage();
        if ("es".equals(system) || "en".equals(system)) {
            return system;
        }
        return "es";
    }

    /** 1 tap = 1st letter, 2 taps = 2nd letter on dual keys */
    public boolean isTwoTypeMode() {
        return prefs.getBoolean(KEY_TWO_TYPE, true);
    }

    public void setTwoTypeMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_TWO_TYPE, enabled).apply();
    }
}
