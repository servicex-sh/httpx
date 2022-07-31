FROM ubuntu:22.04

COPY assembly/httpx /usr/bin/httpx

ENTRYPOINT ["/usr/bin/httpx"]