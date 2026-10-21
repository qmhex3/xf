#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FFMPEG_VERSION="${FFMPEG_VERSION:-9.0.2}"
DAV1D_VERSION="${DAV1D_VERSION:-1.5.4}"
# VideoLAN release signing key (fingerprint as published in distro packaging, e.g. Arch dav1d validpgpkeys).
DAV1D_KEY_FINGERPRINT="${DAV1D_KEY_FINGERPRINT:-65F7C6B4206BD057A7EB73787180713BE58D1ADC}"
# Optional extra pin: a tarball digest you trust. When set, it replaces the GPG check.
DAV1D_SHA256="${DAV1D_SHA256:-}"
NDK="${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME}"
API=24
ABIS=("arm64-v8a" "armeabi-v7a" "x86_64")
OUT_ROOT="${1:-$ROOT_DIR/build/ffmpeg}"
if [[ "$OUT_ROOT" != /* ]]; then OUT_ROOT="$ROOT_DIR/$OUT_ROOT"; fi
SRC_DIR="${OUT_ROOT}/src"
mkdir -p "$SRC_DIR" "$OUT_ROOT"

if [ ! -d "$SRC_DIR/ffmpeg" ]; then
  curl -fsSL "https://ffmpeg.org/releases/ffmpeg-${FFMPEG_VERSION}.tar.xz" -o "$SRC_DIR/ffmpeg.tar.xz"
  curl -fsSL "https://ffmpeg.org/releases/ffmpeg-${FFMPEG_VERSION}.tar.xz.asc" -o "$SRC_DIR/ffmpeg.tar.xz.asc"
  curl -fsSL https://ffmpeg.org/ffmpeg-devel.asc -o "$SRC_DIR/ffmpeg-devel.asc"
  FFMPEG_KEY_FINGERPRINT="FCF986EA15E6E293A5644F10B4322F04D67658D8"
  ACTUAL_FINGERPRINT="$(gpg --batch --show-keys --with-colons "$SRC_DIR/ffmpeg-devel.asc" | awk -F: '$1 == "fpr" {print $10; exit}')"
  test "$ACTUAL_FINGERPRINT" = "$FFMPEG_KEY_FINGERPRINT"
  gpg --batch --import "$SRC_DIR/ffmpeg-devel.asc"
  gpg --batch --verify "$SRC_DIR/ffmpeg.tar.xz.asc" "$SRC_DIR/ffmpeg.tar.xz"
  tar -xJf "$SRC_DIR/ffmpeg.tar.xz" -C "$SRC_DIR"
  mv "$SRC_DIR/ffmpeg-${FFMPEG_VERSION}" "$SRC_DIR/ffmpeg"
fi
printf '%s\n' "$FFMPEG_VERSION" > "$OUT_ROOT/FFMPEG_VERSION.txt"

DAV1D_BASE="https://downloads.videolan.org/pub/videolan/dav1d/${DAV1D_VERSION}"
if [ ! -d "$SRC_DIR/dav1d" ]; then
  curl -fsSL "$DAV1D_BASE/dav1d-${DAV1D_VERSION}.tar.xz" -o "$SRC_DIR/dav1d.tar.xz"
  curl -fsSL "$DAV1D_BASE/dav1d-${DAV1D_VERSION}.tar.xz.sha256" -o "$SRC_DIR/dav1d.tar.xz.sha256"
  ACTUAL_DAV1D_SHA256="$(sha256sum "$SRC_DIR/dav1d.tar.xz" | awk '{print $1}')"
  PUBLISHED_DAV1D_SHA256="$(awk '{print $1; exit}' "$SRC_DIR/dav1d.tar.xz.sha256")"
  test "$ACTUAL_DAV1D_SHA256" = "$PUBLISHED_DAV1D_SHA256"
  if [ -n "$DAV1D_SHA256" ]; then
    test "$ACTUAL_DAV1D_SHA256" = "$DAV1D_SHA256"
  else
    curl -fsSL "$DAV1D_BASE/dav1d-${DAV1D_VERSION}.tar.xz.asc" -o "$SRC_DIR/dav1d.tar.xz.asc"
    DAV1D_GNUPGHOME="$SRC_DIR/dav1d-gnupg"
    rm -rf "$DAV1D_GNUPGHOME"; mkdir -m 700 "$DAV1D_GNUPGHOME"
    for keyserver in hkps://keyserver.ubuntu.com hkps://pgp.mit.edu; do
      gpg --homedir "$DAV1D_GNUPGHOME" --batch --keyserver "$keyserver" --recv-keys "$DAV1D_KEY_FINGERPRINT" || true
      gpg --homedir "$DAV1D_GNUPGHOME" --batch --list-keys "$DAV1D_KEY_FINGERPRINT" >/dev/null 2>&1 && break
    done
    if ! gpg --homedir "$DAV1D_GNUPGHOME" --batch --list-keys "$DAV1D_KEY_FINGERPRINT" >/dev/null 2>&1; then
      curl -fsSL "https://download.videolan.org/pub/keys/${DAV1D_KEY_FINGERPRINT: -16}.asc" -o "$SRC_DIR/dav1d-key.asc" || true
      gpg --homedir "$DAV1D_GNUPGHOME" --batch --import "$SRC_DIR/dav1d-key.asc" || true
    fi
    if ! gpg --homedir "$DAV1D_GNUPGHOME" --batch --list-keys "$DAV1D_KEY_FINGERPRINT" >/dev/null 2>&1; then
      echo "could not import VideoLAN signing key $DAV1D_KEY_FINGERPRINT from any source; pin DAV1D_SHA256 instead." >&2
      exit 1
    fi
    SIGNED_BY="$(gpg --homedir "$DAV1D_GNUPGHOME" --batch --status-fd 1 --verify "$SRC_DIR/dav1d.tar.xz.asc" "$SRC_DIR/dav1d.tar.xz" 2>/dev/null | awk '$2 == "VALIDSIG" {print $NF}')"
    if [ "$SIGNED_BY" != "$DAV1D_KEY_FINGERPRINT" ]; then
      echo "dav1d signature check failed (signed by '${SIGNED_BY:-nobody}', expected $DAV1D_KEY_FINGERPRINT)." >&2
      echo "If VideoLAN rotated its key, set DAV1D_KEY_FINGERPRINT or pin DAV1D_SHA256." >&2
      exit 1
    fi
  fi
  tar -xJf "$SRC_DIR/dav1d.tar.xz" -C "$SRC_DIR"
  mv "$SRC_DIR/dav1d-${DAV1D_VERSION}" "$SRC_DIR/dav1d"
fi
printf '%s\n' "$DAV1D_VERSION" > "$OUT_ROOT/DAV1D_VERSION.txt"
command -v meson >/dev/null || { echo "meson is required to build libdav1d"; exit 1; }
command -v ninja >/dev/null || { echo "ninja is required to build libdav1d"; exit 1; }

HOST_TAG=linux-x86_64
TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/$HOST_TAG"
export PATH="$TOOLCHAIN/bin:$PATH"
export LDFLAGS="-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"

COMMON_FLAGS=(
  --disable-everything
  --disable-doc
  --disable-programs
  --disable-avdevice
  --enable-swscale
  --disable-network
  --disable-gpl
  --disable-nonfree
  --disable-version3
  --disable-debug
  --enable-small
  --enable-lto
  --enable-shared
  --enable-avcodec
  --enable-avformat
  --enable-avutil
  --enable-swresample
  --enable-libdav1d
  --enable-demuxer=matroska,avi,asf,flv,mpegts,mpegps,rm,ogg,mov,gif,webp,image2,mxf,dv,dnxhd,mjpeg_2000,image_j2k_pipe,nut,ivf,yuv4mpegpipe,wtv,bink,smacker,gxf,lxf,h264,hevc,vc1,mpegvideo,m4v,mjpeg,obu
  --enable-decoder=h264,hevc,mpeg4,mpeg1video,mpeg2video,vp8,vp9,mjpeg,gif,png,webp,wmv1,wmv2,wmv3,vc1,msmpeg4v1,msmpeg4v2,msmpeg4v3,flv,h263,h263p,rv10,rv20,rv30,rv40,theora,vp3,vp5,vp6,vp6f,prores,ffv1,dnxhd,dvvideo,jpeg2000,v210,v410,rawvideo,bink,smacker,amv,svq1,svq3,libdav1d
  --enable-parser=h264,hevc,mpeg4video,mpegvideo,vp3,vp8,vp9,vc1,h263,mjpeg,dnxhd,jpeg2000,prores,webp,mpegaudio,aac,av1,ac3
  --enable-protocol=file
  --enable-decoder=pcm_s16le,aac,mp3
  --extra-cflags="-Os -fPIC -ffunction-sections -fdata-sections"
  --extra-ldflags="-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384 -Wl,--gc-sections -Wl,--icf=safe"
)

# Builds libdav1d as a static, position-independent archive so it links into libavcodec.so.
# No extra .so is shipped, so the codec pack library list and size limits stay unchanged.
build_dav1d() {
  local ABI=$1 TRIPLE=$2 MESON_FAMILY=$3 MESON_CPU=$4
  local DAV1D_PREFIX="$OUT_ROOT/dav1d/$ABI"
  local BUILD_DIR="$OUT_ROOT/dav1d-build/$ABI"
  local CROSS_FILE="$OUT_ROOT/dav1d-build/$ABI.cross"
  mkdir -p "$OUT_ROOT/dav1d-build"
  cat > "$CROSS_FILE" <<EOF
[binaries]
c = '$TOOLCHAIN/bin/${TRIPLE}${API}-clang'
cpp = '$TOOLCHAIN/bin/${TRIPLE}${API}-clang++'
ar = '$TOOLCHAIN/bin/llvm-ar'
ranlib = '$TOOLCHAIN/bin/llvm-ranlib'
strip = '$TOOLCHAIN/bin/llvm-strip'
nm = '$TOOLCHAIN/bin/llvm-nm'
pkg-config = 'pkg-config'

[host_machine]
system = 'android'
cpu_family = '$MESON_FAMILY'
cpu = '$MESON_CPU'
endian = 'little'

[built-in options]
c_args = ['-fPIC']
c_link_args = ['-Wl,-z,max-page-size=16384', '-Wl,-z,common-page-size=16384']
cpp_link_args = ['-Wl,-z,max-page-size=16384', '-Wl,-z,common-page-size=16384']

[properties]
needs_exe_wrapper = true
EOF
  rm -rf "$BUILD_DIR" "$DAV1D_PREFIX"
  meson setup "$BUILD_DIR" "$SRC_DIR/dav1d" \
    --cross-file "$CROSS_FILE" \
    --prefix "$DAV1D_PREFIX" \
    --libdir lib \
    --buildtype release \
    --default-library static \
    -Db_staticpic=true \
    -Db_lto=false \
    -Denable_tools=false \
    -Denable_tests=false \
    -Denable_examples=false \
    -Denable_docs=false
  ninja -C "$BUILD_DIR"
  ninja -C "$BUILD_DIR" install
  test -f "$DAV1D_PREFIX/lib/libdav1d.a"
  test -f "$DAV1D_PREFIX/lib/pkgconfig/dav1d.pc"
}

build_abi() {
  local ABI=$1
  local TRIPLE CPU ARCH MESON_FAMILY MESON_CPU
  case "$ABI" in
    arm64-v8a) TRIPLE=aarch64-linux-android; CPU=armv8-a; ARCH=aarch64; MESON_FAMILY=aarch64; MESON_CPU=aarch64 ;;
    armeabi-v7a) TRIPLE=armv7a-linux-androideabi; CPU=armv7-a; ARCH=arm; MESON_FAMILY=arm; MESON_CPU=armv7 ;;
    x86_64) TRIPLE=x86_64-linux-android; CPU=x86-64; ARCH=x86_64; MESON_FAMILY=x86_64; MESON_CPU=x86_64 ;;
    *) echo "bad abi $ABI"; exit 1 ;;
  esac
  local PREFIX="$OUT_ROOT/$ABI"
  mkdir -p "$PREFIX"
  build_dav1d "$ABI" "$TRIPLE" "$MESON_FAMILY" "$MESON_CPU"
  # Only the cross-built dav1d may be seen by FFmpeg's configure, never host libraries.
  export PKG_CONFIG_LIBDIR="$OUT_ROOT/dav1d/$ABI/lib/pkgconfig"
  unset PKG_CONFIG_PATH
  cd "$SRC_DIR/ffmpeg"
  make distclean >/dev/null 2>&1 || true
  ./configure \
    --prefix="$PREFIX" \
    --target-os=android \
    --arch="$ARCH" \
    --cpu="$CPU" \
    --cc="${TRIPLE}${API}-clang" \
    --cxx="${TRIPLE}${API}-clang++" \
    --nm=llvm-nm \
    --ar=llvm-ar \
    --ranlib=llvm-ranlib \
    --strip=llvm-strip \
    --enable-cross-compile \
    --pkg-config=pkg-config \
    --pkg-config-flags=--static \
    --sysroot="$TOOLCHAIN/sysroot" \
    "${COMMON_FLAGS[@]}"
  bash "$ROOT_DIR/scripts/check-ffmpeg-license.sh" --config "$SRC_DIR/ffmpeg"
  bash "$ROOT_DIR/scripts/check-ffmpeg-components.sh" "$SRC_DIR/ffmpeg"
  make -j"$(nproc)"
  make install
  llvm-strip --strip-unneeded "$PREFIX/lib/"*.so || true
  grep -q '^#define CONFIG_LIBDAV1D 1' config.h
  local JNI_DIR="$ROOT_DIR/codecpack/src/main/jniLibs/$ABI"
  local INC_DIR="$ROOT_DIR/codecpack/src/main/cpp/ffmpeg/include"
  mkdir -p "$JNI_DIR" "$INC_DIR"
  cp "$PREFIX/lib/"libavutil.so "$JNI_DIR/"
  cp "$PREFIX/lib/"libswresample.so "$JNI_DIR/"
  cp "$PREFIX/lib/"libswscale.so "$JNI_DIR/"
  cp "$PREFIX/lib/"libavcodec.so "$JNI_DIR/"
  cp "$PREFIX/lib/"libavformat.so "$JNI_DIR/"
  if [ ! -d "$INC_DIR/libavcodec" ]; then cp -R "$PREFIX/include/"libavcodec "$INC_DIR/"; fi
  if [ ! -d "$INC_DIR/libavformat" ]; then cp -R "$PREFIX/include/"libavformat "$INC_DIR/"; fi
  if [ ! -d "$INC_DIR/libavutil" ]; then cp -R "$PREFIX/include/"libavutil "$INC_DIR/"; fi
  if [ ! -d "$INC_DIR/libswscale" ]; then cp -R "$PREFIX/include/"libswscale "$INC_DIR/"; fi
  if [ ! -d "$INC_DIR/libswresample" ]; then cp -R "$PREFIX/include/"libswresample "$INC_DIR/"; fi
  cd -
  echo "Built $ABI -> $PREFIX"
}

for abi in "${ABIS[@]}"; do
  build_abi "$abi"
done

echo "Done. Libraries under $OUT_ROOT/<abi>/lib"
