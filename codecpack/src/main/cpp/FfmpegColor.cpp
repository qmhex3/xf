#include "FfmpegColor.h"
#include "FfmpegAlpha.h"
#include "FfmpegDeinterlace.h"
#include "FfmpegDolby.h"
#include "FfmpegGamut.h"
#include "FfmpegHdr.h"
#include "FfmpegMatrix.h"
#include "FfmpegSignalCapture.h"
#include <algorithm>
#include <cmath>
#include <cstddef>
#include <cstdint>
#include <vector>
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/frame.h>
#include <libavutil/mastering_display_metadata.h>
#include <libavutil/pixdesc.h>
#include <libswscale/swscale.h>
}
#include "FfmpegColorProbe.h"
#include "FfmpegColorBuffers.h"

using namespace frameout_color;

bool convertFrame(AVFrame* frame, int width, int height, int rotation, bool flipH, SwsContext*& scaler, int& scalerWidth, int& scalerHeight, std::int64_t& scalerKey, std::vector<std::uint8_t>& rgba, std::uint8_t* external, frameout::GainMap* gain, float peakHintNits, int dolbyProfile, frameout::SignalFrame* exactSignal) {
    if (!frame || width <= 0 || height <= 0) return false;
    FrameCopy deinterlaced;
    if (AVFrame* copy = deinterlacedCopy(frame, deinterlaced)) frame = copy;
    if (gain) gain->valid = false;
    if (exactSignal) *exactSignal = frameout::SignalFrame();
    const bool rangeDeclared = frame->color_range != AVCOL_RANGE_UNSPECIFIED;
    const bool turned = rotation == 90 || rotation == 270;
    const bool transformed = rotation != 0 || flipH;
    const int sw = turned ? height : width;
    const int sh = turned ? width : height;
    if (frame->color_range == AVCOL_RANGE_UNSPECIFIED && !isJpegRangeFormat(frame->format) && !isGrayFormat(frame->format)) {
        const bool prior = scaler && ((scalerKey >> 24) & 3) == 1;
        if (prior || looksFullRange(frame)) frame->color_range = AVCOL_RANGE_JPEG;
    }
    const AVDOVIMetadata* dolbyMeta = dolbyProfile == 5 ? dolbyMetadata(frame) : nullptr;
    const bool matrixDecode = dolbyMeta == nullptr && needsMatrixDecode(frame);
    const std::int64_t baseKey = scalerKeyFor(frame) | (dolbyMeta ? kDolbyKey : 0) | (matrixDecode ? kMatrixKey : 0);
    const bool hdr = dolbyMeta != nullptr || isHdrTransfer(frame);
    const bool reuse = scaler && scalerWidth == sw && scalerHeight == sh && (scalerKey & ~kFallbackKey) == baseKey;
    if (!reuse) {
        releaseScaler(scaler);
        std::int64_t key = baseKey;
        bool signalDomain = false;
        if (dolbyMeta || matrixDecode) {
            scaler = makeScaler(frame, sw, sh, AV_PIX_FMT_YUV444P16LE);
            signalDomain = scaler != nullptr;
        } else if (hdr) {
            scaler = makeScaler(frame, sw, sh, AV_PIX_FMT_RGBA64LE);
        }
        if (!scaler) {
            if (hdr || matrixDecode) key |= kFallbackKey;
            scaler = makeScaler(frame, sw, sh, AV_PIX_FMT_RGBA);
        }
        if (!scaler) return false;
        if (signalDomain) keepSignalRange(scaler, frame);
        else applyColorspace(scaler, frame);
        scalerWidth = sw;
        scalerHeight = sh;
        scalerKey = key;
    }
    const bool scalerFallback = (scalerKey & kFallbackKey) != 0;
    const bool wide = hdr && !scalerFallback;
    const bool dolby = wide && dolbyMeta != nullptr;
    const bool signal = matrixDecode && !scalerFallback;
    const std::size_t total = static_cast<std::size_t>(width) * static_cast<std::size_t>(height) * 4u;
    std::uint8_t* destination = external;
    if (!destination) { rgba.resize(total); destination = rgba.data(); }
    static thread_local std::vector<std::uint8_t> scratch;
    static thread_local std::vector<std::uint8_t> gainPlane;
    static thread_local std::vector<std::uint8_t> gainTurned;
    static thread_local std::vector<std::uint16_t> wideBuffer;
    static thread_local std::vector<std::uint16_t> signalPlanes;
    ScratchGuard guard{scratch, gainPlane, gainTurned, wideBuffer, signalPlanes};
    std::uint8_t* target = !transformed ? destination : (scratch.resize(total), scratch.data());
    const std::size_t pixels = static_cast<std::size_t>(sw) * sh;
    if (wide || signal) {
        wideBuffer.resize(pixels * 4);
        if (dolby || signal) {
            signalPlanes.resize(pixels * 3);
            std::uint16_t* y = signalPlanes.data();
            std::uint16_t* u = y + pixels;
            std::uint16_t* v = u + pixels;
            std::uint8_t* planes[4] = { reinterpret_cast<std::uint8_t*>(y), reinterpret_cast<std::uint8_t*>(u), reinterpret_cast<std::uint8_t*>(v), nullptr };
            const int stride[4] = { sw * 2, sw * 2, sw * 2, 0 };
            if (sws_scale(scaler, frame->data, frame->linesize, 0, frame->height, planes, stride) != sh) return false;
            if (dolby) dolbyToPq16(y, u, v, sw, sh, dolbyMeta, wideBuffer.data());
            else specialMatrixToRgb16(y, u, v, pixels, static_cast<int>(frame->colorspace), sourceRange(frame) == 1, frame->color_trc == AVCOL_TRC_ARIB_STD_B67, bitDepthOf(frame->format), wideBuffer.data());
        } else {
            std::uint8_t* planes[4] = { reinterpret_cast<std::uint8_t*>(wideBuffer.data()), nullptr, nullptr, nullptr };
            const int stride[4] = { sw * 8, 0, 0, 0 };
            if (sws_scale(scaler, frame->data, frame->linesize, 0, frame->height, planes, stride) != sh) return false;
        }
        if (!wide) {
            narrowRgba64(wideBuffer.data(), pixels, target);
            if (primariesClass(frame) != 0) gamutToBt709(target, pixels, primariesClass(frame));
        }
    }
    if (wide) {
        if (exactSignal) captureExactSignal({frame, wideBuffer.data(), sw, sh, width, height, rotation, flipH, wide, dolby, dolbyProfile, matrixDecode, rangeDeclared}, *exactSignal);
        HdrParams params;
        params.hlg = !dolby && frame->color_trc == AVCOL_TRC_ARIB_STD_B67;
        params.primaries = dolby ? 1 : primariesClass(frame);
        const float dolbyPeak = dolby ? dolbyPeakNits(dolbyMeta) : 0.0f;
        params.peakNits = peakFor(frame, dolbyPeak > 0.0f ? dolbyPeak : peakHintNits);
        const bool wantGain = gain != nullptr;
        if (wantGain) gainPlane.resize(pixels * 8);
        toneMapHdr16(wideBuffer.data(), sw, sh, params, target, wantGain ? reinterpret_cast<std::uint16_t*>(gainPlane.data()) : nullptr);
        if (wantGain) {
            if (transformed) {
                gainTurned.resize(pixels * 8);
                rotateTyped(reinterpret_cast<const std::uint64_t*>(gainPlane.data()), sw, sh, rotation, flipH, reinterpret_cast<std::uint64_t*>(gainTurned.data()));
                packGain(reinterpret_cast<const std::uint16_t*>(gainTurned.data()), width, height, *gain);
            } else {
                packGain(reinterpret_cast<const std::uint16_t*>(gainPlane.data()), width, height, *gain);
            }
        }
    } else if (!signal) {
        if (!scaleInto(frame, scaler, sw, sh, target)) return false;
        if (hdr) toneMapHdrToSdr(target, sw, sh, frame->color_trc == AVCOL_TRC_ARIB_STD_B67, peakFor(frame, peakHintNits), primariesClass(frame));
        else if (primariesClass(frame) != 0) gamutToBt709(target, pixels, primariesClass(frame));
    }
    if (hasAlphaChannel(frame->format)) flattenAlpha(target, pixels);
    if (transformed) rotateInto(target, sw, sh, rotation, flipH, destination);
    return true;
}

void applyCodecColorDefaults(AVFrame* frame, int codecId) {
    if (!frame || !isFullRangeCodec(codecId)) return;
    if (frame->color_range == AVCOL_RANGE_UNSPECIFIED) frame->color_range = AVCOL_RANGE_JPEG;
    if (frame->colorspace == AVCOL_SPC_UNSPECIFIED && codecId != AV_CODEC_ID_JPEG2000) frame->colorspace = AVCOL_SPC_SMPTE170M;
}

void releaseScaler(SwsContext*& scaler) {
    if (scaler) sws_freeContext(scaler);
    scaler = nullptr;
}
