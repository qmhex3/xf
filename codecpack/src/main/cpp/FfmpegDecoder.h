#pragma once
#include <atomic>
#include <cstddef>
#include <cstdint>
#include <memory>
#include <string>
#include <vector>

namespace frameout {

struct GainMap {
    std::vector<std::uint8_t> data;
    int width = 0;
    int height = 0;
    float ratioMin[3] = {1.0f, 1.0f, 1.0f};
    float ratioMax[3] = {1.0f, 1.0f, 1.0f};
    bool valid = false;
};

struct SignalFrame {
    std::vector<std::uint16_t> rgb;
    int width = 0;
    int height = 0;
    int primaries = 0;
    int transfer = 0;
    bool valid = false;
};

struct DecodedFrame {
    std::vector<std::uint8_t> rgba;
    std::uint8_t* external = nullptr;
    std::int64_t ptsUs = 0;
    bool keyframe = false;
    int nearestKeyframeIndex = -1;
    std::int64_t nearestKeyframePtsUs = -1;
    int width = 0;
    int height = 0;
    GainMap gain;
    SignalFrame signal;
};

struct IndexedFrame {
    std::int64_t ptsUs = -1;
    bool keyframe = false;
    int nearestKeyframeIndex = -1;
    std::int64_t nearestKeyframePtsUs = -1;
};

bool probe(const std::string& path, std::string& result);
int frameCount(const std::string& path);
int indexFrames(const std::string& path, std::vector<IndexedFrame>& out, long long cancelToken = 0);
int indexFramesCancellable(const std::string& path, std::vector<IndexedFrame>& out, const std::shared_ptr<std::atomic<bool>>& cancel);
int indexFramesPrefix(const std::string& path, std::vector<IndexedFrame>& out, int maxPackets, bool& complete);
int indexImpl(const std::string& path, std::vector<IndexedFrame>& out, const std::shared_ptr<std::atomic<bool>>& cancel, std::size_t maxPackets, bool& complete);
void setIndexCancel(long long token, bool cancelled);
void releaseIndexCancel(long long token);
int width(const std::string& path);
int height(const std::string& path);
int decodeFrame(const std::string& path, int index, DecodedFrame& out, int hintIndex = -1, std::int64_t hintPtsUs = -1);
int seekSessionToKeyframe(const std::string& path, int keyframeIndex, std::int64_t keyframePtsUs);
void closeDecoderSession(const std::string& path);
void cancelDecoderSession(const std::string& path);
int installIndexTable(const std::string& path, std::vector<IndexedFrame>&& table);
void setGainmapEnabled(bool enabled);
bool lastGain(const std::string& path, GainMap& out);
void setSignalEnabled(bool enabled);
bool signalEnabled();
void keepSignal(const std::string& path, SignalFrame&& signal);
bool signalInfo(const std::string& path, int* meta);
bool takeSignal(const std::string& path, SignalFrame& out);
void dropSignal(const std::string& path);

}
