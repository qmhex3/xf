#pragma once

struct AVStream;

struct DisplayGeometry {
    int width = 0;
    int height = 0;
    int rotation = 0;
    bool flipH = false;
};

DisplayGeometry displayGeometry(const AVStream* stream);
