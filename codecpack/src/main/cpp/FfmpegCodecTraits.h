#pragma once
#if FRAMEOUT_HAS_FFMPEG
extern "C" {
#include <libavcodec/codec_desc.h>
#include <libavformat/avformat.h>
}

inline bool isIntraOnlyStream(const AVStream* stream) {
    if (!stream || !stream->codecpar) return false;
    const AVCodecDescriptor* descriptor = avcodec_descriptor_get(stream->codecpar->codec_id);
    return descriptor && (descriptor->props & AV_CODEC_PROP_INTRA_ONLY) != 0;
}
#endif
