# https://www.docker.com/blog/faster-multi-platform-builds-dockerfile-cross-compilation-guide/
FROM --platform=$BUILDPLATFORM ubuntu:22.04

ARG TARGETARCH

COPY assembly/httpx-${TARGETARCH} /usr/bin/httpx

ENTRYPOINT ["/usr/bin/httpx"]