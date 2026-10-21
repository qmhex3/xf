#pragma once
#include <atomic>
#include <cstdint>
#include <memory>
#include <string>
#include <vector>
#include "FfmpegColor.h"
#include "FfmpegDecoder.h"
#include "FfmpegError.h"
#if FRAMEOUT_HAS_FFMPEG
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/cpu.h>
#include <libavutil/frame.h>
#include <libavutil/mastering_display_metadata.h>
}
namespace frameout_session {
struct DecoderContext {
    AVFormatContext* format = nullptr;
    AVCodecContext* codec = nullptr;
    AVPacket* packet = nullptr;
    AVFrame* frame = nullptr;
    SwsContext* scaler = nullptr;
    int stream = -1;
    int scalerWidth = 0;
    int scalerHeight = 0;
    std::int64_t scalerFormat = -1;
    int nearestKeyframeIndex = -1;
    std::int64_t nearestKeyframePtsUs = -1;
    int rotation = 0;
    bool flipH = false;
    bool intraOnly = false;
    std::int64_t leadingFloorUs = -1;
    std::int64_t expectedKeyPtsUs = -1;
    float streamPeakNits = 0.0f;
    int dolbyProfile = 0;
    std::vector<frameout::IndexedFrame> table;
    bool tableLoaded = false;
    std::int64_t lastPtsUs = -1;
    frameout::FrameOutError failure = frameout::FrameOutError::None;
    std::shared_ptr<std::atomic<bool>> cancel;
};
extern std::atomic<bool> gainmapEnabled;
inline constexpr int kMaxConsecutiveErrors = 64;
inline constexpr int kCancelledRc = -11;
inline bool cancelRequested(const DecoderContext& d) { return d.cancel && d.cancel->load(); }
void armInterrupt(DecoderContext& d);
void closeDecoder(DecoderContext& d);
int decoderThreads();
bool openDecoder(const std::string& path, DecoderContext& d);
bool isKeyFrame(const AVFrame* frame, const DecoderContext& d);
std::int64_t framePts(AVFrame* frame, const DecoderContext& d);
bool resetDecoder(DecoderContext& d);
bool jumpToKeyframe(DecoderContext& d, int& sessionIndex, int index, int hintIndex, std::int64_t hintPtsUs, bool force = false);
bool loadTable(const char* path, DecoderContext& d);
int decodePackets(DecoderContext& d, int index, int& currentIndex, frameout::DecodedFrame& out, std::int64_t targetPts);
}
#endif
