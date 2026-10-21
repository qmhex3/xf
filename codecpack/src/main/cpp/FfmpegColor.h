#pragma once
#include <cstdint>
#include <vector>
#include "FfmpegDecoder.h"

struct AVFrame;
struct SwsContext;
bool convertFrame(AVFrame* frame, int width, int height, int rotation, bool flipH, SwsContext*& scaler, int& scalerWidth, int& scalerHeight, std::int64_t& scalerKey, std::vector<std::uint8_t>& rgba, std::uint8_t* external = nullptr, frameout::GainMap* gain = nullptr, float peakHintNits = 0.0f, int dolbyProfile = 0, frameout::SignalFrame* exactSignal = nullptr);
void applyCodecColorDefaults(AVFrame* frame, int codecId);
void releaseScaler(SwsContext*& scaler);
