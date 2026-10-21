#pragma once
#include "FfmpegColor.h"
#include "FfmpegAlpha.h"
#include "FfmpegDeinterlace.h"
#include "FfmpegDolby.h"
#include "FfmpegGamut.h"
#include "FfmpegHdr.h"
#include "FfmpegMatrix.h"
#include <algorithm>
#include <cmath>
#include <cstddef>
#include <cstdint>
#include <vector>
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/opt.h>
#include <libavutil/frame.h>
#include <libavutil/mastering_display_metadata.h>
#include <libavutil/pixdesc.h>
#include <libswscale/swscale.h>
}

namespace frameout_color {
inline bool isJpegRangeFormat(int format) {
    switch (format) {
        case AV_PIX_FMT_YUVJ420P:
        case AV_PIX_FMT_YUVJ422P:
        case AV_PIX_FMT_YUVJ444P:
        case AV_PIX_FMT_YUVJ440P:
        case AV_PIX_FMT_YUVJ411P:
            return true;
        default:
            return false;
    }
}

inline bool isHdrTransfer(const AVFrame* f) {
    if (static_cast<int>(f->colorspace) == kSpaceIctcp) return true;
    if (f->color_trc == AVCOL_TRC_SMPTE2084 || f->color_trc == AVCOL_TRC_ARIB_STD_B67) return true;
    if (f->color_trc != AVCOL_TRC_UNSPECIFIED) return false;
    return av_frame_get_side_data(f, AV_FRAME_DATA_MASTERING_DISPLAY_METADATA) != nullptr || av_frame_get_side_data(f, AV_FRAME_DATA_CONTENT_LIGHT_LEVEL) != nullptr;
}

inline int primariesClass(const AVFrame* f) {
    switch (f->color_primaries) {
        case AVCOL_PRI_BT709: return 0;
        case AVCOL_PRI_SMPTE432: return 2;
        case AVCOL_PRI_SMPTE431: return 3;
        case AVCOL_PRI_BT2020: return 1;
        default:
            if (f->colorspace == AVCOL_SPC_BT2020_NCL || f->colorspace == AVCOL_SPC_BT2020_CL) return 1;
            if (f->colorspace == AVCOL_SPC_BT709) return 0;
            return isHdrTransfer(f) ? 1 : 0;
    }
}

inline int swsColorspace(const AVFrame* f) {
    switch (f->colorspace) {
        case AVCOL_SPC_BT709: return SWS_CS_ITU709;
        case AVCOL_SPC_BT470BG:
        case AVCOL_SPC_SMPTE170M: return SWS_CS_ITU601;
        case AVCOL_SPC_SMPTE240M: return SWS_CS_SMPTE240M;
        case AVCOL_SPC_FCC: return SWS_CS_FCC;
        case AVCOL_SPC_BT2020_NCL:
        case AVCOL_SPC_BT2020_CL: return SWS_CS_BT2020;
        default:
            if (isHdrTransfer(f) && primariesClass(f) == 1) return SWS_CS_BT2020;
            return (f->height > 576 || f->width >= 1280) ? SWS_CS_ITU709 : SWS_CS_ITU601;
    }
}

inline bool hasAlphaChannel(int format) {
    const AVPixFmtDescriptor* d = av_pix_fmt_desc_get(static_cast<AVPixelFormat>(format));
    return d && (d->flags & AV_PIX_FMT_FLAG_ALPHA) != 0;
}

inline bool needsMatrixDecode(const AVFrame* f) {
    if (!isSpecialMatrix(static_cast<int>(f->colorspace))) return false;
    const AVPixFmtDescriptor* d = av_pix_fmt_desc_get(static_cast<AVPixelFormat>(f->format));
    if (!d || d->nb_components < 3) return false;
    return (d->flags & (AV_PIX_FMT_FLAG_RGB | AV_PIX_FMT_FLAG_PAL | AV_PIX_FMT_FLAG_ALPHA)) == 0;
}

inline int bitDepthOf(int format) {
    const AVPixFmtDescriptor* d = av_pix_fmt_desc_get(static_cast<AVPixelFormat>(format));
    return d ? d->comp[0].depth : 8;
}

inline bool isFullRangeCodec(int codecId) {
    switch (codecId) {
        case AV_CODEC_ID_MJPEG:
        case AV_CODEC_ID_MJPEGB:
        case AV_CODEC_ID_LJPEG:
        case AV_CODEC_ID_JPEGLS:
        case AV_CODEC_ID_SP5X:
        case AV_CODEC_ID_AMV:
        case AV_CODEC_ID_JPEG2000:
            return true;
        default:
            return false;
    }
}

inline bool isGrayFormat(int format) {
    const AVPixFmtDescriptor* d = av_pix_fmt_desc_get(static_cast<AVPixelFormat>(format));
    if (!d || d->nb_components > 2) return false;
    return (d->flags & (AV_PIX_FMT_FLAG_RGB | AV_PIX_FMT_FLAG_PAL)) == 0;
}

inline int sourceRange(const AVFrame* f) {
    if (isJpegRangeFormat(f->format)) return 1;
    if (f->color_range == AVCOL_RANGE_JPEG) return 1;
    if (f->color_range == AVCOL_RANGE_MPEG) return 0;
    return (isJpegRangeFormat(f->format) || isGrayFormat(f->format)) ? 1 : 0;
}

inline bool looksFullRange(const AVFrame* f) {
    const AVPixFmtDescriptor* d = av_pix_fmt_desc_get(static_cast<AVPixelFormat>(f->format));
    if (!d || d->nb_components < 3 || !f->data[0] || f->width < 48 || f->height < 48) return false;
    if ((d->flags & (AV_PIX_FMT_FLAG_RGB | AV_PIX_FMT_FLAG_PAL | AV_PIX_FMT_FLAG_BE | AV_PIX_FMT_FLAG_HWACCEL | AV_PIX_FMT_FLAG_ALPHA)) != 0) return false;
    if ((d->flags & AV_PIX_FMT_FLAG_PLANAR) == 0) return false;
    const int depth = d->comp[0].depth;
    if (depth != 8 && depth != 10) return false;
    const int low = depth == 8 ? 12 : 48;
    const int high = depth == 8 ? 243 : 972;
    const int grid = 48;
    int outside = 0;
    for (int gy = 0; gy < grid; ++gy) {
        const int y = gy * (f->height - 1) / (grid - 1);
        const std::uint8_t* row = f->data[0] + static_cast<std::ptrdiff_t>(y) * f->linesize[0];
        for (int gx = 0; gx < grid; ++gx) {
            const int x = gx * (f->width - 1) / (grid - 1);
            int v;
            if (depth == 8) {
                v = row[x];
            } else {
                v = row[x * 2] | (row[x * 2 + 1] << 8);
            }
            if (v <= low || v >= high) ++outside;
        }
    }
    return outside * 1000 > grid * grid * 15;
}

inline void keepSignalRange(SwsContext* scaler, const AVFrame* f) {
    const int* table = sws_getCoefficients(SWS_CS_DEFAULT);
    const int range = sourceRange(f);
    sws_setColorspaceDetails(scaler, table, range, table, range, 0, 1 << 16, 1 << 16);
}

inline void applyColorspace(SwsContext* scaler, const AVFrame* f) {
    if (f->colorspace == AVCOL_SPC_RGB) return;
    const int* inverse = sws_getCoefficients(swsColorspace(f));
    const int* table = sws_getCoefficients(SWS_CS_DEFAULT);
    sws_setColorspaceDetails(scaler, inverse, sourceRange(f), table, 1, 0, 1 << 16, 1 << 16);
}

constexpr std::int64_t kFallbackKey = static_cast<std::int64_t>(1) << 30;
constexpr std::int64_t kDolbyKey = static_cast<std::int64_t>(1) << 29;
constexpr std::int64_t kMatrixKey = static_cast<std::int64_t>(1) << 31;
constexpr int kScaleFlags = SWS_BILINEAR | SWS_ACCURATE_RND | SWS_FULL_CHR_H_INT;

inline bool chromaPosition(const AVFrame* f, int& h, int& v) {
    switch (f->chroma_location) {
        case AVCHROMA_LOC_LEFT: h = 0; v = 128; return true;
        case AVCHROMA_LOC_CENTER: h = 128; v = 128; return true;
        case AVCHROMA_LOC_TOPLEFT: h = 0; v = 0; return true;
        case AVCHROMA_LOC_TOP: h = 128; v = 0; return true;
        case AVCHROMA_LOC_BOTTOMLEFT: h = 0; v = 256; return true;
        case AVCHROMA_LOC_BOTTOM: h = 128; v = 256; return true;
        default: break;
    }
    const int format = f->format;
    if (format == AV_PIX_FMT_YUVJ420P || format == AV_PIX_FMT_YUVJ422P) { h = 128; v = 128; return true; }
    return false;
}

inline SwsContext* makeScaler(const AVFrame* f, int dstW, int dstH, AVPixelFormat dst) {
    SwsContext* ctx = sws_alloc_context();
    if (!ctx) return nullptr;
    av_opt_set_int(ctx, "srcw", f->width, 0);
    av_opt_set_int(ctx, "srch", f->height, 0);
    av_opt_set_int(ctx, "src_format", f->format, 0);
    av_opt_set_int(ctx, "dstw", dstW, 0);
    av_opt_set_int(ctx, "dsth", dstH, 0);
    av_opt_set_int(ctx, "dst_format", dst, 0);
    av_opt_set_int(ctx, "sws_flags", kScaleFlags, 0);
    int h = 0;
    int v = 0;
    if (chromaPosition(f, h, v)) {
        av_opt_set_int(ctx, "src_h_chr_pos", h, 0);
        av_opt_set_int(ctx, "src_v_chr_pos", v, 0);
    }
    if (sws_init_context(ctx, nullptr, nullptr) < 0) {
        sws_freeContext(ctx);
        return nullptr;
    }
    return ctx;
}

inline std::int64_t scalerKeyFor(const AVFrame* f) {
    const std::int64_t format = f->format & 0xFFFF;
    const std::int64_t space = static_cast<std::int64_t>(f->colorspace & 0xFF) << 16;
    const std::int64_t range = static_cast<std::int64_t>(sourceRange(f) & 0x3) << 24;
    const std::int64_t hdr = isHdrTransfer(f) ? (static_cast<std::int64_t>(1) << 26) : 0;
    const std::int64_t primaries = static_cast<std::int64_t>(primariesClass(f) & 0x3) << 27;
    const std::int64_t srcW = static_cast<std::int64_t>(f->width & 0x7FFFFFFF) << 32;
    const std::int64_t srcH = static_cast<std::int64_t>(f->height & 0x7FFF) << 48;
    return format | space | range | hdr | primaries | srcW | srcH;
}
}
