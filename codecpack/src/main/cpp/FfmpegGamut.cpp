#include "FfmpegGamut.h"
#include "FfmpegHdr.h"
#include <algorithm>
#include <cmath>
#include <cstdint>

namespace {
struct GamutTables {
    float decode[256];
    std::uint8_t encode[4096];
    GamutTables() {
        for (int i = 0; i < 256; ++i) {
            const float v = static_cast<float>(i) / 255.0f;
            decode[i] = v <= 0.04045f ? v / 12.92f : std::pow((v + 0.055f) / 1.055f, 2.4f);
        }
        for (int i = 0; i < 4096; ++i) {
            const float v = static_cast<float>(i) / 4095.0f;
            const float e = v <= 0.0031308f ? 12.92f * v : 1.055f * std::pow(v, 1.0f / 2.4f) - 0.055f;
            encode[i] = static_cast<std::uint8_t>(std::clamp(e, 0.0f, 1.0f) * 255.0f + 0.5f);
        }
    }
};

void applyMatrix(std::uint8_t* rgba, std::size_t pixels, const float (&m)[9]) {
    static const GamutTables tables;
    for (std::size_t i = 0; i < pixels; ++i) {
        std::uint8_t* p = rgba + i * 4;
        const float r = tables.decode[p[0]];
        const float g = tables.decode[p[1]];
        const float b = tables.decode[p[2]];
        float c[3] = {
            m[0] * r + m[1] * g + m[2] * b,
            m[3] * r + m[4] * g + m[5] * b,
            m[6] * r + m[7] * g + m[8] * b
        };
        fitToGamut(c, 0.2126f * c[0] + 0.7152f * c[1] + 0.0722f * c[2]);
        p[0] = tables.encode[static_cast<int>(std::clamp(c[0], 0.0f, 1.0f) * 4095.0f + 0.5f)];
        p[1] = tables.encode[static_cast<int>(std::clamp(c[1], 0.0f, 1.0f) * 4095.0f + 0.5f)];
        p[2] = tables.encode[static_cast<int>(std::clamp(c[2], 0.0f, 1.0f) * 4095.0f + 0.5f)];
    }
}

const float kBt2020ToBt709[9] = { 1.6605f, -0.5876f, -0.0728f, -0.1246f, 1.1329f, -0.0083f, -0.0182f, -0.1006f, 1.1187f };
const float kP3ToBt709[9] = { 1.2249f, -0.2249f, 0.0f, -0.0420f, 1.0420f, 0.0f, -0.0196f, -0.0786f, 1.0983f };
const float kDciP3ToBt709[9] = { 1.157516f, -0.154962f, -0.002554f, -0.041500f, 1.045568f, -0.004068f, -0.018050f, -0.078578f, 1.096628f };
}

void gamutToBt709(std::uint8_t* rgba, std::size_t pixels, int primaries) {
    if (primaries == 1) applyMatrix(rgba, pixels, kBt2020ToBt709);
    else if (primaries == 2) applyMatrix(rgba, pixels, kP3ToBt709);
    else if (primaries == 3) applyMatrix(rgba, pixels, kDciP3ToBt709);
}
