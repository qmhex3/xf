#pragma once
#include <cstddef>
#include <cstdint>

struct HdrParams {
    bool hlg = false;
    int primaries = 1;
    float peakNits = 1000.0f;
};

void toneMapHdrToSdr(std::uint8_t* rgba, int width, int height, bool hlg, float peakNits, int primaries = 1);
float toneMapHdr16(const std::uint16_t* rgba64, int width, int height, const HdrParams& params, std::uint8_t* rgba8, std::uint16_t* gain);
void fitToGamut(float* rgb, float luma);
