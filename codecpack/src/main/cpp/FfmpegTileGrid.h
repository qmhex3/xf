#pragma once
#include <string>
#include "FfmpegDecoder.h"

bool isAvifStill(const std::string& path);
bool tileGridSize(const std::string& path, int& width, int& height);
int decodeTileGrid(const std::string& path, frameout::DecodedFrame& out);
