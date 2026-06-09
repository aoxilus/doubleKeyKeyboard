package com.blackberrykeyboard.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;

import com.blackberrykeyboard.prefs.KeyboardPreferences;

public final class KeySoundPlayer {
    private static KeySoundPlayer instance;

    private ToneGenerator toneGenerator;
    private boolean initialized;

    private KeySoundPlayer() {
    }

    public static synchronized KeySoundPlayer get() {
        if (instance == null) {
            instance = new KeySoundPlayer();
        }
        return instance;
    }

    private void ensureInit() {
        if (initialized) {
            return;
        }
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, 60);
            initialized = true;
        } catch (RuntimeException ignored) {
            initialized = false;
        }
    }

    public void playKey(Context context) {
        KeyboardPreferences prefs = KeyboardPreferences.get(context);
        if (!prefs.isSoundEnabled()) {
            return;
        }
        ensureInit();
        if (toneGenerator != null) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 35);
        }
    }

    public void release() {
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
        initialized = false;
    }
}
