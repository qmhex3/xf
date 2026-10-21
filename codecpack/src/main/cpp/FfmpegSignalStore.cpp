#include "FfmpegDecoder.h"
#include <atomic>
#include <map>
#include <mutex>
#include <utility>

namespace {
std::atomic<bool> enabled{false};
std::mutex storeMutex;
std::map<std::string, frameout::SignalFrame>& store() {
    static std::map<std::string, frameout::SignalFrame> value;
    return value;
}
}

namespace frameout {

void setSignalEnabled(bool on) {
    enabled.store(on);
    if (on) return;
    std::lock_guard<std::mutex> lock(storeMutex);
    store().clear();
}

bool signalEnabled() {
    return enabled.load();
}

void keepSignal(const std::string& path, SignalFrame&& signal) {
    std::lock_guard<std::mutex> lock(storeMutex);
    if (!enabled.load() || !signal.valid) {
        store().erase(path);
        return;
    }
    store()[path] = std::move(signal);
}

bool signalInfo(const std::string& path, int* meta) {
    std::lock_guard<std::mutex> lock(storeMutex);
    auto it = store().find(path);
    if (it == store().end() || !it->second.valid) return false;
    meta[0] = it->second.width;
    meta[1] = it->second.height;
    meta[2] = it->second.primaries;
    meta[3] = it->second.transfer;
    return true;
}

bool takeSignal(const std::string& path, SignalFrame& out) {
    std::lock_guard<std::mutex> lock(storeMutex);
    auto it = store().find(path);
    if (it == store().end() || !it->second.valid) return false;
    out = std::move(it->second);
    store().erase(it);
    return true;
}

void dropSignal(const std::string& path) {
    std::lock_guard<std::mutex> lock(storeMutex);
    store().erase(path);
}

}
