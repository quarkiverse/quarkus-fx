#!/usr/bin/env bash
# Sets up GraalVM Community 25 Innovation 5 (JDK 25.0.5), the GraalVM of the native jobs (the same as Quarkus Desktop).
# Oracle GraalVM 25.0.x lacks the bootstrap registration API that the native builds of Quarkus 4.0.0 call
# (quarkusio/quarkus#58350), and GraalVM 25.1 or later builds the AWT and Swing of Quarkus Desktop into macOS native
# executables. The release has no build for macOS on Intel nor Windows on arm64. The archive is checked against its
# SHA-256, then JAVA_HOME, GRAALVM_HOME and the PATH of the next steps are set.
set -euo pipefail

release=graal-25.5.5.0.3
base=graalvm-community-jdk-25i5-25.0.5.0.3
case "$RUNNER_OS-$RUNNER_ARCH" in
  Linux-X64) file="${base}_linux-x64_bin.tar.gz" sha=c6438dee6c3ade2220d40b7ccdcad24b512d30e233e61bb4fc70519522de34ef ;;
  Linux-ARM64) file="${base}_linux-aarch64_bin.tar.gz" sha=41741c0650ae7564a78335f9e4fc353a902e6ff1d4140f2e4fabd1c55d5cce66 ;;
  macOS-ARM64) file="${base}_macos-aarch64_bin.tar.gz" sha=54369d4a5931a5a2101c7c52444a4e7e6647a3a4e041335053cda6fd22295e0c ;;
  Windows-X64) file="${base}_windows-x64_bin.zip" sha=b02c3b162129fa8cbfa7ebd0fcc1ae6eba2fc736bf3bef8e40e5ace3e9d9a0d6 ;;
  *) echo "::error::GraalVM Community 25 Innovation 5 has no build for $RUNNER_OS on $RUNNER_ARCH"; exit 1 ;;
esac

directory="$RUNNER_TEMP/graalvm"
mkdir -p "$directory"
archive="$RUNNER_TEMP/$file"
curl -fsSL --retry 3 --retry-all-errors -o "$archive" "https://github.com/graalvm/graalvm-ce-builds/releases/download/$release/$file"
if command -v sha256sum > /dev/null; then
  echo "$sha  $archive" | sha256sum -c -
else
  echo "$sha  $archive" | shasum -a 256 -c -
fi
if [[ "$file" == *.zip ]]; then
  unzip -q "$archive" -d "$directory"
else
  tar -xzf "$archive" -C "$directory"
fi
rm "$archive"

home=$(find "$directory" -mindepth 1 -maxdepth 1 -type d | head -1)
# the JDK of a macOS archive is in Contents/Home
[[ -d "$home/Contents/Home" ]] && home="$home/Contents/Home"
if [[ "$RUNNER_OS" == Windows ]]; then
  home=$(cygpath -w "$home")
fi
echo "JAVA_HOME=$home" >> "$GITHUB_ENV"
echo "GRAALVM_HOME=$home" >> "$GITHUB_ENV"
echo "$home/bin" >> "$GITHUB_PATH"
"$home/bin/java" -version
