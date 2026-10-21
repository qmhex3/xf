#include "FfmpegDecoder.h"
#include "FfmpegError.h"
#include <jni.h>
#include <android/bitmap.h>
#include <string>
#include <cstdint>
#include <climits>
#include <vector>
#include <algorithm>
#include "FrameoutJni.h"

extern "C" JNIEXPORT jint JNICALL
Java_com_frameout_codecpack_FfmpegBridge_nativeLastError(JNIEnv* env, jclass, jstring path) {
    try {
        return static_cast<jint>(frameout::lastError(pathFromJni(env, path)));
    } catch (...) {
        return static_cast<jint>(frameout::FrameOutError::Internal);
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_frameout_codecpack_FfmpegBridge_nativeInit(JNIEnv*, jclass) {
    try {
#if FRAMEOUT_HAS_FFMPEG
    return 0;
#else
    return -2;
#endif
    } catch (...) {
    return -100;
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_frameout_codecpack_FfmpegBridge_nativeProbe(JNIEnv* env, jclass, jstring path) {
    try {
    std::string result;
#if FRAMEOUT_HAS_FFMPEG
    if (frameout::probe(pathFromJni(env, path), result)) return env->NewStringUTF(result.c_str());
#endif
    return env->NewStringUTF("unavailable");
    } catch (...) {
    return env->NewStringUTF("unavailable");
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_frameout_codecpack_FfmpegBridge_nativeFrameCount(JNIEnv* env, jclass, jstring path) {
    try {
    return frameout::frameCount(pathFromJni(env, path));
    } catch (...) {
    return failed(env, path);
    }
}

static jint decodeInto(JNIEnv* env, jstring path, jint index, jint hintIndex, jlong hintPtsUs, jobject outRgba, jint width, jint height, jlongArray ptsOut, jbooleanArray keyframeOut, jintArray nearestKeyframeIndexOut, jlongArray nearestKeyframePtsOut) {
    if (width <= 0 || height <= 0) return -1;
    auto* destination = static_cast<std::uint8_t*>(env->GetDirectBufferAddress(outRgba));
    const jlong capacity = env->GetDirectBufferCapacity(outRgba);
    if (!destination || capacity < static_cast<jlong>(width) * height * 4) return -2;
    frameout::DecodedFrame out;
    out.width = width;
    out.height = height;
    out.external = destination;
    const int rc = frameout::decodeFrame(pathFromJni(env, path), index, out, hintIndex, static_cast<std::int64_t>(hintPtsUs));
    if (ptsOut && env->GetArrayLength(ptsOut) > 0) {
        const jlong pts = static_cast<jlong>(out.ptsUs);
        env->SetLongArrayRegion(ptsOut, 0, 1, &pts);
    }
    if (keyframeOut && env->GetArrayLength(keyframeOut) > 0) {
        const jboolean keyframe = out.keyframe ? JNI_TRUE : JNI_FALSE;
        env->SetBooleanArrayRegion(keyframeOut, 0, 1, &keyframe);
    }
    if (nearestKeyframeIndexOut && env->GetArrayLength(nearestKeyframeIndexOut) > 0) {
        const jint nearest = static_cast<jint>(out.nearestKeyframeIndex);
        env->SetIntArrayRegion(nearestKeyframeIndexOut, 0, 1, &nearest);
    }
    if (nearestKeyframePtsOut && env->GetArrayLength(nearestKeyframePtsOut) > 0) {
        const jlong nearestPts = static_cast<jlong>(out.nearestKeyframePtsUs);
        env->SetLongArrayRegion(nearestKeyframePtsOut, 0, 1, &nearestPts);
    }
    return rc;
}

static void storeMeta(JNIEnv* env, const frameout::DecodedFrame& out, jlongArray ptsOut, jbooleanArray keyframeOut, jintArray nearestKeyframeIndexOut, jlongArray nearestKeyframePtsOut) {
    if (ptsOut && env->GetArrayLength(ptsOut) > 0) {
        const jlong pts = static_cast<jlong>(out.ptsUs);
        env->SetLongArrayRegion(ptsOut, 0, 1, &pts);
    }
    if (keyframeOut && env->GetArrayLength(keyframeOut) > 0) {
        const jboolean keyframe = out.keyframe ? JNI_TRUE : JNI_FALSE;
        env->SetBooleanArrayRegion(keyframeOut, 0, 1, &keyframe);
    }
    if (nearestKeyframeIndexOut && env->GetArrayLength(nearestKeyframeIndexOut) > 0) {
        const jint nearest = static_cast<jint>(out.nearestKeyframeIndex);
        env->SetIntArrayRegion(nearestKeyframeIndexOut, 0, 1, &nearest);
    }
    if (nearestKeyframePtsOut && env->GetArrayLength(nearestKeyframePtsOut) > 0) {
        const jlong nearestPts = static_cast<jlong>(out.nearestKeyframePtsUs);
        env->SetLongArrayRegion(nearestKeyframePtsOut, 0, 1, &nearestPts);
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_frameout_codecpack_FfmpegBridge_nativeDecodeFrameToBitmap(JNIEnv* env, jclass, jstring path, jint index, jint hintIndex, jlong hintPtsUs, jobject bitmap, jlongArray ptsOut, jbooleanArray keyframeOut, jintArray nearestKeyframeIndexOut, jlongArray nearestKeyframePtsOut) {
    try {
    AndroidBitmapInfo info;
    if (!bitmap || AndroidBitmap_getInfo(env, bitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS) return -102;
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888 || info.width == 0 || info.height == 0 || info.stride != info.width * 4) return -102;
    void* pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS || !pixels) return -102;
    frameout::DecodedFrame out;
    out.width = static_cast<int>(info.width);
    out.height = static_cast<int>(info.height);
    out.external = static_cast<std::uint8_t*>(pixels);
    int rc;
    try {
        rc = frameout::decodeFrame(pathFromJni(env, path), index, out, hintIndex, static_cast<std::int64_t>(hintPtsUs));
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        throw;
    }
    AndroidBitmap_unlockPixels(env, bitmap);
    storeMeta(env, out, ptsOut, keyframeOut, nearestKeyframeIndexOut, nearestKeyframePtsOut);
    return rc;
    } catch (...) {
    return failed(env, path);
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_frameout_codecpack_FfmpegBridge_nativeDecodeFrame(JNIEnv* env, jclass, jstring path, jint index, jobject outRgba, jint width, jint height, jlongArray ptsOut, jbooleanArray keyframeOut, jintArray nearestKeyframeIndexOut, jlongArray nearestKeyframePtsOut) {
    try {
    return decodeInto(env, path, index, -1, -1, outRgba, width, height, ptsOut, keyframeOut, nearestKeyframeIndexOut, nearestKeyframePtsOut);
    } catch (...) {
    return failed(env, path);
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_frameout_codecpack_FfmpegBridge_nativeDecodeFrameFrom(JNIEnv* env, jclass, jstring path, jint index, jint hintIndex, jlong hintPtsUs, jobject outRgba, jint width, jint height, jlongArray ptsOut, jbooleanArray keyframeOut, jintArray nearestKeyframeIndexOut, jlongArray nearestKeyframePtsOut) {
    try {
    return decodeInto(env, path, index, hintIndex, hintPtsUs, outRgba, width, height, ptsOut, keyframeOut, nearestKeyframeIndexOut, nearestKeyframePtsOut);
    } catch (...) {
    return failed(env, path);
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_frameout_codecpack_FfmpegBridge_nativeSeekSessionToKeyframe(JNIEnv* env, jclass, jstring path, jint keyframeIndex, jlong keyframePtsUs) {
    try {
    return frameout::seekSessionToKeyframe(pathFromJni(env, path), keyframeIndex, static_cast<std::int64_t>(keyframePtsUs));
    } catch (...) {
    return failed(env, path);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_frameout_codecpack_FfmpegBridge_nativeCloseSession(JNIEnv* env, jclass, jstring path) {
    try {
    frameout::closeDecoderSession(pathFromJni(env, path));
    } catch (...) {
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_frameout_codecpack_FfmpegBridge_nativeCancelSession(JNIEnv* env, jclass, jstring path) {
    try {
    frameout::cancelDecoderSession(pathFromJni(env, path));
    } catch (...) {
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_frameout_codecpack_FfmpegBridge_nativeSetSessionTable(JNIEnv* env, jclass, jstring path, jlongArray values) {
    try {
    if (!values) return -1;
    const jsize size = env->GetArrayLength(values);
    if (size <= 0 || size % 4 != 0) return -1;
    std::vector<jlong> raw(static_cast<std::size_t>(size));
    env->GetLongArrayRegion(values, 0, size, raw.data());
    std::vector<frameout::IndexedFrame> table;
    table.reserve(raw.size() / 4);
    for (std::size_t i = 0; i < raw.size(); i += 4) {
        frameout::IndexedFrame frame;
        frame.ptsUs = static_cast<std::int64_t>(raw[i]);
        frame.keyframe = raw[i + 1] != 0;
        frame.nearestKeyframeIndex = static_cast<int>(raw[i + 2]);
        frame.nearestKeyframePtsUs = static_cast<std::int64_t>(raw[i + 3]);
        table.push_back(frame);
    }
    return frameout::installIndexTable(pathFromJni(env, path), std::move(table));
    } catch (...) {
    return failed(env, path);
    }
}
