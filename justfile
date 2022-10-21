#!/usr/bin/env just --justfile

VERSION := "0.36.0"

# build the project
build:
   mvn -DskipTests package

# rebuild the project, and clean first
rebuild:
   mvn -DskipTests clean package

# list targets
list-targets:
   java -jar target/httpx.jar --list

# list targets
list-summary:
   java -jar target/httpx.jar --summary

# invoke myip http request
myip:
   java -jar target/httpx.jar myip

#  grpcurl test
grpcurl-test:
   grpcurl -plaintext -d '{"name": "Jackie"}' localhost:50052 org.mvnsearch.service.Greeter/SayHello

# native build with GraalVM native-image
native-build:
   mvn -Pnative -DskipTests clean package
   upx -7 target/httpx
   cp target/httpx ~/bin/httpx

# dependency tree
dependencies:
  mvn dependency:tree -Dscope=compile > dependencies.txt

# dependencies updates
updates:
   mvn versions:display-dependency-updates > updates.txt

copy-dependencies:
   rm -rf target/dependency
   mvn dependency:copy-dependencies -DincludeGroupIds=software.amazon.awssdk

# Docker image build
image-build:
   mkdir -p assembly
   rm -rf assembly/*
   (cd assembly; wget https://github.com/servicex-sh/httpx/releases/download/v{{VERSION}}/httpx-linux-x86_64.zip; unzip httpx-linux-x86_64.zip; rm -rf httpx-linux-x86_64.zip; chmod u+x httpx)
   docker build -t linuxchina/httpx:{{VERSION}} .

# Docker image build
image-build-arm64:
   docker build -t linuxchina/httpx:{{VERSION}}-arm64 .
   docker build -t linuxchina/httpx:latest-arm64 .
   docker push linuxchina/httpx:{{VERSION}}-arm64
   docker push linuxchina/httpx:latest-arm64

image-build-multi-platform:
   docker buildx build --push --platform linux/arm64/v8,linux/amd64 --tag linuxchina/httpx:{{VERSION}} .
   docker buildx build --push --platform linux/arm64/v8,linux/amd64 --tag linuxchina/httpx:latest .

# httpx docker image test
image-test:
   docker run --rm linuxchina/httpx:{{VERSION}} --version

# Push image to DockerHub
image-push:
   docker tag linuxchina/httpx:{{VERSION}} linuxchina/httpx:latest
   docker push linuxchina/httpx:{{VERSION}}
   docker push linuxchina/httpx:latest