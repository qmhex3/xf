#pragma once
#include "FfmpegDecoder.h"
#include "FfmpegRotate.h"
#include <cstddef>
#include <cstdint>
#include <new>

namespace frameout_color {

constexpr int kCodePrimariesBt709 = 1;
constexpr int kCodePrimariesBt2020 = 9;
constexpr int kCodePrimariesDci = 11;
constexpr int kCodePrimariesP3 = 12;
constexpr int kCodeTransferPq = 16;
constexpr int kCodeTransferHlg = 18;
constexpr int kCodeMatrixBt709 = 1;
constexpr int kCodeMatrixBt2020Ncl = 9;

struct SignalGate {
    bool wide = false;
    bool dolbyFrame = false;
    int dolbyProfile = 0;
    bool matrixDecode = false;
    bool resampled = false;
    bool rangeDeclared = false;
    int colorspace = 2;
    int primaries = 2;
    int transfer = 2;
};

inline bool exactPrimaries(int code) {
    return code == kCodePrimariesBt709 || code == kCodePrimariesBt2020 || code == kCodePrimariesDci || code == kCodePrimariesP3;
}

inline bool exactTransfer(int code) {
    return code == kCodeTransferPq || code == kCodeTransferHlg;
}

inline bool exactMatrix(int code) {
    return code == kCodeMatrixBt709 || code == kCodeMatrixBt2020Ncl;
}

inline bool signalExact(const SignalGate& g) {
    if (!g.wide || g.dolbyFrame || g.dolbyProfile != 0 || g.matrixDecode || g.resampled || !g.rangeDeclared) return false;
    return exactPrimaries(g.primaries) && exactTransfer(g.transfer) && exactMatrix(g.colorspace);
}

inline bool packSignal(const std::uint16_t* rgba64, int srcW, int srcH, int rotation, bool flip, int primaries, int transfer, frameout::SignalFrame& out) {
    out = frameout::SignalFrame();
    if (!rgba64 || srcW <= 0 || srcH <= 0) return false;
    try {
        out.rgb.resize(static_cast<std::size_t>(srcW) * static_cast<std::size_t>(srcH) * 3u);
    } catch (const std::bad_alloc&) {
        std::vector<std::uint16_t>().swap(out.rgb);
        return false;
    }
    rotatePackRgb16(rgba64, srcW, srcH, rotation, flip, out.rgb.data());
    out.width = rotatedWidth(srcW, srcH, rotation);
    out.height = (rotation == 90 || rotation == 270) ? srcW : srcH;
    out.primaries = primaries;
    out.transfer = transfer;
    out.valid = true;
    return true;
}

}
