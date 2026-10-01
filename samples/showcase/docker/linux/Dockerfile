# Linux environment to build and run the showcase (JVM and native modes) with a virtual X server.
#
#   docker build -t quarkus-fx-showcase-linux docker/linux
#   docker run --rm --init -v "$PWD":/showcase -v "$HOME/.m2":/root/.m2 quarkus-fx-showcase-linux java tools/Cycle.java linux
FROM quay.io/quarkus/ubi-quarkus-graalvmce-builder-image:jdk-25 AS graalvm

FROM ubuntu:24.04
ENV DEBIAN_FRONTEND=noninteractive LANG=C.UTF-8
RUN apt-get update && apt-get install -y --no-install-recommends \
        xvfb xauth \
        libgtk-3-0t64 libxtst6 libxxf86vm1 libgl1 libgl1-mesa-dri libegl1 libglu1-mesa \
        libasound2t64 libavcodec60 libavformat60 \
        fonts-dejavu-core fonts-liberation fonts-noto-core fonts-noto-cjk fonts-noto-color-emoji \
        build-essential zlib1g-dev libfreetype-dev \
        curl unzip ca-certificates git \
    && rm -rf /var/lib/apt/lists/*
# No sound card in a container : play audio to the ALSA null device
RUN printf 'pcm.!default {\n    type null\n}\n' > /etc/asound.conf
COPY --from=graalvm /opt/graalvm /opt/graalvm
ENV JAVA_HOME=/opt/graalvm GRAALVM_HOME=/opt/graalvm PATH=/opt/graalvm/bin:$PATH
COPY xvfb-exec /usr/local/bin/xvfb-exec
WORKDIR /showcase
ENTRYPOINT ["xvfb-exec"]
