#include "FfmpegDisplay.h"
#if FRAMEOUT_HAS_FFMPEG
#include <cmath>
#include <cstdint>
#include <cstdlib>
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/display.h>
}

namespace {
int snapClockwise(double degrees) {
    const long rounded = std::lround(degrees);
    const int normalized = static_cast<int>(((rounded % 360) + 360) % 360);
    return ((normalized + 45) / 90 % 4) * 90;
}

const std::uint8_t* displayMatrix(const AVStream* stream) {
#if LIBAVCODEC_VERSION_INT >= AV_VERSION_INT(60, 31, 102)
    const AVCodecParameters* par = stream->codecpar;
    const AVPacketSideData* data = av_packet_side_data_get(par->coded_side_data, par->nb_coded_side_data, AV_PKT_DATA_DISPLAYMATRIX);
    return data ? data->data : nullptr;
#else
    return av_stream_get_side_data(stream, AV_PKT_DATA_DISPLAYMATRIX, nullptr);
#endif
}

struct Orientation {
    int rotation = 0;
    bool flipH = false;
};

Orientation streamOrientation(const AVStream* stream) {
    Orientation o;
    const std::uint8_t* matrix = displayMatrix(stream);
    if (matrix) {
        const auto* m = reinterpret_cast<const std::int32_t*>(matrix);
        const double a = m[0], b = m[1], c = m[3], d = m[4];
        if (a * d - b * c < 0.0) {
            o.flipH = true;
            if (a != 0.0 || b != 0.0) {
                const double clockwise = std::atan2(-b, -a) * 180.0 / 3.14159265358979323846;
                o.rotation = snapClockwise(clockwise);
                return o;
            }
        } else {
            const double counterClockwise = av_display_rotation_get(m);
            if (!std::isnan(counterClockwise)) {
                o.rotation = snapClockwise(-counterClockwise);
                return o;
            }
        }
    }
    const AVDictionaryEntry* tag = av_dict_get(stream->metadata, "rotate", nullptr, 0);
    o.rotation = tag ? snapClockwise(std::atof(tag->value)) : 0;
    return o;
}

AVRational sampleAspect(const AVStream* stream) {
    AVRational sar = stream->sample_aspect_ratio;
    if (sar.num <= 0 || sar.den <= 0) sar = stream->codecpar->sample_aspect_ratio;
    if (sar.num <= 0 || sar.den <= 0) return AVRational{1, 1};
    return sar;
}
}

DisplayGeometry displayGeometry(const AVStream* stream) {
    DisplayGeometry g;
    const AVRational sar = sampleAspect(stream);
    const int codedWidth = stream->codecpar->width;
    const int codedHeight = stream->codecpar->height;
    const long stretched = std::lround(static_cast<double>(codedWidth) * sar.num / sar.den);
    const int displayWidth = static_cast<int>(stretched < 1 ? 1 : stretched);
    const Orientation orientation = streamOrientation(stream);
    g.rotation = orientation.rotation;
    g.flipH = orientation.flipH;
    const bool turned = g.rotation == 90 || g.rotation == 270;
    g.width = turned ? codedHeight : displayWidth;
    g.height = turned ? displayWidth : codedHeight;
    return g;
}
#endif
