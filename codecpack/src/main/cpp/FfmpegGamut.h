#pragma once
#include <cstddef>
#include <cstdint>

void gamutToBt709(std::uint8_t* rgba, std::size_t pixels, int primaries);
