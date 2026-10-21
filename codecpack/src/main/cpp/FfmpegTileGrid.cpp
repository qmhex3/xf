#include "FfmpegTileGrid.h"
#include <cstdio>
#include <cstring>
#include <map>
#include <vector>
#include "FfmpegColor.h"
#if FRAMEOUT_HAS_FFMPEG
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
}
#include "FfmpegOpenGuard.h"
#endif

bool isAvifStill(const std::string& path) {
    std::FILE* file = std::fopen(path.c_str(), "rb");
    if (!file) return false;
    unsigned char head[12] = {0};
    const std::size_t read = std::fread(head, 1, sizeof(head), file);
    std::fclose(file);
    return read == sizeof(head) && std::memcmp(head + 4, "ftyp", 4) == 0 && std::memcmp(head + 8, "avif", 4) == 0;
}

#if FRAMEOUT_HAS_FFMPEG && LIBAVFORMAT_VERSION_MAJOR >= 61
namespace {
constexpr unsigned kMaxTiles = 1024;
constexpr long long kMaxCanvasPixels = 64LL * 1000 * 1000;

struct Grid {
    AVFormatContext* format = nullptr;
    const AVStreamGroup* group = nullptr;
    const AVStreamGroupTileGrid* spec = nullptr;
    int width = 0;
    int height = 0;
    ~Grid() { if (format) avformat_close_input(&format); }
};

bool openGrid(const std::string& path, Grid& g) {
    frameout::OpenGuard guard(&g.format);
    if (guard.open(path.c_str()) < 0) return false;
    if (guard.findStreamInfo() < 0) return false;
    for (unsigned i = 0; i < g.format->nb_stream_groups; ++i) {
        const AVStreamGroup* group = g.format->stream_groups[i];
        if (group->type != AV_STREAM_GROUP_PARAMS_TILE_GRID || !group->params.tile_grid) continue;
        const AVStreamGroupTileGrid* spec = group->params.tile_grid;
        const bool cropped = spec->width > 0 && spec->height > 0 &&
            spec->horizontal_offset >= 0 && spec->vertical_offset >= 0 &&
            spec->horizontal_offset + spec->width <= spec->coded_width &&
            spec->vertical_offset + spec->height <= spec->coded_height;
        g.width = cropped ? spec->width : spec->coded_width;
        g.height = cropped ? spec->height : spec->coded_height;
        if (g.width <= 0 || g.height <= 0 || spec->coded_width <= 0 || spec->coded_height <= 0) continue;
        if (spec->nb_tiles == 0 || spec->nb_tiles > kMaxTiles || group->nb_streams == 0) continue;
        if (static_cast<long long>(spec->coded_width) * spec->coded_height > kMaxCanvasPixels) continue;
        g.group = group;
        g.spec = spec;
        return true;
    }
    return false;
}

bool collectPackets(Grid& g, std::map<int, AVPacket*>& packets) {
    std::map<int, bool> wanted;
    for (unsigned i = 0; i < g.group->nb_streams; ++i) wanted[g.group->streams[i]->index] = true;
    AVPacket* packet = av_packet_alloc();
    if (!packet) return false;
    while (packets.size() < wanted.size() && av_read_frame(g.format, packet) >= 0) {
        if (wanted.count(packet->stream_index) && !packets.count(packet->stream_index) && packet->size > 0) {
            packets[packet->stream_index] = av_packet_clone(packet);
        }
        av_packet_unref(packet);
    }
    av_packet_free(&packet);
    return packets.size() == wanted.size();
}

struct TileScaler {
    SwsContext* scaler = nullptr;
    int width = 0;
    int height = 0;
    std::int64_t key = -1;
    ~TileScaler() { releaseScaler(scaler); }
};

bool decodeTile(const AVStream* stream, const AVPacket* packet, TileScaler& s, std::vector<std::uint8_t>& rgba, int& width, int& height) {
    const AVCodec* codec = avcodec_find_decoder(stream->codecpar->codec_id);
    if (!codec) return false;
    AVCodecContext* context = avcodec_alloc_context3(codec);
    AVFrame* frame = av_frame_alloc();
    bool ok = false;
    if (context && frame && avcodec_parameters_to_context(context, stream->codecpar) >= 0 && avcodec_open2(context, codec, nullptr) >= 0) {
        if (avcodec_send_packet(context, packet) >= 0) {
            avcodec_send_packet(context, nullptr);
            if (avcodec_receive_frame(context, frame) >= 0) {
                width = frame->width;
                height = frame->height;
                applyCodecColorDefaults(frame, context->codec_id);
                ok = convertFrame(frame, width, height, 0, false, s.scaler, s.width, s.height, s.key, rgba, nullptr, nullptr, 0.0f);
            }
        }
    }
    if (frame) av_frame_free(&frame);
    if (context) avcodec_free_context(&context);
    return ok;
}

void blit(std::vector<std::uint8_t>& canvas, int canvasWidth, int canvasHeight, const std::vector<std::uint8_t>& tile, int tileWidth, int tileHeight, int x, int y) {
    for (int row = 0; row < tileHeight; ++row) {
        const int cy = y + row;
        if (cy < 0 || cy >= canvasHeight) continue;
        const int from = x < 0 ? -x : 0;
        const int to = x + tileWidth > canvasWidth ? canvasWidth - x : tileWidth;
        if (to <= from) continue;
        std::memcpy(canvas.data() + (static_cast<std::size_t>(cy) * canvasWidth + x + from) * 4,
                    tile.data() + (static_cast<std::size_t>(row) * tileWidth + from) * 4,
                    static_cast<std::size_t>(to - from) * 4);
    }
}

bool compose(Grid& g, std::vector<std::uint8_t>& canvas) {
    std::map<int, AVPacket*> packets;
    bool ok = collectPackets(g, packets);
    const int cw = g.spec->coded_width;
    const int ch = g.spec->coded_height;
    canvas.assign(static_cast<std::size_t>(cw) * ch * 4, 0);
    for (std::size_t i = 0; i < canvas.size(); i += 4) std::memcpy(canvas.data() + i, g.spec->background, 4);
    TileScaler scaler;
    std::vector<std::uint8_t> tile;
    for (unsigned i = 0; ok && i < g.spec->nb_tiles; ++i) {
        const unsigned idx = g.spec->offsets[i].idx;
        if (idx >= g.group->nb_streams) { ok = false; break; }
        const AVStream* stream = g.group->streams[idx];
        int tw = 0, th = 0;
        ok = decodeTile(stream, packets[stream->index], scaler, tile, tw, th);
        if (ok) blit(canvas, cw, ch, tile, tw, th, g.spec->offsets[i].horizontal, g.spec->offsets[i].vertical);
    }
    for (auto& entry : packets) av_packet_free(&entry.second);
    return ok;
}

bool resizeInto(const std::uint8_t* source, int sourceStride, int sw, int sh, std::uint8_t* target, int tw, int th) {
    if (sw == tw && sh == th) {
        for (int row = 0; row < th; ++row) std::memcpy(target + static_cast<std::size_t>(row) * tw * 4, source + static_cast<std::size_t>(row) * sourceStride, static_cast<std::size_t>(tw) * 4);
        return true;
    }
    SwsContext* scaler = sws_getContext(sw, sh, AV_PIX_FMT_RGBA, tw, th, AV_PIX_FMT_RGBA, SWS_BILINEAR, nullptr, nullptr, nullptr);
    if (!scaler) return false;
    const std::uint8_t* planes[4] = { source, nullptr, nullptr, nullptr };
    const int strides[4] = { sourceStride, 0, 0, 0 };
    std::uint8_t* outPlanes[4] = { target, nullptr, nullptr, nullptr };
    const int outStrides[4] = { tw * 4, 0, 0, 0 };
    const int rows = sws_scale(scaler, planes, strides, 0, sh, outPlanes, outStrides);
    sws_freeContext(scaler);
    return rows == th;
}
}
#endif

bool tileGridSize(const std::string& path, int& width, int& height) {
#if FRAMEOUT_HAS_FFMPEG && LIBAVFORMAT_VERSION_MAJOR >= 61
    if (!isAvifStill(path)) return false;
    Grid g;
    if (!openGrid(path, g)) return false;
    width = g.width;
    height = g.height;
    return true;
#else
    (void)path; (void)width; (void)height;
    return false;
#endif
}

int decodeTileGrid(const std::string& path, frameout::DecodedFrame& out) {
#if FRAMEOUT_HAS_FFMPEG && LIBAVFORMAT_VERSION_MAJOR >= 61
    if (out.width <= 0 || out.height <= 0) return -1;
    Grid g;
    if (!openGrid(path, g)) return -2;
    std::vector<std::uint8_t> canvas;
    if (!compose(g, canvas)) return -5;
    const bool cropped = g.width != g.spec->coded_width || g.height != g.spec->coded_height;
    const int x0 = cropped ? g.spec->horizontal_offset : 0;
    const int y0 = cropped ? g.spec->vertical_offset : 0;
    const int stride = g.spec->coded_width * 4;
    const std::uint8_t* origin = canvas.data() + static_cast<std::size_t>(y0) * stride + static_cast<std::size_t>(x0) * 4;
    std::uint8_t* target = out.external;
    if (!target) {
        out.rgba.resize(static_cast<std::size_t>(out.width) * out.height * 4);
        target = out.rgba.data();
    }
    if (!resizeInto(origin, stride, g.width, g.height, target, out.width, out.height)) return -5;
    out.ptsUs = 0;
    out.keyframe = true;
    out.nearestKeyframeIndex = 0;
    out.nearestKeyframePtsUs = 0;
    return 0;
#else
    (void)path; (void)out;
    return -10;
#endif
}
