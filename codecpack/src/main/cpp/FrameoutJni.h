#pragma once
#include "FfmpegError.h"
#include <jni.h>
#include <string>

static inline std::string pathFromJni(JNIEnv* env, jstring value) {
    const char* raw = env->GetStringUTFChars(value, nullptr);
    std::string path = raw ? raw : "";
    if (raw) env->ReleaseStringUTFChars(value, raw);
    return path;
}

static inline jint failed(JNIEnv* env, jstring path) {
    try {
        frameout::recordError(pathFromJni(env, path), frameout::classifyException());
    } catch (...) {
    }
    return -100;
}

static inline jlongArray failedArray(JNIEnv* env, jstring path) {
    failed(env, path);
    return nullptr;
}
