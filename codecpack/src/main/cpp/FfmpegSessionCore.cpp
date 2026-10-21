#include "FfmpegSessionCore.h"
#include <atomic>
#include <cstdlib>
#include <string>
#include <vector>
#include "FfmpegCodecTraits.h"
#include "FfmpegDisplay.h"
#if FRAMEOUT_HAS_FFMPEG
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/cpu.h>
#include <libavutil/dovi_meta.h>
#include <libavutil/frame.h>
#include <libavutil/mastering_display_metadata.h>
}
#include "FfmpegOpenGuard.h"
namespace frameout_session {
constexpr std::int64_t kPtsToleranceUs = 2;
std::atomic<bool> gainmapEnabled{false};
void closeDecoder(DecoderContext& d) {
    releaseScaler(d.scaler);
    if (d.packet) av_packet_free(&d.packet);
    if (d.frame) av_frame_free(&d.frame);
    if (d.codec) avcodec_free_context(&d.codec);
    if (d.format) avformat_close_input(&d.format);
    d.stream = -1; d.scalerWidth = 0; d.scalerHeight = 0; d.scalerFormat = -1;
    d.nearestKeyframeIndex = -1; d.nearestKeyframePtsUs = -1;
    d.rotation = 0; d.flipH = false; d.intraOnly = false; d.leadingFloorUs = -1; d.expectedKeyPtsUs = -1;
    d.streamPeakNits = 0.0f;
    d.dolbyProfile = 0;
    d.lastPtsUs = -1;
}
int decoderThreads() {
    const int cores = av_cpu_count();
    return cores < 1 ? 1 : (cores > 4 ? 4 : cores);
}
float streamPeak(const AVStream* stream) {
    const AVCodecParameters* par = stream->codecpar;
    for (int i = 0; i < par->nb_coded_side_data; ++i) {
        const AVPacketSideData& sd = par->coded_side_data[i];
        if (sd.type == AV_PKT_DATA_CONTENT_LIGHT_LEVEL && sd.size >= static_cast<std::size_t>(sizeof(AVContentLightMetadata))) {
            const auto maxCll = reinterpret_cast<const AVContentLightMetadata*>(sd.data)->MaxCLL;
            if (maxCll > 0) return static_cast<float>(maxCll);
        }
    }
    for (int i = 0; i < par->nb_coded_side_data; ++i) {
        const AVPacketSideData& sd = par->coded_side_data[i];
        if (sd.type == AV_PKT_DATA_MASTERING_DISPLAY_METADATA && sd.size >= static_cast<std::size_t>(sizeof(AVMasteringDisplayMetadata))) {
            const auto* m = reinterpret_cast<const AVMasteringDisplayMetadata*>(sd.data);
            if (m->has_luminance && m->max_luminance.den > 0) return static_cast<float>(m->max_luminance.num) / static_cast<float>(m->max_luminance.den);
        }
    }
    return 0.0f;
}
int dolbyProfileOf(const AVStream* stream) {
    const AVCodecParameters* par = stream->codecpar;
    for (int i = 0; i < par->nb_coded_side_data; ++i) {
        const AVPacketSideData& sd = par->coded_side_data[i];
        if (sd.type == AV_PKT_DATA_DOVI_CONF && sd.size >= static_cast<std::size_t>(sizeof(AVDOVIDecoderConfigurationRecord))) {
            return reinterpret_cast<const AVDOVIDecoderConfigurationRecord*>(sd.data)->dv_profile;
        }
    }
    return 0;
}
bool failOpen(const std::string& path, frameout::FrameOutError code, DecoderContext& d) {
    frameout::recordError(path, code);
    closeDecoder(d);
    return false;
}
bool openCodec(const std::string& path, DecoderContext& d) {
    AVStream* stream = d.format->streams[d.stream];
    if (frameout::streamEncrypted(stream)) return failOpen(path, frameout::FrameOutError::Drm, d);
    const AVCodec* codec = avcodec_find_decoder(stream->codecpar->codec_id);
    if (!codec) return failOpen(path, frameout::FrameOutError::DecoderMissing, d);
    d.codec = avcodec_alloc_context3(codec);
    if (!d.codec) return failOpen(path, frameout::FrameOutError::OutOfMemory, d);
    int rc = avcodec_parameters_to_context(d.codec, stream->codecpar);
    if (rc < 0) return failOpen(path, frameout::classifyOpenCodec(rc, stream), d);
    d.codec->thread_count = decoderThreads();
    d.codec->thread_type = FF_THREAD_FRAME | FF_THREAD_SLICE;
#ifdef AV_CODEC_EXPORT_DATA_DOVI
    d.codec->export_side_data |= AV_CODEC_EXPORT_DATA_DOVI;
#endif
    rc = avcodec_open2(d.codec, codec, nullptr);
    if (rc < 0) return failOpen(path, frameout::classifyOpenCodec(rc, stream), d);
    return true;
}
bool openDecoder(const std::string& path, DecoderContext& d) {
    frameout::clearError(path);
    frameout::OpenGuard guard(&d.format);
    int rc = guard.open(path.c_str());
    if (rc < 0) return failOpen(path, frameout::classifyOpenInput(rc), d);
    rc = guard.findStreamInfo();
    if (rc < 0) return failOpen(path, frameout::classifyOpenInput(rc), d);
    const int best = av_find_best_stream(d.format, AVMEDIA_TYPE_VIDEO, -1, -1, nullptr, 0);
    if (best < 0) return failOpen(path, frameout::classifyNoStream(d.format, best), d);
    d.stream = best;
    if (!frameout::dimensionsAllowed(d.format->streams[best]->codecpar)) return failOpen(path, frameout::FrameOutError::UnsupportedFeature, d);
    if (!openCodec(path, d)) return false;
    d.streamPeakNits = streamPeak(d.format->streams[d.stream]);
    d.dolbyProfile = dolbyProfileOf(d.format->streams[d.stream]);
    const DisplayGeometry geometry = displayGeometry(d.format->streams[d.stream]);
    d.rotation = geometry.rotation;
    d.flipH = geometry.flipH;
    d.intraOnly = isIntraOnlyStream(d.format->streams[d.stream]);
    d.packet = av_packet_alloc(); d.frame = av_frame_alloc();
    if (!d.packet || !d.frame) return failOpen(path, frameout::FrameOutError::OutOfMemory, d);
    return true;
}
bool isKeyFrame(const AVFrame* frame, const DecoderContext& d) {
    return d.intraOnly || (frame->flags & AV_FRAME_FLAG_KEY) != 0;
}
std::int64_t framePts(AVFrame* frame, const DecoderContext& d) {
    if (frame->best_effort_timestamp == AV_NOPTS_VALUE) return -1;
    return av_rescale_q(frame->best_effort_timestamp, d.format->streams[d.stream]->time_base, AVRational{1, 1000000});
}
bool resetDecoder(DecoderContext& d) {
    if (!d.format || !d.codec || av_seek_frame(d.format, d.stream, 0, AVSEEK_FLAG_BACKWARD) < 0) return false;
    avcodec_flush_buffers(d.codec); d.nearestKeyframeIndex = -1; d.nearestKeyframePtsUs = -1; d.leadingFloorUs = -1; d.expectedKeyPtsUs = -1; d.lastPtsUs = -1; return true;
}
bool jumpToKeyframe(DecoderContext& d, int& sessionIndex, int index, int hintIndex, std::int64_t hintPtsUs, bool force) {
    if (hintIndex < 0 || hintPtsUs < 0 || hintIndex > index) return false;
    const bool ahead = hintIndex > sessionIndex + 1;
    const bool behind = index <= sessionIndex;
    if (!force && !ahead && !behind) return false;
    const auto target = av_rescale_q(hintPtsUs, AVRational{1, 1000000}, d.format->streams[d.stream]->time_base);
    if (av_seek_frame(d.format, d.stream, target, AVSEEK_FLAG_BACKWARD) < 0) return false;
    avcodec_flush_buffers(d.codec);
    d.leadingFloorUs = hintPtsUs - 2;
    d.expectedKeyPtsUs = hintPtsUs;
    d.nearestKeyframeIndex = -1; d.nearestKeyframePtsUs = -1;
    sessionIndex = hintIndex - 1;
    d.lastPtsUs = hintPtsUs - 1;
    return true;
}
int interruptCallback(void* opaque) {
    return cancelRequested(*static_cast<const DecoderContext*>(opaque)) ? 1 : 0;
}
void armInterrupt(DecoderContext& d) {
    if (d.format) d.format->interrupt_callback = AVIOInterruptCB{&interruptCallback, &d};
}
bool loadTable(const char* path, DecoderContext& d) {
    if (d.tableLoaded) return !d.table.empty();
    d.tableLoaded = true;
    if (frameout::indexFramesCancellable(path, d.table, d.cancel) < 0) {
        d.table.clear();
        if (cancelRequested(d)) d.tableLoaded = false;
    }
    return !d.table.empty();
}
enum class Landing { Skip, Accept, Missed };
Landing checkLanding(DecoderContext& d, std::int64_t pts) {
    if (d.leadingFloorUs < 0) return Landing::Accept;
    if (pts >= 0 && pts < d.leadingFloorUs) return Landing::Skip;
    const bool missed = d.expectedKeyPtsUs >= 0 && pts >= 0 && std::llabs(pts - d.expectedKeyPtsUs) > 2;
    d.leadingFloorUs = -1; d.expectedKeyPtsUs = -1;
    return missed ? Landing::Missed : Landing::Accept;
}
int decodePackets(DecoderContext& d, int index, int& currentIndex, frameout::DecodedFrame& out, std::int64_t targetPts) {
    const bool byPts = targetPts >= 0;
    bool flushed = false;
    int errors = 0;
    while (true) {
        if (cancelRequested(d)) return kCancelledRc;
        const int received = avcodec_receive_frame(d.codec, d.frame);
        if (received >= 0) {
            errors = 0;
            const auto pts = framePts(d.frame, d);
            const Landing landing = checkLanding(d, pts);
            if (landing == Landing::Skip) { av_frame_unref(d.frame); continue; }
            if (landing == Landing::Missed) { av_frame_unref(d.frame); return -8; }
            const int decodedIndex = currentIndex++;
            if (pts >= 0) d.lastPtsUs = pts;
            if (!byPts && isKeyFrame(d.frame, d) && pts >= 0) { d.nearestKeyframeIndex = decodedIndex; d.nearestKeyframePtsUs = pts; }
            const bool hit = byPts ? (pts >= 0 && pts >= targetPts - kPtsToleranceUs) : decodedIndex == index;
            if (hit) {
                out.ptsUs = pts; out.keyframe = isKeyFrame(d.frame, d);
                applyCodecColorDefaults(d.frame, d.codec->codec_id);
                const bool converted = convertFrame(d.frame, out.width, out.height, d.rotation, d.flipH, d.scaler, d.scalerWidth, d.scalerHeight, d.scalerFormat, out.rgba, out.external, gainmapEnabled.load() ? &out.gain : nullptr, d.streamPeakNits, d.dolbyProfile, frameout::signalEnabled() ? &out.signal : nullptr);
                av_frame_unref(d.frame); return converted ? 1 : -5;
            }
            av_frame_unref(d.frame); continue;
        }
        if (received == AVERROR_EOF) return 0;
        if (received != AVERROR(EAGAIN)) {
            d.failure = frameout::classifyDecodeError(received);
            if (++errors > kMaxConsecutiveErrors) return -6;
            if (!byPts && currentIndex++ == index) return -5;
            continue;
        }
        if (flushed) return 0;
        const int read = av_read_frame(d.format, d.packet);
        if (read < 0) { avcodec_send_packet(d.codec, nullptr); flushed = true; continue; }
        if (d.packet->stream_index == d.stream) {
            if (frameout::packetEncrypted(d.packet)) { av_packet_unref(d.packet); d.failure = frameout::FrameOutError::Drm; return -6; }
            const int sent = avcodec_send_packet(d.codec, d.packet);
            if (sent < 0 && sent != AVERROR(EAGAIN)) {
                d.failure = frameout::classifyDecodeError(sent);
                const std::int64_t raw = d.packet->pts != AV_NOPTS_VALUE ? d.packet->pts : d.packet->dts;
                const std::int64_t badPts = raw == AV_NOPTS_VALUE ? -1 : av_rescale_q(raw, d.format->streams[d.stream]->time_base, AVRational{1, 1000000});
                av_packet_unref(d.packet);
                if (++errors > kMaxConsecutiveErrors) return -6;
                if (!byPts && currentIndex++ == index) { out.ptsUs = badPts; out.keyframe = false; return -5; }
                continue;
            }
        }
        av_packet_unref(d.packet);
    }
}
}
#endif
