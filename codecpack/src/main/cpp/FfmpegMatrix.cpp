#include "FfmpegMatrix.h"
#include <algorithm>
#include <cmath>

namespace {
constexpr float kPqM1 = 0.1593017578125f;
constexpr float kPqM2 = 78.84375f;
constexpr float kPqC1 = 0.8359375f;
constexpr float kPqC2 = 18.8515625f;
constexpr float kPqC3 = 18.6875f;
constexpr float kHlgA = 0.17883277f;
constexpr float kHlgB = 0.28466892f;
constexpr float kHlgC = 0.55991073f;
constexpr float kBtAlpha = 1.09929682680944f;
constexpr float kBtBeta = 0.018053968510807f;

float pqEotf(float v) {
    const float p = std::pow(std::clamp(v, 0.0f, 1.0f), 1.0f / kPqM2);
    const float den = kPqC2 - kPqC3 * p;
    if (den <= 0.0f) return 1.0f;
    return std::pow(std::max(p - kPqC1, 0.0f) / den, 1.0f / kPqM1);
}

float pqOetf(float l) {
    const float p = std::pow(std::clamp(l, 0.0f, 1.0f), kPqM1);
    return std::pow((kPqC1 + kPqC2 * p) / (1.0f + kPqC3 * p), kPqM2);
}

float hlgEotf(float e) {
    e = std::clamp(e, 0.0f, 1.0f);
    return e <= 0.5f ? (e * e) / 3.0f : (std::exp((e - kHlgC) / kHlgA) + kHlgB) / 12.0f;
}

float hlgOetf(float l) {
    l = std::clamp(l, 0.0f, 1.0f);
    return l <= 1.0f / 12.0f ? std::sqrt(3.0f * l) : kHlgA * std::log(12.0f * l - kHlgB) + kHlgC;
}

float bt2020Oetf(float l) {
    l = std::max(l, 0.0f);
    return l < kBtBeta ? 4.5f * l : kBtAlpha * std::pow(l, 0.45f) - (kBtAlpha - 1.0f);
}

float bt2020Eotf(float v) {
    v = std::max(v, 0.0f);
    return v < 4.5f * kBtBeta ? v / 4.5f : std::pow((v + kBtAlpha - 1.0f) / kBtAlpha, 1.0f / 0.45f);
}

std::uint16_t pack(float v) {
    return static_cast<std::uint16_t>(std::clamp(v, 0.0f, 1.0f) * 65535.0f + 0.5f);
}

struct Scale {
    float lumaBlack;
    float lumaSpan;
    float chromaCenter;
    float chromaSpan;
};

Scale scaleFor(bool full, int depth) {
    const int d = std::clamp(depth, 8, 16);
    const float top = static_cast<float>(((1u << d) - 1u) << (16 - d));
    if (full) return {0.0f, top, 32768.0f, top};
    return {4096.0f, 56064.0f, 32768.0f, 57344.0f};
}

void store(std::uint16_t* o, float r, float g, float b) {
    o[0] = pack(r);
    o[1] = pack(g);
    o[2] = pack(b);
    o[3] = 65535;
}

void fromYcgco(const std::uint16_t* y, const std::uint16_t* u, const std::uint16_t* v, std::size_t pixels, const Scale& s, std::uint16_t* out) {
    const float invL = 1.0f / s.lumaSpan, invC = 1.0f / s.chromaSpan;
    for (std::size_t i = 0; i < pixels; ++i) {
        const float luma = (y[i] - s.lumaBlack) * invL;
        const float cg = (u[i] - s.chromaCenter) * invC;
        const float co = (v[i] - s.chromaCenter) * invC;
        store(out + i * 4, luma - cg + co, luma + cg, luma - cg - co);
    }
}

void fromConstantLuminance(const std::uint16_t* y, const std::uint16_t* u, const std::uint16_t* v, std::size_t pixels, const Scale& s, std::uint16_t* out) {
    const float invL = 1.0f / s.lumaSpan, invC = 1.0f / s.chromaSpan;
    for (std::size_t i = 0; i < pixels; ++i) {
        const float yc = (y[i] - s.lumaBlack) * invL;
        const float cb = (u[i] - s.chromaCenter) * invC;
        const float cr = (v[i] - s.chromaCenter) * invC;
        const float bp = yc + cb * (cb <= 0.0f ? 1.9404f : 1.5816f);
        const float rp = yc + cr * (cr <= 0.0f ? 1.7184f : 0.9936f);
        const float luminance = bt2020Eotf(yc);
        const float r = bt2020Eotf(rp);
        const float b = bt2020Eotf(bp);
        const float g = std::max((luminance - 0.2627f * r - 0.0593f * b) / 0.6780f, 0.0f);
        store(out + i * 4, rp, bt2020Oetf(g), bp);
    }
}

void fromIctcp(const std::uint16_t* y, const std::uint16_t* u, const std::uint16_t* v, std::size_t pixels, const Scale& s, bool hlg, std::uint16_t* out) {
    const float invL = 1.0f / s.lumaSpan, invC = 1.0f / s.chromaSpan;
    for (std::size_t i = 0; i < pixels; ++i) {
        const float in = (y[i] - s.lumaBlack) * invL;
        const float ct = (u[i] - s.chromaCenter) * invC;
        const float cp = (v[i] - s.chromaCenter) * invC;
        const float lp = in + 0.00860903703793281f * ct + 0.111029625003026f * cp;
        const float mp = in - 0.00860903703793276f * ct - 0.111029625003026f * cp;
        const float sp = in + 0.560031335710679f * ct - 0.320627174987319f * cp;
        float l, m, sl;
        if (hlg) { l = hlgEotf(lp); m = hlgEotf(mp); sl = hlgEotf(sp); }
        else { l = pqEotf(lp); m = pqEotf(mp); sl = pqEotf(sp); }
        const float r = 3.436606694333078f * l - 2.506452118656270f * m + 0.069845424323191f * sl;
        const float g = -0.791329555598929f * l + 1.983600451792291f * m - 0.192270896193362f * sl;
        const float b = -0.0259499f * l - 0.09891371f * m + 1.12486361f * sl;
        if (hlg) store(out + i * 4, hlgOetf(r), hlgOetf(g), hlgOetf(b));
        else store(out + i * 4, pqOetf(r), pqOetf(g), pqOetf(b));
    }
}
}

bool isSpecialMatrix(int colorspace) {
    return colorspace == kSpaceYcgco || colorspace == kSpaceBt2020Cl || colorspace == kSpaceIctcp;
}

void specialMatrixToRgb16(const std::uint16_t* y, const std::uint16_t* u, const std::uint16_t* v, std::size_t pixels, int colorspace, bool fullRange, bool hlg, int depth, std::uint16_t* rgba64) {
    const Scale s = scaleFor(fullRange, depth);
    if (colorspace == kSpaceYcgco) fromYcgco(y, u, v, pixels, s, rgba64);
    else if (colorspace == kSpaceBt2020Cl) fromConstantLuminance(y, u, v, pixels, s, rgba64);
    else fromIctcp(y, u, v, pixels, s, hlg, rgba64);
}

void narrowRgba64(const std::uint16_t* rgba64, std::size_t pixels, std::uint8_t* rgba8) {
    for (std::size_t i = 0; i < pixels * 4; ++i) {
        rgba8[i] = static_cast<std::uint8_t>(std::min(255, (rgba64[i] + 128) >> 8));
    }
}
