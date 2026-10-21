cmake_minimum_required(VERSION 3.22.1)
project(frameout_ffmpeg)

set(FFMPEG_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg")
set(FFMPEG_INCLUDE "${FFMPEG_ROOT}/include")
set(FFMPEG_LIB "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}")

if(EXISTS "${FFMPEG_INCLUDE}/libavformat/avformat.h" AND EXISTS "${FFMPEG_LIB}/libavformat.so")
    add_library(avutil SHARED IMPORTED)
    set_target_properties(avutil PROPERTIES IMPORTED_LOCATION "${FFMPEG_LIB}/libavutil.so" INTERFACE_INCLUDE_DIRECTORIES "${FFMPEG_INCLUDE}")
    add_library(swresample SHARED IMPORTED)
    set_target_properties(swresample PROPERTIES IMPORTED_LOCATION "${FFMPEG_LIB}/libswresample.so" INTERFACE_INCLUDE_DIRECTORIES "${FFMPEG_INCLUDE}")
    add_library(swscale SHARED IMPORTED)
    set_target_properties(swscale PROPERTIES IMPORTED_LOCATION "${FFMPEG_LIB}/libswscale.so" INTERFACE_INCLUDE_DIRECTORIES "${FFMPEG_INCLUDE}")
    add_library(avcodec SHARED IMPORTED)
    set_target_properties(avcodec PROPERTIES IMPORTED_LOCATION "${FFMPEG_LIB}/libavcodec.so" INTERFACE_INCLUDE_DIRECTORIES "${FFMPEG_INCLUDE}")
    add_library(avformat SHARED IMPORTED)
    set_target_properties(avformat PROPERTIES IMPORTED_LOCATION "${FFMPEG_LIB}/libavformat.so" INTERFACE_INCLUDE_DIRECTORIES "${FFMPEG_INCLUDE}")
    add_library(frameout_ffmpeg SHARED frameout_ffmpeg.cpp FrameoutJniIndex.cpp FfmpegDecoder.cpp FfmpegSignalStore.cpp FfmpegDecoderSession.cpp FfmpegSessionCore.cpp FfmpegAlpha.cpp FfmpegColor.cpp FfmpegMatrix.cpp FfmpegDolby.cpp FfmpegGamut.cpp FfmpegDeinterlace.cpp FfmpegDisplay.cpp FfmpegHdr.cpp FfmpegProbe.cpp FfmpegIndexer.cpp FfmpegTileGrid.cpp FfmpegError.cpp)
    target_compile_definitions(frameout_ffmpeg PRIVATE FRAMEOUT_HAS_FFMPEG=1)
    target_include_directories(frameout_ffmpeg PRIVATE "${FFMPEG_INCLUDE}")
    find_library(log-lib log)
    find_library(dl-lib dl)
    find_library(jnigraphics-lib jnigraphics)
    target_link_libraries(frameout_ffmpeg avformat avcodec avutil swscale swresample ${log-lib} ${dl-lib} ${jnigraphics-lib})
else()
    add_library(frameout_ffmpeg SHARED frameout_ffmpeg.cpp FrameoutJniIndex.cpp FfmpegDecoder.cpp FfmpegSignalStore.cpp FfmpegDecoderSession.cpp FfmpegSessionCore.cpp FfmpegAlpha.cpp FfmpegDeinterlace.cpp FfmpegDisplay.cpp FfmpegHdr.cpp FfmpegProbe.cpp FfmpegIndexer.cpp FfmpegTileGrid.cpp FfmpegError.cpp)
    find_library(log-lib log)
    find_library(jnigraphics-lib jnigraphics)
    target_link_libraries(frameout_ffmpeg ${log-lib} ${jnigraphics-lib})
endif()

target_compile_options(frameout_ffmpeg PRIVATE
    -ffunction-sections -fdata-sections
    -fstack-protector-strong
    -Wformat -Werror=format-security
    $<$<NOT:$<CONFIG:Debug>>:-D_FORTIFY_SOURCE=2>)
set_target_properties(frameout_ffmpeg PROPERTIES LINK_FLAGS "-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384 -Wl,-z,relro -Wl,-z,now -Wl,-z,noexecstack -Wl,--gc-sections -Wl,--icf=safe -Wl,--exclude-libs,ALL -Wl,--version-script=${CMAKE_CURRENT_SOURCE_DIR}/frameout_ffmpeg.map")
