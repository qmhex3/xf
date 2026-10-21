# Onboarding clips

The first two onboarding screens can play a real clip. Put the files in `app/src/main/assets/onboarding/`:

| File | Screen | Shown as |
|------|--------|----------|
| `hero.mp4` | 1 (welcome) | Square viewfinder, center-cropped. The filmstrip under it is built from real frames of this clip, and the needle and timecode follow playback. |
| `formats.mp4` | 2 (formats) | 16:9 card. The tray under it receives four real frames taken at 30%, 46%, 62% and 78% of the clip, each one when playback passes it (with a flash on the card). Put the decisive moment between 30% and 78%. |
| `pick.mp4` (optional) | 3 (smart pick) | Nine stills taken at 38% to 62% of the clip, as one burst; the middle one is presented as the sharpest. Needs no playback, only frames. If `pick.mp4` is missing the screen borrows `formats.mp4`, then `hero.mp4`, then the drawn art. A clip with a clearly sharp subject in the middle of its length reads best. |
| `ready.mp4` (optional) | 4 (ready) | The clip plays in the center card, muted and looped. Four real frames taken at 20%, 42%, 64% and 86% slide into the two side cards as playback passes them, each with a shutter flash, a "Captured" badge and the corner brackets closing in; the timecode follows playback. Without `ready.mp4` the screen shows three stills (40%, 50%, 60%) from the first clip that exists, in the order `pick.mp4`, `formats.mp4`, `hero.mp4`, or the drawn art if none exists. |

If a file is missing, that screen keeps its drawn artwork, so the app always builds and runs.

Format: H.264 MP4, 720p at most, 30 fps, no audio track, 5 to 8 seconds, under 2 MB, with the last frame close to the first so the loop is seamless. The clip is played muted and looped, and paused when the app goes to the background or the system asks to reduce motion (the first frame stays visible).

```
ffmpeg -i input.mov -an -vf "scale=-2:720,fps=30" -c:v libx264 -profile:v main -crf 26 -preset slow -movflags +faststart hero.mp4
```

For `hero.mp4` (square card) crop to 1:1 first, and add a keyframe every few frames (`-g 6 -keyint_min 6`) so the filmstrip thumbnails are taken at exact positions:

```
ffmpeg -i input.mp4 -an -vf "crop='min(iw,ih)':'min(iw,ih)',scale=720:720,fps=30" -c:v libx264 -profile:v main -crf 24 -preset slow -g 6 -keyint_min 6 -movflags +faststart hero.mp4
```

Seamless loop from any clip (plays forward, then backward):

```
ffmpeg -i hero.mp4 -filter_complex "[0:v]split[a][b];[b]reverse[r];[a][r]concat=n=2:v=1[v]" -map "[v]" -an -c:v libx264 -crf 24 -g 6 -movflags +faststart hero_loop.mp4
```
