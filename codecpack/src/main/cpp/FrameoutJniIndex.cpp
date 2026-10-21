#include "FfmpegDecoder.h"
#include "FrameoutJni.h"
#include <jni.h>
#include <android/bitmap.h>
#include <cstdint>
#include <climits>
#include <vector>
#include <algorithm>

static jlongArray indexToArray(JNIEnv* env, jstring path, long long token);

extern "C" JNIEXPORT jlongArray JNICALL
Java_com_frameout_codecpack_FfmpegBridge_nativeFrameIndex(JNIEnv* env, jclass, jstring path) {
    try {
    return indexToArray(env, path, 0);
    } catch (...) {
    return failedArray(env, path);
    }
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_com_frameout_codecpack_FfmpegBridge_nativeFrameIndexCancellable(JNIEnv* env, jclass, jstring path, jlong token) {
    try {
    return indexToArray(env, path, static_cast<long long>(token));
    } catch (...) {
    return failedArray(env, path);
    }
}

static jlongArray framesToArray(JNIEnv* env, const std::vector<frameout::IndexedFrame>& frames) {
    if (frames.size() > static_cast<std::size_t>(INT_MAX / 4)) return nullptr;
    const jsize size = static_cast<jsize>(frames.size() * 4);
    jlongArray result = env->NewLongArray(size);
    if (!result) return nullptr;
    std::vector<jlong> values;
    values.reserve(size);
    for (const auto& frame : frames) {
        values.push_back(static_cast<jlong>(frame.ptsUs));
        values.push_back(frame.keyframe ? 1 : 0);
        values.push_back(static_cast<jlong>(frame.nearestKeyframeIndex));
        values.push_back(static_cast<jlong>(frame.nearestKeyframePtsUs));
    }
    env->SetLongArrayRegion(result, 0, size, values.data());
    return result;
}

static jlongArray indexToArray(JNIEnv* env, jstring path, long long token) {
    std::vector<frameout::IndexedFrame> frames;
    if (frameout::indexFrames(pathFromJni(env, path), frames, token) < 0) return nullptr;
    return framesToArray(env, frames);
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_com_frameout_codecpack_FfmpegBridge_nativeFrameIndexPrefix(JNIEnv* env, jclass, jstring path, jint maxPackets, jbooleanArray completeOut) {
    try {
    std::vector<frameout::IndexedFrame> frames;
    bool complete = true;
    if (frameout::indexFramesPrefix(pathFromJni(env, path), frames, maxPackets, complete) < 0) return nullptr;
    if (completeOut && env->GetArrayLength(completeOut) > 0) {
        const jboolean flag = complete ? JNI_TRUE : JNI_FALSE;
        env->SetBooleanArrayRegion(completeOut, 0, 1, &flag);
    }
    return framesToArray(env, frames);
    } catch (...) {
    return failedArray(env, path);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_frameout_codecpack_FfmpegBridge_nativeSetIndexCancel(JNIEnv*, jclass, jlong token, jboolean cancelled) {
    try {
    frameout::setIndexCancel(static_cast<long long>(token), cancelled == JNI_TRUE);
    } catch (...) {
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_frameout_codecpack_FfmpegBridge_nativeReleaseIndexCancel(JNIEnv*, jclass, jlong token) {
    try {
    frameout::releaseIndexCancel(static_cast<long long>(token));
    } catch (...) {
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_frameout_codecpack_FfmpegBridge_nativeWidth(JNIEnv* env, jclass, jstring path) {
    try {
    return frameout::width(pathFromJni(env, path));
    } catch (...) {
    return failed(env, path);
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_frameout_codecpack_FfmpegBridge_nativeHeight(JNIEnv* env, jclass, jstring path) {
    try {
    return frameout::height(pathFromJni(env, path));
    } catch (...) {
    return failed(env, path);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_frameout_codecpack_FfmpegBridge_nativeSetGainmapEnabled(JNIEnv*, jclass, jboolean enabled) {
    try {
    frameout::setGainmapEnabled(enabled == JNI_TRUE);
    } catch (...) {
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_frameout_codecpack_FfmpegBridge_nativeLastGainmap(JNIEnv* env, jclass, jstring path, jobject outGain, jfloatArray meta) {
    try {
    frameout::GainMap gain;
    if (!frameout::lastGain(pathFromJni(env, path), gain)) return -1;
    auto* destination = static_cast<std::uint8_t*>(env->GetDirectBufferAddress(outGain));
    const jlong capacity = env->GetDirectBufferCapacity(outGain);
    if (!destination || capacity < static_cast<jlong>(gain.data.size())) return -2;
    if (!meta || env->GetArrayLength(meta) < 8) return -3;
    std::copy(gain.data.begin(), gain.data.end(), destination);
    const jfloat values[8] = { static_cast<jfloat>(gain.width), static_cast<jfloat>(gain.height), gain.ratioMin[0], gain.ratioMin[1], gain.ratioMin[2], gain.ratioMax[0], gain.ratioMax[1], gain.ratioMax[2] };
    env->SetFloatArrayRegion(meta, 0, 8, values);
    return 0;
    } catch (...) {
    return -100;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_frameout_codecpack_FfmpegBridge_nativeSetSignalEnabled(JNIEnv*, jclass, jboolean enabled) {
    try {
    frameout::setSignalEnabled(enabled == JNI_TRUE);
    } catch (...) {
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_frameout_codecpack_FfmpegBridge_nativeLastSignal(JNIEnv* env, jclass, jstring path, jshortArray outRgb, jintArray meta) {
    try {
    if (!meta || env->GetArrayLength(meta) < 4) return -3;
    const std::string key = pathFromJni(env, path);
    jint values[4] = {0, 0, 0, 0};
    if (!frameout::signalInfo(key, values)) return -1;
    if (!outRgb) {
        env->SetIntArrayRegion(meta, 0, 4, values);
        return 0;
    }
    if (static_cast<jlong>(env->GetArrayLength(outRgb)) != static_cast<jlong>(values[0]) * values[1] * 3) return -2;
    frameout::SignalFrame signal;
    if (!frameout::takeSignal(key, signal)) return -1;
    env->SetShortArrayRegion(outRgb, 0, static_cast<jsize>(signal.rgb.size()), reinterpret_cast<const jshort*>(signal.rgb.data()));
    env->SetIntArrayRegion(meta, 0, 4, values);
    return 0;
    } catch (...) {
    return -100;
    }
}
