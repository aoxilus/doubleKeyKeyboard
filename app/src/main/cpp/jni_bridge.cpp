#include "keyboard_engine.h"

#include <jni.h>
#include <memory>
#include <vector>

namespace {

KeyboardEngine* fromHandle(jlong handle) {
    return reinterpret_cast<KeyboardEngine*>(handle);
}

jstring toJString(JNIEnv* env, const std::string& value) {
    return env->NewStringUTF(value.c_str());
}

jobjectArray toJStringArray(JNIEnv* env, const std::vector<std::string>& values) {
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray array = env->NewObjectArray(static_cast<jsize>(values.size()), stringClass, nullptr);
    for (jsize i = 0; i < static_cast<jsize>(values.size()); ++i) {
        env->SetObjectArrayElement(array, i, toJString(env, values[static_cast<size_t>(i)]));
    }
    return array;
}

}  // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_blackberrykeyboard_nativeengine_NativeEngine_nativeCreate(
    JNIEnv* env, jclass, jstring dataDir) {
    const char* dirChars = env->GetStringUTFChars(dataDir, nullptr);
    std::string dir(dirChars ? dirChars : "");
    env->ReleaseStringUTFChars(dataDir, dirChars);
    return reinterpret_cast<jlong>(new KeyboardEngine(dir));
}

extern "C" JNIEXPORT void JNICALL
Java_com_blackberrykeyboard_nativeengine_NativeEngine_nativeDestroy(
    JNIEnv*, jclass, jlong handle) {
    delete fromHandle(handle);
}

extern "C" JNIEXPORT jchar JNICALL
Java_com_blackberrykeyboard_nativeengine_NativeEngine_nativeOnKey(
    JNIEnv*, jclass, jlong handle, jchar letter, jboolean isShift) {
    return static_cast<jchar>(fromHandle(handle)->onKey(static_cast<char>(letter), isShift == JNI_TRUE));
}

extern "C" JNIEXPORT jchar JNICALL
Java_com_blackberrykeyboard_nativeengine_NativeEngine_nativeOnDualKey(
    JNIEnv*, jclass, jlong handle, jchar a, jchar b, jboolean isShift) {
    return static_cast<jchar>(fromHandle(handle)->onDualKey(
        static_cast<char>(a), static_cast<char>(b), isShift == JNI_TRUE));
}

extern "C" JNIEXPORT jchar JNICALL
Java_com_blackberrykeyboard_nativeengine_NativeEngine_nativeOnReplaceLastKey(
    JNIEnv*, jclass, jlong handle, jchar letter, jboolean isShift) {
    return static_cast<jchar>(fromHandle(handle)->onReplaceLastKey(
        static_cast<char>(letter), isShift == JNI_TRUE));
}

extern "C" JNIEXPORT void JNICALL
Java_com_blackberrykeyboard_nativeengine_NativeEngine_nativeOnBackspace(
    JNIEnv*, jclass, jlong handle) {
    fromHandle(handle)->onBackspace();
}

extern "C" JNIEXPORT void JNICALL
Java_com_blackberrykeyboard_nativeengine_NativeEngine_nativeOnSpace(
    JNIEnv*, jclass, jlong handle) {
    fromHandle(handle)->onSpace();
}

extern "C" JNIEXPORT void JNICALL
Java_com_blackberrykeyboard_nativeengine_NativeEngine_nativeOnCommit(
    JNIEnv*, jclass, jlong handle) {
    fromHandle(handle)->onCommit();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_blackberrykeyboard_nativeengine_NativeEngine_nativeGetComposingText(
    JNIEnv* env, jclass, jlong handle) {
    return toJString(env, fromHandle(handle)->getComposingText());
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_blackberrykeyboard_nativeengine_NativeEngine_nativeGetSuggestions(
    JNIEnv* env, jclass, jlong handle, jint maxCount) {
    return toJStringArray(env, fromHandle(handle)->getSuggestions(maxCount));
}

extern "C" JNIEXPORT void JNICALL
Java_com_blackberrykeyboard_nativeengine_NativeEngine_nativeApplySuggestion(
    JNIEnv*, jclass, jlong handle, jint index) {
    fromHandle(handle)->applySuggestion(index);
}

extern "C" JNIEXPORT void JNICALL
Java_com_blackberrykeyboard_nativeengine_NativeEngine_nativeResetComposition(
    JNIEnv*, jclass, jlong handle) {
    fromHandle(handle)->resetComposition();
}

extern "C" JNIEXPORT void JNICALL
Java_com_blackberrykeyboard_nativeengine_NativeEngine_nativeSaveUserDictionary(
    JNIEnv*, jclass, jlong handle) {
    fromHandle(handle)->saveUserDictionary();
}

extern "C" JNIEXPORT jint JNICALL
Java_com_blackberrykeyboard_nativeengine_NativeEngine_nativeGetLearnedWordCount(
    JNIEnv*, jclass, jlong handle) {
    return static_cast<jint>(fromHandle(handle)->getLearnedWordCount());
}

extern "C" JNIEXPORT void JNICALL
Java_com_blackberrykeyboard_nativeengine_NativeEngine_nativeClearUserDictionary(
    JNIEnv*, jclass, jlong handle) {
    fromHandle(handle)->clearUserDictionary();
}

extern "C" JNIEXPORT void JNICALL
Java_com_blackberrykeyboard_nativeengine_NativeEngine_nativeSetLearningEnabled(
    JNIEnv*, jclass, jlong handle, jboolean enabled) {
    fromHandle(handle)->setLearningEnabled(enabled == JNI_TRUE);
}

extern "C" JNIEXPORT void JNICALL
Java_com_blackberrykeyboard_nativeengine_NativeEngine_nativeSetLanguage(
    JNIEnv* env, jclass, jlong handle, jstring languageCode) {
    const char* code = env->GetStringUTFChars(languageCode, nullptr);
    std::string lang(code ? code : "auto");
    env->ReleaseStringUTFChars(languageCode, code);
    fromHandle(handle)->setLanguage(lang);
}

extern "C" JNIEXPORT void JNICALL
Java_com_blackberrykeyboard_nativeengine_NativeEngine_nativeSetTwoTypeMode(
    JNIEnv*, jclass, jlong handle, jboolean enabled) {
    fromHandle(handle)->setTwoTypeMode(enabled == JNI_TRUE);
}

extern "C" JNIEXPORT void JNICALL
Java_com_blackberrykeyboard_nativeengine_NativeEngine_nativeUpdatePredictions(
    JNIEnv*, jclass, jlong handle) {
    fromHandle(handle)->updatePredictions();
}
