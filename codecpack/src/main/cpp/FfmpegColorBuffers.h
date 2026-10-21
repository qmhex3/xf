#pragma once
#include "FfmpegColor.h"
#include "FfmpegAlpha.h"
#include "FfmpegDeinterlace.h"
#include "FfmpegDolby.h"
#include "FfmpegGamut.h"
#include "FfmpegGainPack.h"
#include "FfmpegHdr.h"
#include "FfmpegRotate.h"
#include "FfmpegMatrix.h"
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

namespace frameout_color {
constexpr std::size_t kScratchKeepBytes = 256u * 1024u * 1024u;
constexpr std::size_t kScratchSlackBytes = 32u * 1024u * 1024u;

template <typename T>
inline std::size_t heldBytes(const std::vector<T>& buffer) { return buffer.capacity() * sizeof(T); }

template <typename T>
inline std::size_t usedBytes(const std::vector<T>& buffer) { return buffer.size() * sizeof(T); }

struct ScratchGuard {
    std::vector<std::uint8_t>& scratch;
    std::vector<std::uint8_t>& gainPlane;
    std::vector<std::uint8_t>& gainTurned;
    std::vector<std::uint16_t>& wideBuffer;
    std::vector<std::uint16_t>& signalPlanes;
    ~ScratchGuard() {
        const std::size_t held = heldBytes(scratch) + heldBytes(gainPlane) + heldBytes(gainTurned) + heldBytes(wideBuffer) + heldBytes(signalPlanes);
        const std::size_t used = usedBytes(scratch) + usedBytes(gainPlane) + usedBytes(gainTurned) + usedBytes(wideBuffer) + usedBytes(signalPlanes);
        if (held <= kScratchKeepBytes && held <= used * 4u + kScratchSlackBytes) return;
        std::vector<std::uint8_t>().swap(scratch);
        std::vector<std::uint8_t>().swap(gainPlane);
        std::vector<std::uint8_t>().swap(gainTurned);
        std::vector<std::uint16_t>().swap(wideBuffer);
        std::vector<std::uint16_t>().swap(signalPlanes);
    }
};

inline float peakFor(const AVFrame* f, float hint) {
    if (f->color_trc == AVCOL_TRC_ARIB_STD_B67) return 1000.0f;
    float nits = 0.0f;
    if (const AVFrameSideData* d = av_frame_get_side_data(f, AV_FRAME_DATA_CONTENT_LIGHT_LEVEL)) {
        if (d->size >= static_cast<int>(sizeof(AVContentLightMetadata))) {
            nits = static_cast<float>(reinterpret_cast<const AVContentLightMetadata*>(d->data)->MaxCLL);
        }
    }
    if (nits <= 0.0f) {
        if (const AVFrameSideData* d = av_frame_get_side_data(f, AV_FRAME_DATA_MASTERING_DISPLAY_METADATA)) {
            if (d->size >= static_cast<int>(sizeof(AVMasteringDisplayMetadata))) {
                const auto* m = reinterpret_cast<const AVMasteringDisplayMetadata*>(d->data);
                if (m->has_luminance && m->max_luminance.den > 0) nits = static_cast<float>(m->max_luminance.num) / static_cast<float>(m->max_luminance.den);
            }
        }
    }
    if (nits <= 0.0f) nits = hint;
    if (nits <= 0.0f) return 1000.0f;
    return std::clamp(nits, 400.0f, 10000.0f);
}

inline void rotateInto(const std::uint8_t* src, int srcW, int srcH, int rotation, bool flip, std::uint8_t* dst) {
    rotateTyped(reinterpret_cast<const std::uint32_t*>(src), srcW, srcH, rotation, flip, reinterpret_cast<std::uint32_t*>(dst));
}

inline bool isInterlaced(const AVFrame* f) {
#ifdef AV_FRAME_FLAG_INTERLACED
    return (f->flags & AV_FRAME_FLAG_INTERLACED) != 0;
#else
    return f->interlaced_frame != 0;
#endif
}

inline bool topFieldFirst(const AVFrame* f) {
#ifdef AV_FRAME_FLAG_TOP_FIELD_FIRST
    return (f->flags & AV_FRAME_FLAG_TOP_FIELD_FIRST) != 0;
#else
    return f->top_field_first != 0;
#endif
}

struct FrameCopy {
    AVFrame* frame = nullptr;
    ~FrameCopy() { if (frame) av_frame_free(&frame); }
};

inline AVFrame* deinterlacedCopy(const AVFrame* source, FrameCopy& holder) {
    if (!isInterlaced(source) || !canDeinterlaceFormat(source->format)) return nullptr;
    AVFrame* copy = av_frame_clone(source);
    if (!copy) return nullptr;
    if (av_frame_make_writable(copy) < 0) {
        av_frame_free(&copy);
        return nullptr;
    }
    deinterlaceFrame(copy, topFieldFirst(source));
    holder.frame = copy;
    return copy;
}

inline bool scaleInto(AVFrame* frame, SwsContext* scaler, int w, int h, std::uint8_t* dst) {
    std::uint8_t* planes[4] = { dst, nullptr, nullptr, nullptr };
    const int stride[4] = { w * 4, 0, 0, 0 };
    return sws_scale(scaler, frame->data, frame->linesize, 0, frame->height, planes, stride) == h;
}
}
