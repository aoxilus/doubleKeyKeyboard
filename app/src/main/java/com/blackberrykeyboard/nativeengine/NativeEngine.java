package com.blackberrykeyboard.nativeengine;

import android.content.Context;

import com.blackberrykeyboard.dict.DictionaryLoader;

public final class NativeEngine {
    static {
        System.loadLibrary("bbkeyboard");
    }

    private long handle;

    public NativeEngine(Context context, String dataDir) {
        DictionaryLoader.ensureDictionaries(context);
        handle = nativeCreate(dataDir);
    }

    public void destroy() {
        if (handle != 0) {
            nativeDestroy(handle);
            handle = 0;
        }
    }

    public char onKey(char letter, boolean isShift) {
        return nativeOnKey(handle, letter, isShift);
    }

    public char onDualKey(char a, char b, boolean isShift) {
        return nativeOnDualKey(handle, a, b, isShift);
    }

    public char onReplaceLastKey(char letter, boolean isShift) {
        return nativeOnReplaceLastKey(handle, letter, isShift);
    }

    public void onBackspace() {
        nativeOnBackspace(handle);
    }

    public void onSpace() {
        nativeOnSpace(handle);
    }

    public void onCommit() {
        nativeOnCommit(handle);
    }

    public String getComposingText() {
        return nativeGetComposingText(handle);
    }

    public String[] getSuggestions(int maxCount) {
        return nativeGetSuggestions(handle, maxCount);
    }

    public void applySuggestion(int index) {
        nativeApplySuggestion(handle, index);
    }

    public void resetComposition() {
        nativeResetComposition(handle);
    }

    public void saveUserDictionary() {
        nativeSaveUserDictionary(handle);
    }

    public int getLearnedWordCount() {
        return nativeGetLearnedWordCount(handle);
    }

    public void clearUserDictionary() {
        nativeClearUserDictionary(handle);
    }

    public void setLearningEnabled(boolean enabled) {
        nativeSetLearningEnabled(handle, enabled);
    }

    public void setLanguage(String languageCode) {
        nativeSetLanguage(handle, languageCode);
    }

    public void setTwoTypeMode(boolean enabled) {
        nativeSetTwoTypeMode(handle, enabled);
    }

    public void updatePredictions() {
        nativeUpdatePredictions(handle);
    }

    private static native long nativeCreate(String dataDir);
    private static native void nativeDestroy(long handle);
    private static native char nativeOnKey(long handle, char letter, boolean isShift);
    private static native char nativeOnDualKey(long handle, char a, char b, boolean isShift);
    private static native char nativeOnReplaceLastKey(long handle, char letter, boolean isShift);
    private static native void nativeOnBackspace(long handle);
    private static native void nativeOnSpace(long handle);
    private static native void nativeOnCommit(long handle);
    private static native String nativeGetComposingText(long handle);
    private static native String[] nativeGetSuggestions(long handle, int maxCount);
    private static native void nativeApplySuggestion(long handle, int index);
    private static native void nativeResetComposition(long handle);
    private static native void nativeSaveUserDictionary(long handle);
    private static native int nativeGetLearnedWordCount(long handle);
    private static native void nativeClearUserDictionary(long handle);
    private static native void nativeSetLearningEnabled(long handle, boolean enabled);
    private static native void nativeSetLanguage(long handle, String languageCode);
    private static native void nativeSetTwoTypeMode(long handle, boolean enabled);
    private static native void nativeUpdatePredictions(long handle);
}
