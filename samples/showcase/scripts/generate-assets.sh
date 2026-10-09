#!/usr/bin/env bash
# Regenerates the binary test assets of the showcase (images, audio, video) with local macOS tools only.
set -euo pipefail
cd "$(dirname "$0")/.."
JAVA_HOME=${JAVA_HOME:-$HOME/.sdkman/candidates/java/25-graalce}
RES=src/main/resources/showcase

"$JAVA_HOME/bin/java" tools/GenImages.java "$RES/images"

"$JAVA_HOME/bin/java" tools/GenAudio.java "$RES/media/hello.wav"
afconvert -f AIFC -d BEI16@22050 "$RES/media/hello.wav" "$RES/media/hello.aiff"
afconvert -f AIFF -d BEI16@22050 "$RES/media/hello.wav" "$RES/media-pages/hello-pcm.aiff"
afconvert -f m4af -d aac "$RES/media/hello.wav" "$RES/media/hello.m4a"

swift tools/GenVideo.swift "$RES/media/clip.mp4"

ls -la "$RES/images" "$RES/media"
