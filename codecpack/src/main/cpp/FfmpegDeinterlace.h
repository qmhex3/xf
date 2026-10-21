#pragma once

struct AVFrame;

bool canDeinterlaceFormat(int format);
void deinterlaceFrame(AVFrame* frame, bool topFieldFirst);
