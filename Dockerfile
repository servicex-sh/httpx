# https://www.docker.com/blog/faster-multi-platform-builds-dockerfile-cross-compilation-guide/
FROM ubuntu:22.04

COPY assembly/httpx /usr/bin/httpx

ENTRYPOINT ["/usr/bin/httpx"]