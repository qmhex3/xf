#pragma once
#include <cstddef>
#include <cstdint>

namespace frameout_color {

inline void rotatedCoord(int x, int y, int srcW, int srcH, int rotation, bool flip, int& dx, int& dy) {
    const int fx = flip ? srcW - 1 - x : x;
    if (rotation == 90) { dx = srcH - 1 - y; dy = fx; }
    else if (rotation == 180) { dx = srcW - 1 - fx; dy = srcH - 1 - y; }
    else if (rotation == 270) { dx = y; dy = srcW - 1 - fx; }
    else { dx = fx; dy = y; }
}

inline int rotatedWidth(int srcW, int srcH, int rotation) {
    return (rotation == 90 || rotation == 270) ? srcH : srcW;
}

template <typename T>
inline void rotateTyped(const T* in, int srcW, int srcH, int rotation, bool flip, T* out) {
    const int dstW = rotatedWidth(srcW, srcH, rotation);
    for (int y = 0; y < srcH; ++y) {
        for (int x = 0; x < srcW; ++x) {
            int dx = 0, dy = 0;
            rotatedCoord(x, y, srcW, srcH, rotation, flip, dx, dy);
            out[static_cast<std::size_t>(dy) * dstW + dx] = in[static_cast<std::size_t>(y) * srcW + x];
        }
    }
}

inline void rotatePackRgb16(const std::uint16_t* rgba64, int srcW, int srcH, int rotation, bool flip, std::uint16_t* rgb) {
    const int dstW = rotatedWidth(srcW, srcH, rotation);
    for (int y = 0; y < srcH; ++y) {
        const std::uint16_t* row = rgba64 + static_cast<std::size_t>(y) * srcW * 4;
        for (int x = 0; x < srcW; ++x) {
            int dx = 0, dy = 0;
            rotatedCoord(x, y, srcW, srcH, rotation, flip, dx, dy);
            std::uint16_t* out = rgb + (static_cast<std::size_t>(dy) * dstW + dx) * 3;
            out[0] = row[x * 4];
            out[1] = row[x * 4 + 1];
            out[2] = row[x * 4 + 2];
        }
    }
}

}
