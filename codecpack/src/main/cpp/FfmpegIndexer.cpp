#include "FfmpegDecoder.h"
#include "FfmpegCodecTraits.h"
#include "FfmpegTileGrid.h"
#include "FfmpegError.h"
#include <algorithm>
#include <atomic>
#include <climits>
#include <memory>
#include <mutex>
#include <unordered_map>

namespace {
using CancelFlag = std::shared_ptr<std::atomic<bool>>;
std::mutex cancelMutex;
std::unordered_map<long long, CancelFlag> cancelFlags;

CancelFlag flagFor(long long token, bool create) {
    if (token == 0) return nullptr;
    std::lock_guard<std::mutex> lock(cancelMutex);
    auto it = cancelFlags.find(token);
    if (it != cancelFlags.end()) return it->second;
    if (!create) return nullptr;
    CancelFlag flag = std::make_shared<std::atomic<bool>>(false);
    cancelFlags[token] = flag;
    return flag;
}

bool isCancelled(const CancelFlag& flag) {
    return flag && flag->load();
}
}

namespace frameout {
void setIndexCancel(long long token, bool cancelled) {
    CancelFlag flag = flagFor(token, true);
    if (flag) flag->store(cancelled);
}

void releaseIndexCancel(long long token) {
    if (token == 0) return;
    std::lock_guard<std::mutex> lock(cancelMutex);
    cancelFlags.erase(token);
}
}

#if FRAMEOUT_HAS_FFMPEG
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/mathematics.h>
}
#include "FfmpegOpenGuard.h"

namespace {
constexpr int kCancelled = -6;
constexpr int kNoVideo = -2;
constexpr int kOpenFailed = -1;
constexpr int kTooLarge = -5;
constexpr std::size_t kReorderTail = 48;

struct PacketEntry {
    std::int64_t ptsUs;
    bool keyframe;
};

struct Demuxer {
    AVFormatContext* format = nullptr;
    AVPacket* packet = nullptr;
    int stream = -1;

    ~Demuxer() {
        if (packet) av_packet_free(&packet);
        if (format) avformat_close_input(&format);
    }
};

int failDemuxer(const std::string& path, frameout::FrameOutError code, int result) {
    frameout::recordError(path, code);
    return result;
}

int openDemuxer(const std::string& path, Demuxer& d) {
    frameout::clearError(path);
    frameout::OpenGuard guard(&d.format);
    int rc = guard.open(path.c_str());
    if (rc < 0) return failDemuxer(path, frameout::classifyOpenInput(rc), kOpenFailed);
    rc = guard.findStreamInfo();
    if (rc < 0) return failDemuxer(path, frameout::classifyOpenInput(rc), kOpenFailed);
    d.stream = av_find_best_stream(d.format, AVMEDIA_TYPE_VIDEO, -1, -1, nullptr, 0);
    if (d.stream < 0) return failDemuxer(path, frameout::classifyNoStream(d.format, d.stream), kNoVideo);
    if (frameout::streamEncrypted(d.format->streams[d.stream])) return failDemuxer(path, frameout::FrameOutError::Drm, kOpenFailed);
    d.packet = av_packet_alloc();
    return d.packet ? 0 : failDemuxer(path, frameout::FrameOutError::OutOfMemory, kOpenFailed);
}

std::int64_t packetPtsUs(const AVPacket* packet, const AVStream* stream, std::int64_t previousUs) {
    const std::int64_t raw = packet->pts != AV_NOPTS_VALUE ? packet->pts : packet->dts;
    if (raw == AV_NOPTS_VALUE) return previousUs >= 0 ? previousUs + 1 : 0;
    return av_rescale_q(raw, stream->time_base, AVRational{1, 1000000});
}

bool usable(const AVPacket* packet, int stream) {
    if (packet->stream_index != stream || packet->size <= 0) return false;
    return (packet->flags & AV_PKT_FLAG_DISCARD) == 0;
}

int readPackets(Demuxer& d, std::vector<PacketEntry>& entries, const CancelFlag& cancel, frameout::FrameOutError& failure, std::size_t maxPackets, bool& truncated) {
    const AVStream* stream = d.format->streams[d.stream];
    const bool intraOnly = isIntraOnlyStream(stream);
    std::int64_t previousUs = -1;
    int rc;
    while ((rc = av_read_frame(d.format, d.packet)) >= 0) {
        if (isCancelled(cancel)) {
            av_packet_unref(d.packet);
            return kCancelled;
        }
        if (usable(d.packet, d.stream)) {
            const std::int64_t pts = packetPtsUs(d.packet, stream, previousUs);
            entries.push_back(PacketEntry{pts, intraOnly || (d.packet->flags & AV_PKT_FLAG_KEY) != 0});
            previousUs = pts;
        }
        av_packet_unref(d.packet);
        if (maxPackets > 0 && entries.size() >= maxPackets) {
            truncated = true;
            break;
        }
    }
    if (!truncated && rc != AVERROR_EOF) failure = frameout::classifyDecodeError(rc);
    return 0;
}

void toPresentationOrder(std::vector<PacketEntry>& entries) {
    std::stable_sort(entries.begin(), entries.end(), [](const PacketEntry& a, const PacketEntry& b) {
        return a.ptsUs < b.ptsUs;
    });
}

void emitIndexed(const std::vector<PacketEntry>& entries, std::vector<frameout::IndexedFrame>& out) {
    out.reserve(entries.size());
    for (const PacketEntry& entry : entries) {
        frameout::IndexedFrame frame;
        frame.ptsUs = entry.ptsUs;
        frame.keyframe = entry.keyframe;
        if (entry.keyframe && entry.ptsUs >= 0) {
            frame.nearestKeyframeIndex = static_cast<int>(out.size());
            frame.nearestKeyframePtsUs = entry.ptsUs;
        } else if (!out.empty()) {
            frame.nearestKeyframeIndex = out.back().nearestKeyframeIndex;
            frame.nearestKeyframePtsUs = out.back().nearestKeyframePtsUs;
        }
        out.push_back(frame);
    }
}
}
#endif

namespace frameout {
int indexFrames(const std::string& path, std::vector<IndexedFrame>& out, long long cancelToken) {
    return indexFramesCancellable(path, out, flagFor(cancelToken, false));
}

int indexFramesCancellable(const std::string& path, std::vector<IndexedFrame>& out, const std::shared_ptr<std::atomic<bool>>& cancel) {
    bool complete = true;
    return indexImpl(path, out, cancel, 0, complete);
}

int indexFramesPrefix(const std::string& path, std::vector<IndexedFrame>& out, int maxPackets, bool& complete) {
    return indexImpl(path, out, nullptr, maxPackets > 0 ? static_cast<std::size_t>(maxPackets) : 0, complete);
}

int indexImpl(const std::string& path, std::vector<IndexedFrame>& out, const std::shared_ptr<std::atomic<bool>>& cancel, std::size_t maxPackets, bool& complete) {
#if FRAMEOUT_HAS_FFMPEG
    complete = true;
    if (isCancelled(cancel)) return kCancelled;
    int gridWidth = 0, gridHeight = 0;
    if (tileGridSize(path, gridWidth, gridHeight)) {
        IndexedFrame still;
        still.ptsUs = 0;
        still.keyframe = true;
        still.nearestKeyframeIndex = 0;
        still.nearestKeyframePtsUs = 0;
        out.push_back(still);
        return 1;
    }
    Demuxer demuxer;
    const int opened = openDemuxer(path, demuxer);
    if (opened < 0) return opened;
    std::vector<PacketEntry> entries;
    frameout::FrameOutError failure = frameout::FrameOutError::None;
    bool truncated = false;
    const int read = readPackets(demuxer, entries, cancel, failure, maxPackets, truncated);
    if (read < 0) return read;
    if (entries.empty()) return failDemuxer(path, failure == frameout::FrameOutError::None ? frameout::FrameOutError::CorruptData : failure, kOpenFailed);
    if (entries.size() > static_cast<std::size_t>(INT_MAX)) return kTooLarge;
    toPresentationOrder(entries);
    if (truncated) {
        complete = false;
        entries.resize(entries.size() > kReorderTail ? entries.size() - kReorderTail : 0);
        if (entries.empty()) return 0;
    }
    emitIndexed(entries, out);
    return static_cast<int>(out.size());
#else
    (void)path; (void)out; (void)cancel; (void)maxPackets; complete = true;
    return -10;
#endif
}
}
