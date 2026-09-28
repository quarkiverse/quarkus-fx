#!/bin/sh
# Builds the libjvm stand-ins of WebKit (see libjvm-stand-in.c) into
# src/main/resources/io/quarkiverse/fx/graal/libjvm-stand-in : the Linux ones with GCC in Ubuntu 24.04 containers
# (Docker), the macOS one with the Xcode command line tools. Run on macOS, from any directory. The same files for the
# same toolchain : the Ubuntu image is pinned, but GCC (13) and binutils (2.42) are installed from the Ubuntu archive
# when the script runs, and the macOS library records the versions of the SDK and of the linker.
set -eu

cd "$(dirname "$0")/.."
out=resources/io/quarkiverse/fx/graal/libjvm-stand-in
# ubuntu:24.04 (linux/amd64 and linux/arm64)
image=ubuntu:24.04@sha256:008173c23f95b170204355c12626cb5a965d779a7e1283b09e9cffbb1bf33ca3

# -nostdlib : no dependency (the C library is not needed), -s : no symbols, -fno-ident and --build-id=none : neither
# the version of GCC (.comment) nor a build ID
for platform in amd64:x86_64 arm64:aarch64; do
    mkdir -p "$out/linux-${platform#*:}"
    docker run --rm --platform "linux/${platform%%:*}" -v "$PWD:/src" -w /src "$image" sh -c "
        apt-get update -qq && apt-get install -qq -y --no-install-recommends gcc > /dev/null
        gcc -shared -nostdlib -s -fno-ident -Wl,-soname,libjvm.so -Wl,--build-id=none \
            -o $out/linux-${platform#*:}/libjvm.so c/libjvm-stand-in.c"
done

# The minimum macOS version of libjfxwebkit.dylib (JavaFX 21 to 27), the compatibility version it expects from
# libjvm.dylib, both slices signed by the linker (ad-hoc), as it does by default for arm64
mkdir -p "$out/macos"
clang -dynamiclib -arch arm64 -arch x86_64 -mmacosx-version-min=11.0 -Wl,-adhoc_codesign \
    -install_name @rpath/libjvm-stand-in.dylib -compatibility_version 1.0.0 -current_version 1.0.0 \
    -o "$out/macos/libjvm.dylib" c/libjvm-stand-in.c

# Resources, not executables
chmod 644 "$out"/*/libjvm.*
