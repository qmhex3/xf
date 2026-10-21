#pragma once
#include <cstddef>
#include <cstdint>

constexpr int kSpaceYcgco = 8;
constexpr int kSpaceBt2020Cl = 10;
constexpr int kSpaceIctcp = 14;

bool isSpecialMatrix(int colorspace);
void specialMatrixToRgb16(const std::uint16_t* y, const std::uint16_t* u, const std::uint16_t* v, std::size_t pixels, int colorspace, bool fullRange, bool hlg, int depth, std::uint16_t* rgba64);
void narrowRgba64(const std::uint16_t* rgba64, std::size_t pixels, std::uint8_t* rgba8);
