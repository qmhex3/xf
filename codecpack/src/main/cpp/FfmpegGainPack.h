#pragma once
#include "FfmpegDecoder.h"
#include <algorithm>
#include <cmath>
#include <cstddef>
#include <cstdint>
#include <vector>

constexpr float kGainLogLowBound = -4.2f;
constexpr float kGainLogHighBound = 4.0f;
constexpr double kGainTailFraction = 0.0;

inline void packGain(const std::uint16_t* plane, int w, int h, frameout::GainMap& out) {
    const std::size_t count = static_cast<std::size_t>(w) * h;
    out.width = w;
    out.height = h;
    out.data.resize(count * 3);
    const float span = kGainLogHighBound - kGainLogLowBound;
    for (int k = 0; k < 3; ++k) {
        std::vector<std::uint32_t> histogram(65536, 0);
        for (std::size_t i = 0; i < count; ++i) ++histogram[plane[i * 4 + k]];
        const std::size_t tail = static_cast<std::size_t>(static_cast<double>(count) * kGainTailFraction);
        std::uint16_t lo = 0, hi = 65535;
        std::size_t seen = 0;
        for (int v = 0; v < 65536; ++v) { seen += histogram[v]; if (seen > tail) { lo = static_cast<std::uint16_t>(v); break; } }
        seen = 0;
        for (int v = 65535; v >= 0; --v) { seen += histogram[v]; if (seen > tail) { hi = static_cast<std::uint16_t>(v); break; } }
        if (count == 0 || hi < lo) { lo = 0; hi = 0; }
        const float logLo = std::min(kGainLogLowBound + span * lo / 65535.0f, 0.0f);
        const float logHi = std::max(kGainLogLowBound + span * hi / 65535.0f, 0.0f);
        const float width = std::max(logHi - logLo, 1e-4f);
        int zero = static_cast<int>(std::lround(-logLo / width * 255.0f));
        zero = std::clamp(zero, 0, 255);
        float step = 0.0f;
        if (zero > 0) step = std::max(step, -logLo / zero);
        if (zero < 255) step = std::max(step, logHi / (255 - zero));
        step = std::max(step, 1e-5f);
        out.ratioMin[k] = std::exp(-step * zero);
        out.ratioMax[k] = std::exp(step * (255 - zero));
        for (std::size_t i = 0; i < count; ++i) {
            const float lg = kGainLogLowBound + span * plane[i * 4 + k] / 65535.0f;
            out.data[i * 3 + k] = static_cast<std::uint8_t>(std::clamp(std::lround(lg / step) + zero, 0L, 255L));
        }
    }
    out.valid = true;
}

