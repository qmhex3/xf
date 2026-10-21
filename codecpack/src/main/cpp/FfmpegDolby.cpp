#include "FfmpegDolby.h"
#include <algorithm>
#include <cmath>
#include <cstddef>
extern "C" {
#include <libavutil/dovi_meta.h>
#include <libavutil/frame.h>
#include <libavutil/rational.h>
}

namespace {
constexpr int kTable = 4096;
constexpr float kInv16 = 1.0f / 65535.0f;

const float kLmsToRgb[9] = {
    3.06441879f, -2.16597676f, 0.10155818f,
    -0.65612108f, 1.78554118f, -0.12943749f,
    0.01736321f, -0.04725154f, 1.03004253f
};

struct PqTables {
    float eotf[kTable + 1];
    std::uint16_t oetf[kTable + 1];
    PqTables() {
        const double m1 = 2610.0 / 16384.0, m2 = 2523.0 / 32.0;
        const double c1 = 3424.0 / 4096.0, c2 = 2413.0 / 128.0, c3 = 2392.0 / 128.0;
        for (int i = 0; i <= kTable; ++i) {
            const double v = static_cast<double>(i) / kTable;
            const double p = std::pow(v, 1.0 / m2);
            const double num = std::max(p - c1, 0.0);
            const double den = c2 - c3 * p;
            eotf[i] = den <= 0.0 ? 1.0f : static_cast<float>(std::pow(num / den, 1.0 / m1));
            const double s = static_cast<double>(i) / kTable;
            const double lin = s * s;
            const double q = std::pow(lin, m1);
            const double e = std::pow((c1 + c2 * q) / (1.0 + c3 * q), m2);
            oetf[i] = static_cast<std::uint16_t>(std::clamp(e, 0.0, 1.0) * 65535.0 + 0.5);
        }
    }
};

const PqTables& pq() {
    static const PqTables t;
    return t;
}

inline float pqDecode(const PqTables& t, float v) {
    const float x = std::clamp(v, 0.0f, 1.0f) * kTable;
    const int i = std::min(static_cast<int>(x), kTable - 1);
    const float f = x - static_cast<float>(i);
    return t.eotf[i] + (t.eotf[i + 1] - t.eotf[i]) * f;
}

inline std::uint16_t pqEncode(const PqTables& t, float linear) {
    const float s = std::sqrt(std::clamp(linear, 0.0f, 1.0f)) * kTable;
    const int i = std::min(static_cast<int>(s), kTable - 1);
    const float f = s - static_cast<float>(i);
    return static_cast<std::uint16_t>(t.oetf[i] + (static_cast<float>(t.oetf[i + 1]) - static_cast<float>(t.oetf[i])) * f + 0.5f);
}

struct Piece {
    bool mmr = false;
    int order = 1;
    float poly[3] = {0.0f, 0.0f, 0.0f};
    float constant = 0.0f;
    float coef[3][7] = {};
};

struct Curve {
    int count = 1;
    float pivots[AV_DOVI_MAX_PIECES + 1] = {};
    Piece pieces[AV_DOVI_MAX_PIECES];
    float low = 0.0f;
    float high = 1.0f;
};

float ratio(const AVRational& r) {
    return r.den != 0 ? static_cast<float>(av_q2d(r)) : 0.0f;
}

Curve buildCurve(const AVDOVIReshapingCurve& src, float pivotScale, float coefScale) {
    Curve c;
    c.count = std::clamp(static_cast<int>(src.num_pivots) - 1, 1, static_cast<int>(AV_DOVI_MAX_PIECES));
    for (int i = 0; i <= c.count; ++i) c.pivots[i] = static_cast<float>(src.pivots[i]) * pivotScale;
    c.low = c.pivots[0];
    c.high = c.pivots[c.count];
    for (int i = 0; i < c.count; ++i) {
        Piece& p = c.pieces[i];
        p.mmr = src.mapping_idc[i] == AV_DOVI_MAPPING_MMR;
        for (int k = 0; k < 3; ++k) p.poly[k] = k <= src.poly_order[i] ? coefScale * static_cast<float>(src.poly_coef[i][k]) : 0.0f;
        p.order = std::clamp(static_cast<int>(src.mmr_order[i]), 1, 3);
        p.constant = coefScale * static_cast<float>(src.mmr_constant[i]);
        for (int j = 0; j < 3; ++j) for (int k = 0; k < 7; ++k) p.coef[j][k] = coefScale * static_cast<float>(src.mmr_coef[i][j][k]);
    }
    return c;
}

inline float evaluate(const Curve& c, float s, float y, float u, float v) {
    int i = 0;
    while (i + 1 < c.count && s >= c.pivots[i + 1]) ++i;
    const Piece& p = c.pieces[i];
    float out;
    if (!p.mmr) {
        out = (p.poly[2] * s + p.poly[1]) * s + p.poly[0];
    } else {
        const float t[7] = {y, u, v, y * u, y * v, u * v, y * u * v};
        float power[7] = {t[0], t[1], t[2], t[3], t[4], t[5], t[6]};
        out = p.constant;
        for (int j = 0; j < p.order; ++j) {
            for (int k = 0; k < 7; ++k) {
                out += p.coef[j][k] * power[k];
                power[k] *= t[k];
            }
        }
    }
    return std::clamp(out, c.low, c.high);
}
}

const AVDOVIMetadata* dolbyMetadata(const AVFrame* frame) {
    if (!frame) return nullptr;
    const AVFrameSideData* sd = av_frame_get_side_data(frame, AV_FRAME_DATA_DOVI_METADATA);
    if (!sd || sd->size < sizeof(AVDOVIMetadata)) return nullptr;
    const auto* meta = reinterpret_cast<const AVDOVIMetadata*>(sd->data);
    const AVDOVIRpuDataHeader* header = av_dovi_get_header(meta);
    const AVDOVIDataMapping* mapping = av_dovi_get_mapping(meta);
    const AVDOVIColorMetadata* color = av_dovi_get_color(meta);
    if (!header || !mapping || !color) return nullptr;
    if (header->bl_bit_depth < 8 || header->bl_bit_depth > 16) return nullptr;
    return meta;
}

float dolbyPeakNits(const AVDOVIMetadata* meta) {
    if (!meta) return 0.0f;
    const AVDOVIColorMetadata* color = av_dovi_get_color(meta);
    if (!color || color->source_max_pq == 0) return 0.0f;
    return pqDecode(pq(), static_cast<float>(color->source_max_pq) / 4095.0f) * 10000.0f;
}

void dolbyToPq16(const std::uint16_t* yPlane, const std::uint16_t* uPlane, const std::uint16_t* vPlane, int width, int height, const AVDOVIMetadata* meta, std::uint16_t* rgba64) {
    const PqTables& t = pq();
    const AVDOVIRpuDataHeader* header = av_dovi_get_header(meta);
    const AVDOVIDataMapping* mapping = av_dovi_get_mapping(meta);
    const AVDOVIColorMetadata* color = av_dovi_get_color(meta);
    const int depth = header->bl_bit_depth;
    const float full = static_cast<float>((1 << depth) - 1);
    const float pivotScale = 1.0f / full;
    const float coefScale = 1.0f / static_cast<float>(1LL << std::min<int>(header->coef_log2_denom, 30));
    const Curve curves[3] = {
        buildCurve(mapping->curves[0], pivotScale, coefScale),
        buildCurve(mapping->curves[1], pivotScale, coefScale),
        buildCurve(mapping->curves[2], pivotScale, coefScale)
    };
    float m[9], r[9], off[3];
    for (int i = 0; i < 9; ++i) { m[i] = ratio(color->ycc_to_rgb_matrix[i]); r[i] = ratio(color->rgb_to_lms_matrix[i]); }
    const float offsetScale = static_cast<float>(1LL << depth) / full;
    for (int i = 0; i < 3; ++i) off[i] = ratio(color->ycc_to_rgb_offset[i]) * offsetScale;
    float lmsToRgb[9];
    for (int row = 0; row < 3; ++row) {
        for (int col = 0; col < 3; ++col) {
            lmsToRgb[row * 3 + col] = kLmsToRgb[row * 3] * r[col] + kLmsToRgb[row * 3 + 1] * r[3 + col] + kLmsToRgb[row * 3 + 2] * r[6 + col];
        }
    }
    const std::size_t pixels = static_cast<std::size_t>(width) * static_cast<std::size_t>(height);
    for (std::size_t i = 0; i < pixels; ++i) {
        const float y = yPlane[i] * kInv16;
        const float u = uPlane[i] * kInv16;
        const float v = vPlane[i] * kInv16;
        const float a = evaluate(curves[0], y, y, u, v) - off[0];
        const float b = evaluate(curves[1], u, y, u, v) - off[1];
        const float c = evaluate(curves[2], v, y, u, v) - off[2];
        const float l = pqDecode(t, m[0] * a + m[1] * b + m[2] * c);
        const float mm = pqDecode(t, m[3] * a + m[4] * b + m[5] * c);
        const float s = pqDecode(t, m[6] * a + m[7] * b + m[8] * c);
        std::uint16_t* o = rgba64 + i * 4;
        o[0] = pqEncode(t, lmsToRgb[0] * l + lmsToRgb[1] * mm + lmsToRgb[2] * s);
        o[1] = pqEncode(t, lmsToRgb[3] * l + lmsToRgb[4] * mm + lmsToRgb[5] * s);
        o[2] = pqEncode(t, lmsToRgb[6] * l + lmsToRgb[7] * mm + lmsToRgb[8] * s);
        o[3] = 65535;
    }
}
