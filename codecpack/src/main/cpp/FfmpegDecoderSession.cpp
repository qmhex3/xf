#include "FfmpegDecoderSession.h"
#include <mutex>
#include <map>
#include <memory>
#include <string>
#include <sys/stat.h>
#include <cstdlib>
#include <atomic>
#include <utility>
#include <vector>
#include "FfmpegColor.h"
#include "FfmpegCodecTraits.h"
#include "FfmpegDisplay.h"
#include "FfmpegError.h"
#include "FfmpegTileGrid.h"
#if FRAMEOUT_HAS_FFMPEG
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/cpu.h>
#include <libavutil/frame.h>
#include <libavutil/mastering_display_metadata.h>
}
#include "FfmpegOpenGuard.h"
#include "FfmpegSessionCore.h"
namespace {
using namespace frameout_session;
struct Slot {
    DecoderContext ctx;
    int index = -1;
    std::mutex mutex;
    long long dev = 0;
    long long ino = 0;
    long long size = 0;
    long long mtimeNs = 0;
    unsigned long long lastUsed = 0;
    frameout::GainMap gain;
    std::shared_ptr<std::atomic<bool>> cancel = std::make_shared<std::atomic<bool>>(false);
    int grid = -1;
    ~Slot() { closeDecoder(ctx); }
};
constexpr int kMissedRetries = 2;
int seekFailed(const std::string& path) {
    frameout::recordError(path, frameout::FrameOutError::IoError);
    return -3;
}
int decodeGridFrame(const std::string& path, int index, frameout::DecodedFrame& out) {
    if (index != 0) return -1;
    const int rc = decodeTileGrid(path, out);
    if (rc != 0) frameout::recordError(path, frameout::FrameOutError::CorruptData);
    return rc;
}
std::map<std::string, std::shared_ptr<Slot>> slots;
std::mutex slotsMutex;
unsigned long long slotClock = 0;
constexpr std::size_t kMaxSlots = 3;
void identityOf(const std::string& path, long long& dev, long long& ino, long long& size, long long& mtimeNs) {
    struct stat st;
    dev = ino = size = mtimeNs = 0;
    if (stat(path.c_str(), &st) != 0) return;
    dev = static_cast<long long>(st.st_dev);
    ino = static_cast<long long>(st.st_ino);
    size = static_cast<long long>(st.st_size);
    mtimeNs = static_cast<long long>(st.st_mtim.tv_sec) * 1000000000LL + static_cast<long long>(st.st_mtim.tv_nsec);
}
std::shared_ptr<Slot> acquire(const std::string& path) {
    long long dev, ino, size, mtimeNs;
    identityOf(path, dev, ino, size, mtimeNs);
    std::lock_guard<std::mutex> guard(slotsMutex);
    auto it = slots.find(path);
    if (it != slots.end()) {
        const auto& cur = it->second;
        if (cur->dev == dev && cur->ino == ino && cur->size == size && cur->mtimeNs == mtimeNs) { cur->lastUsed = ++slotClock; return cur; }
        slots.erase(it);
    }
    auto fresh = std::make_shared<Slot>();
    fresh->dev = dev; fresh->ino = ino; fresh->size = size; fresh->mtimeNs = mtimeNs;
    fresh->lastUsed = ++slotClock;
    slots[path] = fresh;
    while (slots.size() > kMaxSlots) {
        auto oldest = slots.end();
        for (auto cursor = slots.begin(); cursor != slots.end(); ++cursor) {
            if (cursor->second == fresh) continue;
            if (oldest == slots.end() || cursor->second->lastUsed < oldest->second->lastUsed) oldest = cursor;
        }
        if (oldest == slots.end()) break;
        slots.erase(oldest);
    }
    return fresh;
}
}
#endif
int seekSessionToKeyframe(const std::string& path, int keyframeIndex, std::int64_t keyframePtsUs) {
#if FRAMEOUT_HAS_FFMPEG
    if (keyframeIndex < 0 || keyframePtsUs < 0) return -1;
    auto slot = acquire(path);
    std::lock_guard<std::mutex> lock(slot->mutex);
    DecoderContext& session = slot->ctx;
    session.cancel = slot->cancel;
    if (cancelRequested(session)) return kCancelledRc;
    int& sessionIndex = slot->index;
    if (!session.format || !session.codec) {
        closeDecoder(session); sessionIndex = -1;
        if (!openDecoder(path, session)) return cancelRequested(session) ? kCancelledRc : -2;
        armInterrupt(session);
    }
    auto* stream = session.format->streams[session.stream];
    const auto target = av_rescale_q(keyframePtsUs, AVRational{1, 1000000}, stream->time_base);
    if (av_seek_frame(session.format, session.stream, target, AVSEEK_FLAG_BACKWARD) < 0) return seekFailed(path);
    avcodec_flush_buffers(session.codec);
    session.leadingFloorUs = -1; session.expectedKeyPtsUs = -1; session.lastPtsUs = -1;
    bool eof = false;
    int errors = 0;
    while (!eof) {
        if (cancelRequested(session)) return kCancelledRc;
        const int received = avcodec_receive_frame(session.codec, session.frame);
        if (received >= 0) {
            const auto pts = framePts(session.frame, session);
            if (isKeyFrame(session.frame, session) && pts >= 0 && std::llabs(pts - keyframePtsUs) <= 2) {
                av_frame_unref(session.frame); sessionIndex = keyframeIndex;
                session.nearestKeyframeIndex = keyframeIndex; session.nearestKeyframePtsUs = keyframePtsUs; session.lastPtsUs = keyframePtsUs; return 0;
            }
            av_frame_unref(session.frame); continue;
        }
        if (received == AVERROR_EOF) break;
        if (received != AVERROR(EAGAIN)) { if (++errors > kMaxConsecutiveErrors) return -4; continue; }
        const int read = av_read_frame(session.format, session.packet);
        if (read < 0) { avcodec_send_packet(session.codec, nullptr); eof = true; continue; }
        if (session.packet->stream_index == session.stream) {
            const int sent = avcodec_send_packet(session.codec, session.packet);
            if (sent < 0 && sent != AVERROR(EAGAIN) && ++errors > kMaxConsecutiveErrors) { av_packet_unref(session.packet); return -6; }
        }
        av_packet_unref(session.packet);
    }
    return -7;
#else
    return -10;
#endif
}
int decodeSession(const std::string& path, int index, frameout::DecodedFrame& out, int hintIndex, std::int64_t hintPtsUs) {
#if FRAMEOUT_HAS_FFMPEG
    if (index < 0 || out.width <= 0 || out.height <= 0) return -1;
    auto slot = acquire(path);
    std::lock_guard<std::mutex> lock(slot->mutex);
    slot->ctx.cancel = slot->cancel;
    if (slot->cancel->load()) return kCancelledRc;
    if (slot->grid < 0) {
        int gridWidth = 0, gridHeight = 0;
        slot->grid = tileGridSize(path, gridWidth, gridHeight) ? 1 : 0;
    }
    if (slot->grid == 1) return decodeGridFrame(path, index, out);
    DecoderContext& session = slot->ctx;
    int& sessionIndex = slot->index;
    if (!session.format || !session.codec) {
        closeDecoder(session); sessionIndex = -1;
        if (!openDecoder(path, session)) return cancelRequested(session) ? kCancelledRc : -2;
        armInterrupt(session);
    }
    session.failure = frameout::FrameOutError::None;
    const bool tabled = loadTable(path.c_str(), session) && index < static_cast<int>(session.table.size()) && session.table[index].ptsUs >= 0;
    std::int64_t targetPts = -1;
    int decoded;
    if (tabled) {
        const frameout::IndexedFrame& entry = session.table[index];
        targetPts = entry.ptsUs;
        const std::int64_t keyPts = entry.nearestKeyframePtsUs;
        const bool behind = session.lastPtsUs >= targetPts;
        const bool ahead = keyPts >= 0 && session.lastPtsUs >= 0 && keyPts > session.lastPtsUs + 1;
        bool jumped = false;
        if ((behind || ahead) && keyPts >= 0 && entry.nearestKeyframeIndex >= 0) {
            int ignored = sessionIndex;
            jumped = jumpToKeyframe(session, ignored, index, entry.nearestKeyframeIndex, keyPts, true);
            if (jumped) sessionIndex = ignored;
        }
        if (behind && !jumped) {
            if (!resetDecoder(session)) return seekFailed(path);
            sessionIndex = -1;
        }
        int current = 0;
        decoded = decodePackets(session, index, current, out, targetPts);
        std::int64_t retryBefore = keyPts;
        for (int attempt = 0; decoded == -8 && attempt < kMissedRetries; ++attempt) {
            int earlier = -1;
            for (int i = entry.nearestKeyframeIndex - 1; i >= 0; --i) {
                const frameout::IndexedFrame& e = session.table[i];
                if (e.keyframe && e.ptsUs >= 0 && e.ptsUs < retryBefore) { earlier = i; break; }
            }
            if (earlier < 0) break;
            const std::int64_t earlierPts = session.table[earlier].ptsUs;
            int ignored = sessionIndex;
            if (!jumpToKeyframe(session, ignored, index, earlier, earlierPts, true)) break;
            retryBefore = earlierPts;
            current = 0;
            decoded = decodePackets(session, index, current, out, targetPts);
        }
        if (decoded == -8) {
            if (!resetDecoder(session)) return seekFailed(path);
            current = 0;
            decoded = decodePackets(session, index, current, out, targetPts);
        }
        session.nearestKeyframeIndex = entry.nearestKeyframeIndex;
        session.nearestKeyframePtsUs = entry.nearestKeyframePtsUs;
    } else {
        if (!jumpToKeyframe(session, sessionIndex, index, hintIndex, hintPtsUs) && index <= sessionIndex) {
            if (!resetDecoder(session)) return seekFailed(path);
            sessionIndex = -1;
        }
        int current = sessionIndex + 1;
        decoded = decodePackets(session, index, current, out, -1);
        if (decoded == -8) {
            if (!resetDecoder(session)) return seekFailed(path);
            current = 0;
            decoded = decodePackets(session, index, current, out, -1);
        }
    }
    if (decoded == kCancelledRc) { sessionIndex = -1; return kCancelledRc; }
    out.nearestKeyframeIndex = session.nearestKeyframeIndex; out.nearestKeyframePtsUs = session.nearestKeyframePtsUs;
    if (decoded == 1) { sessionIndex = index; slot->gain = std::move(out.gain); frameout::keepSignal(path, std::move(out.signal)); return 0; }
    if (decoded == -5) { sessionIndex = index; slot->gain = frameout::GainMap(); frameout::dropSignal(path); return -5; }
    frameout::recordError(path, session.failure != frameout::FrameOutError::None ? session.failure : frameout::FrameOutError::CorruptData);
    session.failure = frameout::FrameOutError::None;
    closeDecoder(session); sessionIndex = -1; return -4;
#else
    return -10;
#endif
}
int installTable(const std::string& path, std::vector<frameout::IndexedFrame>&& table) {
#if FRAMEOUT_HAS_FFMPEG
    if (table.empty()) return -1;
    auto slot = acquire(path);
    std::lock_guard<std::mutex> lock(slot->mutex);
    slot->ctx.table = std::move(table);
    slot->ctx.tableLoaded = true;
    return 0;
#else
    (void)path; (void)table;
    return -10;
#endif
}
void setGainmapEnabled(bool enabled) {
#if FRAMEOUT_HAS_FFMPEG
    frameout_session::gainmapEnabled.store(enabled);
#else
    (void)enabled;
#endif
}
bool lastSessionGain(const std::string& path, frameout::GainMap& out) {
#if FRAMEOUT_HAS_FFMPEG
    std::shared_ptr<Slot> slot;
    {
        std::lock_guard<std::mutex> guard(slotsMutex);
        auto it = slots.find(path);
        if (it == slots.end()) return false;
        slot = it->second;
    }
    std::lock_guard<std::mutex> lock(slot->mutex);
    if (!slot->gain.valid) return false;
    out = slot->gain;
    return true;
#else
    (void)path; (void)out;
    return false;
#endif
}
void closeSession(const std::string& path) {
#if FRAMEOUT_HAS_FFMPEG
    std::shared_ptr<Slot> slot;
    {
        std::lock_guard<std::mutex> guard(slotsMutex);
        auto it = slots.find(path);
        if (it == slots.end()) return;
        slot = it->second;
        slots.erase(it);
    }
    slot->cancel->store(true);
    std::lock_guard<std::mutex> lock(slot->mutex);
    closeDecoder(slot->ctx);
    slot->index = -1;
    frameout::dropSignal(path);
#else
    (void)path;
#endif
}
void cancelSession(const std::string& path) {
#if FRAMEOUT_HAS_FFMPEG
    std::shared_ptr<Slot> slot;
    {
        std::lock_guard<std::mutex> guard(slotsMutex);
        auto it = slots.find(path);
        if (it == slots.end()) return;
        slot = it->second;
    }
    slot->cancel->store(true);
#else
    (void)path;
#endif
}
