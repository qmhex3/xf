#pragma once
#include "FfmpegDecoder.h"
#include "FfmpegSignal.h"
extern "C" {
#include <libavutil/frame.h>
}

namespace frameout_color {

struct CaptureRequest {
    const AVFrame* frame;
    const std::uint16_t* wide;
    int sw;
    int sh;
    int width;
    int height;
    int rotation;
    bool flipH;
    bool wideRgb;
    bool dolbyFrame;
    int dolbyProfile;
    bool matrixDecode;
    bool rangeDeclared;
};

inline bool captureExactSignal(const CaptureRequest& r, frameout::SignalFrame& out) {
    out = frameout::SignalFrame();
    SignalGate gate;
    gate.wide = r.wideRgb;
    gate.dolbyFrame = r.dolbyFrame;
    gate.dolbyProfile = r.dolbyProfile;
    gate.matrixDecode = r.matrixDecode;
    gate.resampled = r.sw != r.frame->width || r.sh != r.frame->height;
    gate.rangeDeclared = r.rangeDeclared;
    gate.colorspace = static_cast<int>(r.frame->colorspace);
    gate.primaries = static_cast<int>(r.frame->color_primaries);
    gate.transfer = static_cast<int>(r.frame->color_trc);
    if (!signalExact(gate)) return false;
    if (!packSignal(r.wide, r.sw, r.sh, r.rotation, r.flipH, gate.primaries, gate.transfer, out)) return false;
    if (out.width != r.width || out.height != r.height) out = frameout::SignalFrame();
    return out.valid;
}

}
