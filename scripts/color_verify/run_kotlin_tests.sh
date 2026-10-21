#!/bin/sh
# Compiles the colour parsers, tone mapper and their JUnit tests against minimal Android stubs and runs them.
# Usage: run_kotlin_tests.sh <kotlinc-bin-dir> <junit.jar> <hamcrest.jar> <kotlin-stdlib.jar>
set -e
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
D=$ROOT/engine/src/main/java/com/frameout/engine/decode
T=$ROOT/engine/src/test/java/com/frameout/engine
OUT=$(mktemp -d)
"$1/kotlinc" -nowarn -cp "$2:$3" -d "$OUT" "$ROOT"/scripts/color_verify/android_stubs/*.kt \
  $D/Av1Color.kt $D/Vp9Color.kt $D/MkvColour.kt $D/VuiColor.kt $D/ColorHints.kt $D/ColorParams.kt $D/PeakNits.kt \
  $D/ColorVariants.kt $D/CalibrationRule.kt $D/GainCodec.kt $D/HdrTables.kt $D/HdrToneMapper.kt $D/GamutConvert.kt $D/Dither.kt $D/Mp4Orientation.kt \
  $T/Av1ColorTest.kt $T/ContainerColourTest.kt $T/HdrDesaturationTest.kt $T/VuiColorTest.kt $T/HdrToneMapperTest.kt $T/HdrPeakTest.kt $T/ColorCalibrationTest.kt
java -cp "$OUT:$2:$3:$4" org.junit.runner.JUnitCore com.frameout.engine.Av1ColorTest com.frameout.engine.ContainerColourTest \
  com.frameout.engine.HdrDesaturationTest com.frameout.engine.VuiColorTest com.frameout.engine.HdrToneMapperTest com.frameout.engine.HdrPeakTest com.frameout.engine.ColorCalibrationTest
