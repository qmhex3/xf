#pragma once
#include <cstdint>

struct AVFrame;
struct AVDOVIMetadata;

const AVDOVIMetadata* dolbyMetadata(const AVFrame* frame);
float dolbyPeakNits(const AVDOVIMetadata* meta);
void dolbyToPq16(const std::uint16_t* y, const std::uint16_t* u, const std::uint16_t* v, int width, int height, const AVDOVIMetadata* meta, std::uint16_t* rgba64);
