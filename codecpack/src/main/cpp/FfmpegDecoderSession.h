#pragma once
#include <string>
#include <vector>
#include "FfmpegDecoder.h"

int decodeSession(const std::string& path, int index, frameout::DecodedFrame& out, int hintIndex = -1, std::int64_t hintPtsUs = -1);
int seekSessionToKeyframe(const std::string& path, int keyframeIndex, std::int64_t keyframePtsUs);
void closeSession(const std::string& path);
void cancelSession(const std::string& path);
void setGainmapEnabled(bool enabled);
bool lastSessionGain(const std::string& path, frameout::GainMap& out);
int installTable(const std::string& path, std::vector<frameout::IndexedFrame>&& table);
