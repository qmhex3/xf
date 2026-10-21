#include "FfmpegSignal.h"
#include <cstdio>
#include <cstdlib>
#include <string>
#include <thread>
#include <vector>

using namespace frameout_color;

static int failures = 0;
#define CHECK(cond) do { if (!(cond)) { std::printf("FAIL %s:%d %s\n", __FILE__, __LINE__, #cond); ++failures; } } while (0)

static std::vector<std::uint16_t> makeRgba(int w, int h) {
    std::vector<std::uint16_t> v(static_cast<std::size_t>(w) * h * 4);
    for (int y = 0; y < h; ++y)
        for (int x = 0; x < w; ++x) {
            std::uint16_t* p = &v[(static_cast<std::size_t>(y) * w + x) * 4];
            p[0] = static_cast<std::uint16_t>(1000 + y * 301 + x * 7);
            p[1] = static_cast<std::uint16_t>(2000 + y * 11 + x * 409);
            p[2] = static_cast<std::uint16_t>(65535 - y * 100 - x * 3);
            p[3] = 65535;
        }
    return v;
}

static void expectedXY(int x, int y, int w, int h, int rot, bool flip, int& dx, int& dy) {
    int fx = flip ? w - 1 - x : x;
    if (rot == 0) { dx = fx; dy = y; }
    else if (rot == 90) { dx = h - 1 - y; dy = fx; }
    else if (rot == 180) { dx = w - 1 - fx; dy = h - 1 - y; }
    else { dx = y; dy = w - 1 - fx; }
}

static void testRotations() {
    const int w = 7, h = 5;
    auto src = makeRgba(w, h);
    for (int rot : {0, 90, 180, 270}) {
        for (bool flip : {false, true}) {
            frameout::SignalFrame out;
            CHECK(packSignal(src.data(), w, h, rot, flip, 9, 16, out));
            const int ow = (rot == 90 || rot == 270) ? h : w;
            const int oh = (rot == 90 || rot == 270) ? w : h;
            CHECK(out.width == ow && out.height == oh && out.valid);
            CHECK(out.rgb.size() == static_cast<std::size_t>(ow) * oh * 3);
            int bad = 0;
            for (int y = 0; y < h; ++y)
                for (int x = 0; x < w; ++x) {
                    int dx, dy;
                    expectedXY(x, y, w, h, rot, flip, dx, dy);
                    const std::uint16_t* s = &src[(static_cast<std::size_t>(y) * w + x) * 4];
                    const std::uint16_t* d = &out.rgb[(static_cast<std::size_t>(dy) * ow + dx) * 3];
                    if (s[0] != d[0] || s[1] != d[1] || s[2] != d[2]) ++bad;
                }
            CHECK(bad == 0);
        }
    }
}

static void testRotateTypedMatchesPack() {
    const int w = 6, h = 4;
    auto src = makeRgba(w, h);
    const auto* wide = reinterpret_cast<const std::uint64_t*>(src.data());
    for (int rot : {0, 90, 180, 270}) {
        for (bool flip : {false, true}) {
            std::vector<std::uint64_t> typed(static_cast<std::size_t>(w) * h);
            rotateTyped(wide, w, h, rot, flip, typed.data());
            frameout::SignalFrame out;
            CHECK(packSignal(src.data(), w, h, rot, flip, 1, 16, out));
            const auto* t = reinterpret_cast<const std::uint16_t*>(typed.data());
            int bad = 0;
            for (std::size_t i = 0; i < typed.size(); ++i)
                for (int k = 0; k < 3; ++k) if (t[i * 4 + k] != out.rgb[i * 3 + k]) ++bad;
            CHECK(bad == 0);
        }
    }
}

static SignalGate goodGate() {
    SignalGate g;
    g.wide = true; g.rangeDeclared = true; g.colorspace = 9; g.primaries = 9; g.transfer = 16;
    return g;
}

static void testGate() {
    CHECK(signalExact(goodGate()));
    SignalGate g = goodGate(); g.transfer = 18; CHECK(signalExact(g));
    g = goodGate(); g.primaries = 12; CHECK(signalExact(g));
    g = goodGate(); g.primaries = 11; CHECK(signalExact(g));
    g = goodGate(); g.colorspace = 1; g.primaries = 1; CHECK(signalExact(g));
    g = goodGate(); g.wide = false; CHECK(!signalExact(g));
    g = goodGate(); g.dolbyFrame = true; CHECK(!signalExact(g));
    g = goodGate(); g.dolbyProfile = 8; CHECK(!signalExact(g));
    g = goodGate(); g.matrixDecode = true; CHECK(!signalExact(g));
    g = goodGate(); g.resampled = true; CHECK(!signalExact(g));
    g = goodGate(); g.rangeDeclared = false; CHECK(!signalExact(g));
    g = goodGate(); g.transfer = 2; CHECK(!signalExact(g));
    g = goodGate(); g.transfer = 1; CHECK(!signalExact(g));
    g = goodGate(); g.primaries = 2; CHECK(!signalExact(g));
    g = goodGate(); g.colorspace = 2; CHECK(!signalExact(g));
    g = goodGate(); g.colorspace = 10; CHECK(!signalExact(g));
    g = goodGate(); g.colorspace = 14; CHECK(!signalExact(g));
}

static void testStore() {
    frameout::setSignalEnabled(false);
    frameout::SignalFrame s;
    auto src = makeRgba(4, 3);
    CHECK(packSignal(src.data(), 4, 3, 0, false, 9, 16, s));
    frameout::keepSignal("a", std::move(s));
    int meta[4];
    CHECK(!frameout::signalInfo("a", meta));
    frameout::setSignalEnabled(true);
    CHECK(packSignal(src.data(), 4, 3, 90, false, 9, 18, s));
    frameout::keepSignal("a", std::move(s));
    CHECK(frameout::signalInfo("a", meta) && meta[0] == 3 && meta[1] == 4 && meta[2] == 9 && meta[3] == 18);
    CHECK(frameout::signalInfo("a", meta));
    frameout::SignalFrame taken;
    CHECK(frameout::takeSignal("a", taken) && taken.valid && taken.rgb.size() == 36);
    CHECK(!frameout::takeSignal("a", taken));
    CHECK(!frameout::signalInfo("a", meta));
    CHECK(packSignal(src.data(), 4, 3, 0, false, 9, 16, s));
    frameout::keepSignal("b", std::move(s));
    frameout::keepSignal("b", frameout::SignalFrame());
    CHECK(!frameout::signalInfo("b", meta));
    CHECK(packSignal(src.data(), 4, 3, 0, false, 9, 16, s));
    frameout::keepSignal("c", std::move(s));
    frameout::dropSignal("c");
    CHECK(!frameout::signalInfo("c", meta));
    CHECK(packSignal(src.data(), 4, 3, 0, false, 9, 16, s));
    frameout::keepSignal("d", std::move(s));
    frameout::setSignalEnabled(false);
    CHECK(!frameout::signalInfo("d", meta));
}

static void testPackRejects() {
    frameout::SignalFrame out;
    CHECK(!packSignal(nullptr, 4, 4, 0, false, 9, 16, out) && !out.valid);
    auto src = makeRgba(2, 2);
    CHECK(!packSignal(src.data(), 0, 2, 0, false, 9, 16, out));
}

static void testThreads() {
    frameout::setSignalEnabled(true);
    auto src = makeRgba(16, 16);
    std::vector<std::thread> ts;
    for (int t = 0; t < 8; ++t)
        ts.emplace_back([&, t] {
            std::string key = "t" + std::to_string(t);
            for (int i = 0; i < 200; ++i) {
                frameout::SignalFrame s;
                packSignal(src.data(), 16, 16, 0, false, 9, 16, s);
                frameout::keepSignal(key, std::move(s));
                frameout::SignalFrame o;
                CHECK(frameout::takeSignal(key, o));
            }
        });
    for (auto& t : ts) t.join();
    frameout::setSignalEnabled(false);
}

int main() {
    testRotations();
    testRotateTypedMatchesPack();
    testGate();
    testStore();
    testPackRejects();
    testThreads();
    std::printf(failures == 0 ? "ALL PASS\n" : "FAILURES: %d\n", failures);
    return failures == 0 ? 0 : 1;
}
