#include "FfmpegHdr.h"
#include <algorithm>
#include <cmath>
#include <vector>

namespace {
constexpr float kReferenceWhiteNits = 203.0f;
constexpr float kHlgPeakNits = 1000.0f;
constexpr float kKnee = 0.75f;
constexpr float kDesatStrength = 0.6f;
constexpr float kDesatExponent = 1.5f;
constexpr int kDesatSteps = 1024;
constexpr int kOotfSize = 1024;
constexpr int kOetfSize = 4096;

const float kBayer[8][8] = {
    { 0, 32,  8, 40,  2, 34, 10, 42},
    {48, 16, 56, 24, 50, 18, 58, 26},
    {12, 44,  4, 36, 14, 46,  6, 38},
    {60, 28, 52, 20, 62, 30, 54, 22},
    { 3, 35, 11, 43,  1, 33,  9, 41},
    {51, 19, 59, 27, 49, 17, 57, 25},
    {15, 47,  7, 39, 13, 45,  5, 37},
    {63, 31, 55, 23, 61, 29, 53, 21}
};

struct Tables {
    float eotf[256];
    float ootf[kOotfSize + 1];
    float oetf[kOetfSize];
};

float pqToNits(float v) {
    const float m1 = 0.1593017578125f, m2 = 78.84375f;
    const float c1 = 0.8359375f, c2 = 18.8515625f, c3 = 18.6875f;
    const float p = std::pow(v, 1.0f / m2);
    const float num = std::max(p - c1, 0.0f);
    const float den = c2 - c3 * p;
    return den <= 0.0f ? 10000.0f : 10000.0f * std::pow(num / den, 1.0f / m1);
}

float hlgToScene(float e) {
    const float a = 0.17883277f, b = 0.28466892f, c = 0.55991073f;
    return e <= 0.5f ? (e * e) / 3.0f : (std::exp((e - c) / a) + b) / 12.0f;
}

float srgbEncode(float v) {
    return v <= 0.0031308f ? 12.92f * v : 1.055f * std::pow(v, 1.0f / 2.4f) - 0.055f;
}

Tables build(bool hlg) {
    Tables t;
    for (int i = 0; i < 256; ++i) {
        const float v = static_cast<float>(i) / 255.0f;
        t.eotf[i] = hlg ? hlgToScene(v) : pqToNits(v) / kReferenceWhiteNits;
    }
    for (int i = 0; i <= kOotfSize; ++i) {
        const float y = static_cast<float>(i) / kOotfSize;
        t.ootf[i] = hlg ? std::pow(y, 0.2f) * kHlgPeakNits / kReferenceWhiteNits : 1.0f;
    }
    for (int i = 0; i < kOetfSize; ++i) {
        const float v = srgbEncode(static_cast<float>(i) / (kOetfSize - 1));
        t.oetf[i] = std::clamp(v, 0.0f, 1.0f) * 255.0f;
    }
    return t;
}

const Tables& tables(bool hlg) {
    static const Tables pq = build(false);
    static const Tables hybrid = build(true);
    return hlg ? hybrid : pq;
}

float luma(float r, float g, float b) {
    return 0.2627f * r + 0.6780f * g + 0.0593f * b;
}

float toneTarget(float source, float white) {
    if (source <= kKnee) return std::max(source, 0.0f);
    const float headroom = 1.0f - kKnee;
    const float span = (white - kKnee) / headroom;
    const float u = (source - kKnee) / headroom;
    return std::min(kKnee + headroom * u * (1.0f + u / (span * span)) / (1.0f + u), 1.0f);
}

struct Desat {
    float table[kDesatSteps + 1];
    float scale;
    Desat(float white) : scale(1.0f / (white - kKnee)) {
        for (int i = 0; i <= kDesatSteps; ++i) table[i] = kDesatStrength * std::pow(static_cast<float>(i) / kDesatSteps, kDesatExponent);
    }
    float weight(float source) const {
        if (source <= kKnee) return 0.0f;
        const float t = std::clamp((source - kKnee) * scale, 0.0f, 1.0f);
        return table[static_cast<int>(t * kDesatSteps + 0.5f)];
    }
};

std::uint8_t encode(const Tables& t, float linear, float dither) {
    const float clamped = std::clamp(linear, 0.0f, 1.0f);
    const float v = t.oetf[static_cast<int>(clamped * (kOetfSize - 1) + 0.5f)] + dither;
    return static_cast<std::uint8_t>(std::clamp(static_cast<int>(v + 0.5f), 0, 255));
}

float smoothEotf(const Tables& t, int code, float offset) {
    const int other = std::clamp(offset < 0.0f ? code - 1 : code + 1, 0, 255);
    const float k = std::fabs(offset);
    return t.eotf[code] + (t.eotf[other] - t.eotf[code]) * k;
}
}

namespace {
constexpr int kTable = 4096;
constexpr int kEncodeTable = 16384;
constexpr float kEps = 1.0f / 64.0f;
constexpr float kGainLogLow = -4.2f;
constexpr float kGainLogHigh = 4.0f;
constexpr float kMinLuma = 1e-6f;

struct SignalTables {
    float srgbDecode[256];
    float pq[kTable + 1];
    float hlg[kTable + 1];
    float ootf[kTable + 1];
    float encode[kEncodeTable + 1];
    SignalTables() {
        for (int i = 0; i < 256; ++i) {
            const float v = static_cast<float>(i) / 255.0f;
            srgbDecode[i] = v <= 0.04045f ? v / 12.92f : std::pow((v + 0.055f) / 1.055f, 2.4f);
        }
        for (int i = 0; i <= kTable; ++i) {
            const float v = static_cast<float>(i) / kTable;
            pq[i] = pqToNits(v) / kReferenceWhiteNits;
            hlg[i] = hlgToScene(v);
            ootf[i] = std::pow(v, 0.2f) * kHlgPeakNits / kReferenceWhiteNits;
        }
        for (int i = 0; i <= kEncodeTable; ++i) {
            encode[i] = std::clamp(srgbEncode(static_cast<float>(i) / kEncodeTable), 0.0f, 1.0f) * 255.0f;
        }
    }
};

const SignalTables& signalTables() {
    static const SignalTables t;
    return t;
}


struct Luma { float r, g, b; };

Luma lumaFor(int primaries) {
    if (primaries == 0) return {0.2126f, 0.7152f, 0.0722f};
    if (primaries == 2) return {0.2290f, 0.6917f, 0.0793f};
    if (primaries == 3) return {0.2152f, 0.7091f, 0.0757f};
    return {0.2627f, 0.6780f, 0.0593f};
}

void toDisplay(int primaries, float r, float g, float b, float* out) {
    if (primaries == 0) {
        out[0] = r; out[1] = g; out[2] = b;
    } else if (primaries == 2) {
        out[0] = 1.2249401f * r - 0.2249404f * g;
        out[1] = -0.0420569f * r + 1.0420571f * g;
        out[2] = -0.0196376f * r - 0.0786361f * g + 1.0982735f * b;
    } else if (primaries == 3) {
        out[0] = 1.157516f * r - 0.154962f * g - 0.002554f * b;
        out[1] = -0.041500f * r + 1.045568f * g - 0.004068f * b;
        out[2] = -0.018050f * r - 0.078578f * g + 1.096628f * b;
    } else {
        out[0] = 1.6605f * r - 0.5876f * g - 0.0728f * b;
        out[1] = -0.1246f * r + 1.1329f * g - 0.0083f * b;
        out[2] = -0.0182f * r - 0.1006f * g + 1.1187f * b;
    }
}

void blendToward(float* c, float y, float t) {
    for (int i = 0; i < 3; ++i) c[i] = y + (c[i] - y) * t;
}

void fitGamut(float* c, float y) {
    const float low = std::min({c[0], c[1], c[2]});
    if (low < 0.0f) blendToward(c, y, (y - low) > kMinLuma ? y / (y - low) : 0.0f);
    const float high = std::max({c[0], c[1], c[2]});
    if (high > 1.0f) blendToward(c, y, (1.0f - y) / (high - y));
}

inline int tableIndex(std::uint16_t v) {
    const int i = (static_cast<int>(v) + 8) >> 4;
    return i > kTable ? kTable : i;
}

inline std::uint8_t quantize(const SignalTables& t, float linear, float dither) {
    const float clamped = std::clamp(linear, 0.0f, 1.0f);
    const float v = t.encode[static_cast<int>(clamped * kEncodeTable + 0.5f)] + dither;
    return static_cast<std::uint8_t>(std::clamp(static_cast<int>(v + 0.5f), 0, 255));
}
}

float toneMapHdr16(const std::uint16_t* src, int width, int height, const HdrParams& params, std::uint8_t* rgba8, std::uint16_t* gain) {
    const SignalTables& t = signalTables();
    const float* eotf = params.hlg ? t.hlg : t.pq;
    const float white = std::max(params.hlg ? kHlgPeakNits / kReferenceWhiteNits : params.peakNits / kReferenceWhiteNits, 2.0f);
    const float ratioMax = std::clamp(white, 2.0f, 16.0f);
    const float gainScale = 65535.0f / (kGainLogHigh - kGainLogLow);
    const Luma l = lumaFor(params.primaries);
    const Desat desat(white);
    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            const std::size_t i = static_cast<std::size_t>(y) * width + x;
            const std::uint16_t* p = src + i * 4;
            float r = eotf[tableIndex(p[0])], g = eotf[tableIndex(p[1])], b = eotf[tableIndex(p[2])];
            if (params.hlg) {
                const float k = t.ootf[std::min(kTable, static_cast<int>(std::clamp(l.r * r + l.g * g + l.b * b, 0.0f, 1.0f) * kTable + 0.5f))];
                r *= k; g *= k; b *= k;
            }
            const float source = l.r * r + l.g * g + l.b * b;
            const float target = toneTarget(source, white);
            const float scale = source > kMinLuma ? target / source : 1.0f;
            const float weight = desat.weight(std::max({r, g, b}));
            const float keep = scale * (1.0f - weight);
            const float gray = target * weight;
            float c[3];
            toDisplay(params.primaries, r * keep + gray, g * keep + gray, b * keep + gray, c);
            fitGamut(c, std::clamp(target, 0.0f, 1.0f));
            const float d = (kBayer[y & 7][x & 7] + 0.5f) / 64.0f - 0.5f;
            std::uint8_t* o = rgba8 + i * 4;
            o[0] = quantize(t, c[0], d);
            o[1] = quantize(t, c[1], d);
            o[2] = quantize(t, c[2], d);
            o[3] = 255;
            if (gain) {
                float h[3];
                toDisplay(params.primaries, r, g, b, h);
                std::uint16_t* gp = gain + i * 4;
                for (int k = 0; k < 3; ++k) {
                    const float sdr = t.srgbDecode[o[k]];
                    const float lg = h[k] < 0.0f ? 0.0f : std::clamp(std::log((h[k] + kEps) / (sdr + kEps)), kGainLogLow, kGainLogHigh);
                    gp[k] = static_cast<std::uint16_t>((lg - kGainLogLow) * gainScale + 0.5f);
                }
                gp[3] = 0;
            }
        }
    }
    return ratioMax;
}

void fitToGamut(float* rgb, float luma) {
    fitGamut(rgb, std::clamp(luma, 0.0f, 1.0f));
}

void toneMapHdrToSdr(std::uint8_t* rgba, int width, int height, bool hlg, float peakNits, int primaries) {
    const Tables& t = tables(hlg);
    const float white = std::max(hlg ? kHlgPeakNits / kReferenceWhiteNits : peakNits / kReferenceWhiteNits, 2.0f);
    const Luma l = lumaFor(primaries);
    const Desat desat(white);
    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            std::uint8_t* p = rgba + (static_cast<std::size_t>(y) * width + x) * 4;
            const float d = (kBayer[y & 7][x & 7] + 0.5f) / 64.0f - 0.5f;
            float r = smoothEotf(t, p[0], d), g = smoothEotf(t, p[1], d), b = smoothEotf(t, p[2], d);
            if (hlg) {
                const float factor = t.ootf[std::min(kOotfSize, static_cast<int>(std::clamp(l.r * r + l.g * g + l.b * b, 0.0f, 1.0f) * kOotfSize + 0.5f))];
                r *= factor; g *= factor; b *= factor;
            }
            const float source = l.r * r + l.g * g + l.b * b;
            const float target = source > kKnee ? toneTarget(source, white) : std::max(source, 0.0f);
            const float scale = source > kMinLuma ? target / source : 1.0f;
            const float weight = desat.weight(std::max({r, g, b}));
            const float keep = scale * (1.0f - weight);
            const float gray = target * weight;
            float c[3];
            toDisplay(primaries, r * keep + gray, g * keep + gray, b * keep + gray, c);
            fitGamut(c, std::clamp(target, 0.0f, 1.0f));
            p[0] = encode(t, c[0], d);
            p[1] = encode(t, c[1], d);
            p[2] = encode(t, c[2], d);
        }
    }
}
