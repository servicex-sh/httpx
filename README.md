httpx: CLI for run http file
==========================

httpx is a CLI to execute requests from [JetBrains Http File](https://www.jetbrains.com/help/idea/http-client-in-product-code-editor.html).

# How to use?

Create index.http file with following code, then `chmod u+x index.http`

```
#!/usr/bin/env httpx --httpfile

### get my internet ip
# @name myip
GET https://httpbin.org/ip

### inspection http post
# @name post
POST https://httpbin.org/post
Content-Type: application/json

[ 1 ]

### RSocket Request
// @name request
RSOCKET com.example.UserService.findById
Host: 127.0.0.1:42252
Content-Type: application/json

1

### grpc call SayHello
//@name SayHello
GRPC localhost:50052/org.mvnsearch.service.Greeter/SayHello

{
  "name": "Jackie"
}

### graphql query
//@name query
GRAPHQL http://localhost:8080/graphql
Content-Type: application/graphql

query { hello }
```

Then input `httpx myip` or `./index.http myip` to invoke request.

# Protocols Support

* HTTP Request
* RSocket Request
* GRPC Request: you should install [grpcurl](https://github.com/fullstorydev/grpcurl)
* GraphQL support: Query, Mutation and Subscribe on HTTP and WebSocket(graphql-ws)

# oh-my-zsh integration for shell completion

Please create `~/.oh-my-zsh/custom/plugins/httpx` with following code, then add `httpx` to `plugins` in `.zshrc` file.

```shell
#compdef httpx
#autload

local subcmds=()

while read -r line ; do
   if [[ ! $line == Available* ]] ;
   then
      subcmds+=(${line/[[:space:]]*\#/:})
   fi
done < <(httpx --summary)

_describe 'command' subcmds
```

# How to build from source?

httpx uses [Toolchains Maven Plugin](https://github.com/linux-china/toolchains-maven-plugin) to build project, 
and you don't need to install GraalVM first, and GraalVM will be installed in `~/.m2/jdks`.

```
./mvnw -Pnative -DskipTests clean package
```

# References

* JetBrains HTTP client: https://www.jetbrains.com/help/idea/http-client-in-product-code-editor.html
* http-request-in-editor-spec: https://github.com/JetBrains/http-request-in-editor-spec/blob/master/spec.md
* RSocket: https://rsocket.io/
* gRPCurl: https://github.com/fullstorydev/grpcurl
* picocli: https://picocli.info/
* GraalVM: https://www.graalvm.org/
* UPX: Ultimate Packer for eXecutables -  https://upx.github.io/
* type-detecting-hints-for-third-party-libraries: https://github.com/joshlong/type-detecting-hints-for-third-party-libraries
