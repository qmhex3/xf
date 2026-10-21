#include "FfmpegError.h"
#include <map>
#include <mutex>
#include <new>

#if FRAMEOUT_HAS_FFMPEG
#include <cerrno>
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/error.h>
}
#endif

namespace {
constexpr std::size_t kMaxTracked = 64;
std::mutex errorMutex;
std::map<std::string, frameout::FrameOutError> errors;
}

namespace frameout {

void recordError(const std::string& path, FrameOutError code) {
    std::lock_guard<std::mutex> lock(errorMutex);
    if (errors.size() >= kMaxTracked && errors.find(path) == errors.end()) errors.clear();
    errors[path] = code;
}

void clearError(const std::string& path) {
    std::lock_guard<std::mutex> lock(errorMutex);
    errors.erase(path);
}

FrameOutError lastError(const std::string& path) {
    std::lock_guard<std::mutex> lock(errorMutex);
    auto it = errors.find(path);
    return it == errors.end() ? FrameOutError::None : it->second;
}

FrameOutError classifyException() {
    try {
        throw;
    } catch (const std::bad_alloc&) {
        return FrameOutError::OutOfMemory;
    } catch (...) {
        return FrameOutError::Internal;
    }
}

#if FRAMEOUT_HAS_FFMPEG

namespace {
FrameOutError fromErrno(int averror) {
    if (averror == AVERROR(ENOENT)) return FrameOutError::SourceMissing;
    if (averror == AVERROR(EACCES) || averror == AVERROR(EPERM)) return FrameOutError::PermissionDenied;
    if (averror == AVERROR(EIO) || averror == AVERROR(EPIPE) || averror == AVERROR(ESPIPE)) return FrameOutError::IoError;
    if (averror == AVERROR(ENOMEM)) return FrameOutError::OutOfMemory;
    return FrameOutError::None;
}

bool hasVideoStream(const AVFormatContext* format) {
    for (unsigned i = 0; i < format->nb_streams; ++i) {
        if (format->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) return true;
    }
    return false;
}

bool anyVideoEncrypted(const AVFormatContext* format) {
    for (unsigned i = 0; i < format->nb_streams; ++i) {
        const AVStream* stream = format->streams[i];
        if (stream->codecpar->codec_type == AVMEDIA_TYPE_VIDEO && streamEncrypted(stream)) return true;
    }
    return false;
}
}

bool streamEncrypted(const AVStream* stream) {
    if (!stream || !stream->codecpar) return false;
    const AVCodecParameters* par = stream->codecpar;
    if (av_packet_side_data_get(par->coded_side_data, par->nb_coded_side_data, AV_PKT_DATA_ENCRYPTION_INIT_INFO)) return true;
    return par->codec_tag == MKTAG('e', 'n', 'c', 'v');
}

bool packetEncrypted(const AVPacket* packet) {
    if (!packet) return false;
    return av_packet_side_data_get(packet->side_data, packet->side_data_elems, AV_PKT_DATA_ENCRYPTION_INFO) != nullptr;
}

FrameOutError classifyOpenInput(int averror) {
    const FrameOutError mapped = fromErrno(averror);
    if (mapped != FrameOutError::None) return mapped;
    if (averror == AVERROR_DEMUXER_NOT_FOUND) return FrameOutError::UnsupportedContainer;
    if (averror == AVERROR_INVALIDDATA) return FrameOutError::CorruptData;
    if (averror == AVERROR_PATCHWELCOME || averror == AVERROR_EXPERIMENTAL) return FrameOutError::UnsupportedFeature;
    return FrameOutError::CorruptData;
}

FrameOutError classifyDecodeError(int averror) {
    const FrameOutError mapped = fromErrno(averror);
    if (mapped != FrameOutError::None) return mapped;
    if (averror == AVERROR_PATCHWELCOME || averror == AVERROR_EXPERIMENTAL) return FrameOutError::UnsupportedFeature;
    if (averror == AVERROR_DECODER_NOT_FOUND) return FrameOutError::DecoderMissing;
    return FrameOutError::CorruptData;
}

FrameOutError classifyOpenCodec(int averror, const AVStream* stream) {
    if (streamEncrypted(stream)) return FrameOutError::Drm;
    const FrameOutError mapped = fromErrno(averror);
    if (mapped != FrameOutError::None) return mapped;
    if (averror == AVERROR_PATCHWELCOME || averror == AVERROR_EXPERIMENTAL) return FrameOutError::UnsupportedFeature;
    if (averror == AVERROR_DECODER_NOT_FOUND) return FrameOutError::DecoderMissing;
    if (averror == AVERROR_INVALIDDATA) return FrameOutError::CorruptData;
    return FrameOutError::DecoderInitFailed;
}

FrameOutError classifyNoStream(const AVFormatContext* format, int averror) {
    if (!hasVideoStream(format)) return FrameOutError::NoVideoStream;
    if (anyVideoEncrypted(format)) return FrameOutError::Drm;
    if (averror == AVERROR_DECODER_NOT_FOUND) return FrameOutError::DecoderMissing;
    return FrameOutError::NoVideoStream;
}

#else

bool streamEncrypted(const AVStream*) { return false; }
bool packetEncrypted(const AVPacket*) { return false; }
FrameOutError classifyOpenInput(int) { return FrameOutError::Unknown; }
FrameOutError classifyDecodeError(int) { return FrameOutError::Unknown; }
FrameOutError classifyOpenCodec(int, const AVStream*) { return FrameOutError::Unknown; }
FrameOutError classifyNoStream(const AVFormatContext*, int) { return FrameOutError::Unknown; }

#endif

}
