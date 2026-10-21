#pragma once
#include <cerrno>
#include <cstdint>
extern "C" {
#include <libavformat/avformat.h>
#include <libavutil/dict.h>
#include <libavutil/time.h>
}

namespace frameout {

constexpr int64_t kOpenTimeoutUs = 30LL * 1000000LL;
constexpr int64_t kProbeSizeBytes = 32LL * 1024 * 1024;
constexpr int64_t kAnalyzeDurationUs = 10LL * 1000000LL;
constexpr int kMaxSide = 16384;
constexpr int64_t kMaxPixels = 134217728LL;

struct OpenGuard {
    int64_t deadlineUs;
    AVFormatContext** target;

    explicit OpenGuard(AVFormatContext** format)
        : deadlineUs(av_gettime_relative() + kOpenTimeoutUs), target(format) {}
    OpenGuard(const OpenGuard&) = delete;
    OpenGuard& operator=(const OpenGuard&) = delete;
    ~OpenGuard() { release(); }

    static int expired(void* opaque) {
        return av_gettime_relative() > static_cast<OpenGuard*>(opaque)->deadlineUs ? 1 : 0;
    }

    void release() {
        if (target && *target) (*target)->interrupt_callback = AVIOInterruptCB{nullptr, nullptr};
    }

    int open(const char* path) {
        AVFormatContext* context = avformat_alloc_context();
        if (!context) return AVERROR(ENOMEM);
        context->interrupt_callback = AVIOInterruptCB{&OpenGuard::expired, this};
        AVDictionary* options = nullptr;
        av_dict_set_int(&options, "probesize", kProbeSizeBytes, 0);
        av_dict_set_int(&options, "analyzeduration", kAnalyzeDurationUs, 0);
        *target = context;
        int rc = avformat_open_input(target, path, nullptr, &options);
        av_dict_free(&options);
        return rc;
    }

    int findStreamInfo() { return avformat_find_stream_info(*target, nullptr); }
};

inline bool dimensionsAllowed(const AVCodecParameters* params) {
    if (!params || params->width <= 0 || params->height <= 0) return true;
    if (params->width > kMaxSide || params->height > kMaxSide) return false;
    return static_cast<int64_t>(params->width) * params->height <= kMaxPixels;
}

}
