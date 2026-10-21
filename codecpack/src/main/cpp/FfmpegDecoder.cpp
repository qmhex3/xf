#include "FfmpegDecoder.h"
#include "FfmpegDecoderSession.h"
#include <utility>

namespace frameout {

int decodeFrame(const std::string& path, int index, DecodedFrame& out, int hintIndex, std::int64_t hintPtsUs) {
    return decodeSession(path, index, out, hintIndex, hintPtsUs);
}

int seekSessionToKeyframe(const std::string& path, int keyframeIndex, std::int64_t keyframePtsUs) {
    return ::seekSessionToKeyframe(path, keyframeIndex, keyframePtsUs);
}

void closeDecoderSession(const std::string& path) {
    closeSession(path);
}

void cancelDecoderSession(const std::string& path) {
    cancelSession(path);
}

int installIndexTable(const std::string& path, std::vector<IndexedFrame>&& table) {
    return ::installTable(path, std::move(table));
}

void setGainmapEnabled(bool enabled) {
    ::setGainmapEnabled(enabled);
}

bool lastGain(const std::string& path, GainMap& out) {
    return lastSessionGain(path, out);
}

}
