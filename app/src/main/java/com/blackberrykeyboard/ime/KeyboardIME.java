package com.blackberrykeyboard.ime;

import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.blackberrykeyboard.R;
import com.blackberrykeyboard.nativeengine.NativeEngine;
import com.blackberrykeyboard.prefs.KeyboardPreferences;
import com.blackberrykeyboard.util.KeySoundPlayer;

public class KeyboardIME extends InputMethodService implements BlackBerryKeyboardView.Listener {

    private static final int SUGGEST_DELAY_MS = 40;
    private static final long DOUBLE_SPACE_MS = 350;

    private NativeEngine engine;
    private BlackBerryKeyboardView keyboardView;
    private KeyboardPreferences prefs;
    private boolean isShifted;
    private boolean capsLock;
    private boolean capWords;
    private boolean capSentences;
    private boolean capitalizeWord;
    private long lastSpaceTime;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable updateSuggestionsRunnable = this::updateSuggestionsNow;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = KeyboardPreferences.get(this);
        engine = new NativeEngine(this, getFilesDir().getAbsolutePath());
        applyEngineSettings();
    }

    private void applyEngineSettings() {
        engine.setLearningEnabled(prefs.isLearnEnabled());
        engine.setLanguage(prefs.resolveLanguageCode());
        engine.setTwoTypeMode(prefs.isTwoTypeMode());
    }

    @Override
    public View onCreateInputView() {
        keyboardView = (BlackBerryKeyboardView) getLayoutInflater().inflate(
                R.layout.keyboard_view, null);
        keyboardView.applyPreferences(this);
        keyboardView.setListener(this);
        refreshSuggestions();
        return keyboardView;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        if (keyboardView != null) {
            keyboardView.applyPreferences(this);
            applyEngineSettings();
        }
        updateShiftFromEditor(info);
        if (restarting) {
            engine.resetComposition();
        }
        resetWordCapitalization();
        refreshSuggestions();
        keyboardView.setShifted(isShifted || capsLock);
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        engine.onCommit();
        engine.saveUserDictionary();
        super.onFinishInputView(finishingInput);
    }

    @Override
    public void onDestroy() {
        if (engine != null) {
            engine.saveUserDictionary();
            engine.destroy();
            engine = null;
        }
        super.onDestroy();
    }

    private void updateShiftFromEditor(EditorInfo info) {
        int inputType = info.inputType;
        int capFlags = inputType & InputType.TYPE_MASK_FLAGS;

        capsLock = false;
        capWords = (capFlags & InputType.TYPE_TEXT_FLAG_CAP_WORDS) != 0;
        capSentences = (capFlags & InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0;
        isShifted = capSentences || capWords;
    }

    private void resetWordCapitalization() {
        capitalizeWord = capSentences || capWords || capsLock;
    }

    private void onWordBoundary() {
        if (capWords || capSentences) {
            capitalizeWord = true;
        }
        if (capSentences && !capsLock) {
            isShifted = true;
            if (keyboardView != null) {
                keyboardView.setShifted(true);
            }
        }
    }

    private InputConnection ic() {
        return getCurrentInputConnection();
    }

    private String composingForDisplay(String composing) {
        if (composing == null || composing.isEmpty()) {
            return composing;
        }
        if (isShifted || capsLock || capitalizeWord) {
            return Character.toUpperCase(composing.charAt(0)) + composing.substring(1);
        }
        return composing;
    }

    private String formatWord(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        if (isShifted || capsLock || capitalizeWord) {
            return Character.toUpperCase(word.charAt(0)) + word.substring(1);
        }
        return word;
    }

    private void refreshSuggestions() {
        updateSuggestionsNow();
    }

    private void scheduleSuggestions() {
        mainHandler.removeCallbacks(updateSuggestionsRunnable);
        mainHandler.postDelayed(updateSuggestionsRunnable, SUGGEST_DELAY_MS);
    }

    private void updateSuggestionsNow() {
        if (keyboardView == null || engine == null) {
            return;
        }
        engine.updatePredictions();
        String composing = engine.getComposingText();
        String[] suggestions = engine.getSuggestions(3);
        keyboardView.updateSuggestions(composingForDisplay(composing), suggestions);
    }

    private void playKeySound() {
        KeySoundPlayer.get().playKey(this);
    }

    private void commitFromEngine(char out) {
        InputConnection conn = ic();
        if (conn == null) {
            return;
        }

        String composing = engine.getComposingText();
        if (composing.isEmpty()) {
            conn.finishComposingText();
            conn.commitText(String.valueOf(out), 1);
        } else {
            conn.setComposingText(composingForDisplay(composing), 1);
        }

        if (isShifted && !capsLock) {
            isShifted = false;
            keyboardView.setShifted(false);
        }
        if (capitalizeWord && !capsLock) {
            capitalizeWord = false;
        }

        scheduleSuggestions();
    }

    @Override
    public void onLetter(char letter) {
        playKeySound();
        char out = engine.onKey(letter, isShifted || capsLock);
        commitFromEngine(out);
    }

    @Override
    public void onTwoTypeFirst(char letter) {
        playKeySound();
        char out = engine.onKey(letter, isShifted || capsLock);
        commitFromEngine(out);
    }

    @Override
    public void onTwoTypeSecond(char letter) {
        playKeySound();
        char out = engine.onReplaceLastKey(letter, isShifted || capsLock);
        commitFromEngine(out);
    }

    @Override
    public void onSureTypeKey(char first, char second) {
        playKeySound();
        char out = engine.onDualKey(first, second, isShifted || capsLock);
        commitFromEngine(out);
    }

    @Override
    public void onSymbol(String text) {
        playKeySound();
        InputConnection conn = ic();
        if (conn == null || text == null || text.isEmpty()) {
            return;
        }
        engine.onCommit();
        conn.finishComposingText();
        conn.commitText(text, 1);
        onWordBoundary();
        refreshSuggestions();
    }

    @Override
    public void onBackspace() {
        playKeySound();
        InputConnection conn = ic();
        if (conn == null) {
            return;
        }

        String composing = engine.getComposingText();
        if (!composing.isEmpty()) {
            engine.onBackspace();
            composing = engine.getComposingText();
            if (composing.isEmpty()) {
                conn.finishComposingText();
            } else {
                conn.setComposingText(composingForDisplay(composing), 1);
            }
        } else {
            conn.deleteSurroundingText(1, 0);
        }

        scheduleSuggestions();
    }

    @Override
    public void onSpace() {
        playKeySound();
        InputConnection conn = ic();
        if (conn == null) {
            return;
        }

        long now = System.currentTimeMillis();
        String composing = engine.getComposingText();
        boolean doubleSpace = (now - lastSpaceTime) < DOUBLE_SPACE_MS;
        lastSpaceTime = now;

        if (doubleSpace) {
            if (!composing.isEmpty()) {
                engine.updatePredictions();
                String[] suggestions = engine.getSuggestions(3);
                if (suggestions.length > 0) {
                    onSuggestionSelected(0);
                    return;
                }
            } else if (tryReplaceLastWordWithSuggestion(conn)) {
                return;
            }
        }

        if (!composing.isEmpty()) {
            conn.setComposingText(formatWord(composing), 1);
        }
        engine.onSpace();
        conn.finishComposingText();
        conn.commitText(" ", 1);
        onWordBoundary();
        refreshSuggestions();
    }

    private boolean tryReplaceLastWordWithSuggestion(InputConnection conn) {
        CharSequence before = conn.getTextBeforeCursor(32, 0);
        if (before == null || before.length() == 0) {
            return false;
        }
        String text = before.toString();
        if (!text.endsWith(" ")) {
            return false;
        }
        text = text.substring(0, text.length() - 1);
        int lastSpace = text.lastIndexOf(' ');
        String lastWord = lastSpace >= 0 ? text.substring(lastSpace + 1) : text;
        if (lastWord.isEmpty()) {
            return false;
        }

        engine.resetComposition();
        for (int i = 0; i < lastWord.length(); i++) {
            engine.onKey(lastWord.charAt(i), false);
        }
        engine.updatePredictions();
        String[] suggestions = engine.getSuggestions(3);
        if (suggestions.length == 0 || suggestions[0].equals(lastWord)) {
            engine.resetComposition();
            return false;
        }

        conn.deleteSurroundingText(lastWord.length() + 1, 0);
        engine.applySuggestion(0);
        String word = engine.getComposingText();
        conn.commitText(formatWord(word) + " ", 1);
        engine.onSpace();
        onWordBoundary();
        refreshSuggestions();
        return true;
    }

    @Override
    public void onEnter() {
        playKeySound();
        InputConnection conn = ic();
        if (conn == null) {
            return;
        }

        engine.onCommit();
        conn.finishComposingText();

        EditorInfo ei = getCurrentInputEditorInfo();
        if (ei != null && (ei.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0) {
            conn.performEditorAction(ei.imeOptions & EditorInfo.IME_MASK_ACTION);
        } else {
            conn.commitText("\n", 1);
        }
        refreshSuggestions();
    }

    @Override
    public void onShift() {
        playKeySound();
        if (isShifted) {
            capsLock = !capsLock;
        } else {
            isShifted = true;
        }
        keyboardView.setShifted(isShifted || capsLock);
        refreshSuggestions();
    }

    @Override
    public void onToggleSym() {
        // handled in view
    }

    @Override
    public void onToggleAlt() {
        // handled in view
    }

    @Override
    public void onSuggestionSelected(int index) {
        InputConnection conn = ic();
        if (conn == null) {
            return;
        }

        engine.applySuggestion(index);
        String word = engine.getComposingText();
        if (word.isEmpty()) {
            return;
        }

        conn.setComposingText("", 1);
        conn.commitText(formatWord(word) + " ", 1);
        engine.onSpace();
        onWordBoundary();

        if (isShifted && !capsLock) {
            isShifted = false;
            keyboardView.setShifted(false);
        }

        refreshSuggestions();
    }
}
