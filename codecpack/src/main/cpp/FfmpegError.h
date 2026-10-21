#pragma once
#include <string>

struct AVStream;
struct AVFormatContext;
struct AVPacket;

namespace frameout {

enum class FrameOutError : int {
    None = 0,
    Unknown = 1,
    SourceMissing = 2,
    PermissionDenied = 3,
    IoError = 4,
    CorruptData = 5,
    NoVideoStream = 6,
    DecoderMissing = 7,
    DecoderInitFailed = 8,
    Drm = 9,
    UnsupportedFeature = 10,
    OutOfMemory = 11,
    Internal = 12,
    UnsupportedContainer = 13
};

void recordError(const std::string& path, FrameOutError code);
void clearError(const std::string& path);
FrameOutError lastError(const std::string& path);
FrameOutError classifyException();
FrameOutError classifyOpenInput(int averror);
FrameOutError classifyDecodeError(int averror);
FrameOutError classifyOpenCodec(int averror, const AVStream* stream);
FrameOutError classifyNoStream(const AVFormatContext* format, int averror);
bool streamEncrypted(const AVStream* stream);
bool packetEncrypted(const AVPacket* packet);

}
