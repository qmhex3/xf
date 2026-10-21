#include "FfmpegHdr.h"
#include <cstdio>
#include <cstdlib>
#include <vector>
int main(int argc, char** argv) {
    int w = atoi(argv[1]), h = atoi(argv[2]);
    HdrParams p; p.hlg = atoi(argv[3]); p.primaries = atoi(argv[4]); p.peakNits = atof(argv[5]);
    std::vector<std::uint16_t> in(w * h * 4);
    FILE* f = fopen(argv[6], "rb"); fread(in.data(), 2, in.size(), f); fclose(f);
    std::vector<std::uint8_t> out(w * h * 4);
    std::vector<std::uint16_t> gain(w * h * 4);
    toneMapHdr16(in.data(), w, h, p, out.data(), gain.data());
    f = fopen(argv[7], "wb"); fwrite(out.data(), 1, out.size(), f); fwrite(gain.data(), 2, gain.size(), f); fclose(f);
}
