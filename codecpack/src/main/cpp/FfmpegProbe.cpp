#include "FfmpegDecoder.h"
#include "FfmpegDisplay.h"
#include "FfmpegError.h"
#include "FfmpegTileGrid.h"
#include <cstdio>
#include <climits>
#include <vector>

#if FRAMEOUT_HAS_FFMPEG
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/mathematics.h>
}
#include "FfmpegOpenGuard.h"

namespace {

struct ProbeContext {
    AVFormatContext* format = nullptr;
    AVCodecContext* codec = nullptr;
    int stream = -1;
};

void closeProbe(ProbeContext& d) {
    if (d.codec) avcodec_free_context(&d.codec);
    if (d.format) avformat_close_input(&d.format);
    d.stream = -1;
}

bool failProbe(const std::string& path, frameout::FrameOutError code, ProbeContext& d) {
    frameout::recordError(path, code);
    closeProbe(d);
    return false;
}

bool openProbeCodec(const std::string& path, ProbeContext& d) {
    AVStream* stream = d.format->streams[d.stream];
    if (frameout::streamEncrypted(stream)) return failProbe(path, frameout::FrameOutError::Drm, d);
    const AVCodec* codec = avcodec_find_decoder(stream->codecpar->codec_id);
    if (!codec) return failProbe(path, frameout::FrameOutError::DecoderMissing, d);
    d.codec = avcodec_alloc_context3(codec);
    if (!d.codec) return failProbe(path, frameout::FrameOutError::OutOfMemory, d);
    int rc = avcodec_parameters_to_context(d.codec, stream->codecpar);
    if (rc < 0) return failProbe(path, frameout::classifyOpenCodec(rc, stream), d);
    rc = avcodec_open2(d.codec, codec, nullptr);
    if (rc < 0) return failProbe(path, frameout::classifyOpenCodec(rc, stream), d);
    return true;
}

bool openProbe(const std::string& path, ProbeContext& d) {
    frameout::clearError(path);
    frameout::OpenGuard guard(&d.format);
    int rc = guard.open(path.c_str());
    if (rc < 0) return failProbe(path, frameout::classifyOpenInput(rc), d);
    rc = guard.findStreamInfo();
    if (rc < 0) return failProbe(path, frameout::classifyOpenInput(rc), d);
    const int best = av_find_best_stream(d.format, AVMEDIA_TYPE_VIDEO, -1, -1, nullptr, 0);
    if (best < 0) return failProbe(path, frameout::classifyNoStream(d.format, best), d);
    d.stream = best;
    if (!frameout::dimensionsAllowed(d.format->streams[best]->codecpar)) return failProbe(path, frameout::FrameOutError::UnsupportedFeature, d);
    return openProbeCodec(path, d);
}

}
#endif

namespace frameout {

bool probe(const std::string& path, std::string& result) {
#if FRAMEOUT_HAS_FFMPEG
    int gridWidth = 0, gridHeight = 0;
    if (tileGridSize(path, gridWidth, gridHeight)) {
        char gridBuffer[64];
        std::snprintf(gridBuffer, sizeof(gridBuffer), "video/av1|%d|%d|0", gridWidth, gridHeight);
        result = gridBuffer;
        return true;
    }
    ProbeContext d;
    if (!openProbe(path, d)) return false;
    AVStream* stream = d.format->streams[d.stream];
    const long long duration = d.format->duration == AV_NOPTS_VALUE ? 0 : d.format->duration;
    char buffer[256];
    std::snprintf(buffer, sizeof(buffer), "video/%s|%d|%d|%lld", avcodec_get_name(stream->codecpar->codec_id), d.codec->width, d.codec->height, duration);
    result = buffer;
    closeProbe(d);
    return true;
#else
    return false;
#endif
}

int frameCount(const std::string& path) {
#if FRAMEOUT_HAS_FFMPEG
    int gridWidth = 0, gridHeight = 0;
    if (tileGridSize(path, gridWidth, gridHeight)) return 1;
    ProbeContext d;
    if (!openProbe(path, d)) return 0;
    AVStream* stream = d.format->streams[d.stream];
    long long count = stream->nb_frames;
    closeProbe(d);
    if (count <= 0) {
        std::vector<IndexedFrame> frames;
        if (indexFrames(path, frames) >= 0) count = static_cast<long long>(frames.size());
    }
    return static_cast<int>(count > 0 && count <= INT_MAX ? count : 0);
#else
    return 0;
#endif
}

int width(const std::string& path) {
#if FRAMEOUT_HAS_FFMPEG
    int gridWidth = 0, gridHeight = 0;
    if (tileGridSize(path, gridWidth, gridHeight)) return gridWidth;
    ProbeContext d;
    if (!openProbe(path, d)) return 0;
    const int value = displayGeometry(d.format->streams[d.stream]).width;
    closeProbe(d);
    return value;
#else
    return 0;
#endif
}

int height(const std::string& path) {
#if FRAMEOUT_HAS_FFMPEG
    int gridWidth = 0, gridHeight = 0;
    if (tileGridSize(path, gridWidth, gridHeight)) return gridHeight;
    ProbeContext d;
    if (!openProbe(path, d)) return 0;
    const int value = displayGeometry(d.format->streams[d.stream]).height;
    closeProbe(d);
    return value;
#else
    return 0;
#endif
}

}
