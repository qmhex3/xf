#include "FfmpegDeinterlace.h"
#if FRAMEOUT_HAS_FFMPEG
#include <cstddef>
#include <cstdint>
#include <cstdlib>
extern "C" {
#include <libavutil/frame.h>
#include <libavutil/pixdesc.h>
}

namespace {
constexpr int kLumaThreshold = 12;
constexpr int kChromaThreshold = 6;

struct Plane {
    std::uint8_t* base;
    std::ptrdiff_t stride;
    int step;
    int width;
    int height;
    int threshold;
    bool wide;
    bool bigEndian;
};

inline int load(const Plane& p, const std::uint8_t* s) {
    if (!p.wide) return s[0];
    return p.bigEndian ? ((s[0] << 8) | s[1]) : (s[0] | (s[1] << 8));
}

inline void store(const Plane& p, std::uint8_t* s, int v) {
    if (!p.wide) {
        s[0] = static_cast<std::uint8_t>(v);
    } else if (p.bigEndian) {
        s[0] = static_cast<std::uint8_t>(v >> 8);
        s[1] = static_cast<std::uint8_t>(v & 0xFF);
    } else {
        s[0] = static_cast<std::uint8_t>(v & 0xFF);
        s[1] = static_cast<std::uint8_t>(v >> 8);
    }
}

void copyRow(const Plane& p, std::uint8_t* row, const std::uint8_t* from) {
    for (int x = 0; x < p.width; ++x) {
        const std::ptrdiff_t o = static_cast<std::ptrdiff_t>(x) * p.step;
        store(p, row + o, load(p, from + o));
    }
}

void repairRow(const Plane& p, std::uint8_t* row, const std::uint8_t* above, const std::uint8_t* below) {
    for (int x = 0; x < p.width; ++x) {
        const std::ptrdiff_t o = static_cast<std::ptrdiff_t>(x) * p.step;
        const int cur = load(p, row + o);
        const int up = load(p, above + o);
        const int down = load(p, below + o);
        const int a = cur - up;
        const int b = cur - down;
        const bool comb = (a > p.threshold && b > p.threshold) || (a < -p.threshold && b < -p.threshold);
        if (comb) store(p, row + o, (up + down + 1) >> 1);
    }
}

void filterPlane(const Plane& p, bool topFieldFirst) {
    if (p.height < 3 || p.width <= 0) return;
    for (int y = topFieldFirst ? 1 : 0; y < p.height; y += 2) {
        std::uint8_t* row = p.base + static_cast<std::ptrdiff_t>(y) * p.stride;
        if (y == 0) copyRow(p, row, row + p.stride);
        else if (y == p.height - 1) copyRow(p, row, row - p.stride);
        else repairRow(p, row, row - p.stride, row + p.stride);
    }
}

int ceilShift(int value, int shift) {
    return (value + (1 << shift) - 1) >> shift;
}
}

bool canDeinterlaceFormat(int format) {
    const AVPixFmtDescriptor* d = av_pix_fmt_desc_get(static_cast<AVPixelFormat>(format));
    if (!d || d->nb_components < 1) return false;
    return (d->flags & (AV_PIX_FMT_FLAG_RGB | AV_PIX_FMT_FLAG_PAL | AV_PIX_FMT_FLAG_HWACCEL | AV_PIX_FMT_FLAG_BITSTREAM | AV_PIX_FMT_FLAG_BAYER)) == 0;
}

void deinterlaceFrame(AVFrame* frame, bool topFieldFirst) {
    if (!frame || !canDeinterlaceFormat(frame->format)) return;
    const AVPixFmtDescriptor* d = av_pix_fmt_desc_get(static_cast<AVPixelFormat>(frame->format));
    const bool bigEndian = (d->flags & AV_PIX_FMT_FLAG_BE) != 0;
    const bool hasAlpha = (d->flags & AV_PIX_FMT_FLAG_ALPHA) != 0;
    for (int i = 0; i < d->nb_components; ++i) {
        if (hasAlpha && i == d->nb_components - 1) continue;
        const AVComponentDescriptor& c = d->comp[i];
        if (c.plane < 0 || c.plane >= AV_NUM_DATA_POINTERS || !frame->data[c.plane] || frame->linesize[c.plane] <= 0) continue;
        const bool chroma = i > 0 && d->nb_components > 2;
        const int width = chroma ? ceilShift(frame->width, d->log2_chroma_w) : frame->width;
        const int height = chroma ? ceilShift(frame->height, d->log2_chroma_h) : frame->height;
        const int bits = c.depth + c.shift;
        Plane p;
        p.base = frame->data[c.plane] + c.offset;
        p.stride = frame->linesize[c.plane];
        p.step = c.step;
        p.width = width;
        p.height = height;
        p.wide = c.depth > 8;
        p.bigEndian = bigEndian;
        p.threshold = (chroma ? kChromaThreshold : kLumaThreshold) << (c.depth > 8 ? bits - 8 : 0);
        filterPlane(p, topFieldFirst);
    }
}
#endif
