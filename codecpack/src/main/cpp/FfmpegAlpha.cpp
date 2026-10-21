#include "FfmpegAlpha.h"

void flattenAlpha(std::uint8_t* rgba, std::size_t pixels) {
    for (std::size_t i = 0; i < pixels; ++i) {
        std::uint8_t* p = rgba + i * 4;
        const unsigned a = p[3];
        if (a == 255) continue;
        const unsigned inverse = 255u - a;
        p[0] = static_cast<std::uint8_t>((p[0] * a + 255u * inverse + 127) / 255);
        p[1] = static_cast<std::uint8_t>((p[1] * a + 255u * inverse + 127) / 255);
        p[2] = static_cast<std::uint8_t>((p[2] * a + 255u * inverse + 127) / 255);
        p[3] = 255;
    }
}
