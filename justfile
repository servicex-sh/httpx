#!/usr/bin/env just --justfile

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
   upx -7 target/httpx-osx-x86_64
   cp target/httpx-osx-x86_64 ~/bin/httpx

# dependency tree
dependencies:
  mvn dependency:tree -Dscope=compile > dependencies.txt

# dependencies updates
updates:
   mvn versions:display-dependency-updates > updates.txt

copy-dependencies:
   rm -rf target/dependency
   mvn dependency:copy-dependencies -DincludeGroupIds=software.amazon.awssdk