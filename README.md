httpx: CLI for run http file
==========================

httpx is a CLI to execute requests from [JetBrains Http File](https://www.jetbrains.com/help/idea/http-client-in-product-code-editor.html).

# Request types supported by httpx

* HTTP REST
* PUB/SUB - Apache Kafka, Apache Pulsar, RabbitMQ, NATS, Redis, MQTT, AMQP, Stomp, ZeroMQ
* gRPC
* RSocket
* Apache Dubbo
* Sofa RPC
* Email sending by SMTP
* GraphQL with HTTP and WebSocket
* Memcache: set/get/delete
* Redis: set/hmset/eval
* msgpack-rpc:  Neovim support
* json-rpc: HTTP and TCP support

# How to install?

* Mac : `brew install servicex-sh/tap/httpx`
* Other platform: download binary from https://github.com/servicex-sh/httpx/releases
* JetBrains IDEs plugin: https://plugins.jetbrains.com/plugin/18807-httpx-requests
* Neovim plugin: https://github.com/servicex-sh/httpx.vim

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

### send an email by Gmail
//@name mail
MAIL mailto:demo@example.com
Host: tls://smtp.gmail.com:587
Authorization: Basic your_name@gmail.com:google_app_password
From: your_name@gmail.com
Subject: e-nice to meet you
Content-Type: text/plain

Hi Master:
  this is testing email.

Best regards
Yours sincerely Zombie
```

Then input `httpx myip` or `./index.http myip` to invoke request.

# Protocols Support

* HTTP Request
* RSocket Request
* GRPC Request: you should install [grpcurl](https://github.com/fullstorydev/grpcurl)
* GraphQL support: Query, Mutation and Subscribe on HTTP and WebSocket(graphql-ws)
* EMAIL: send email by SMTP
* PUB/SUB: pub/sub support for Kafka, RabbitMQ/ActiveMQ, Nats, Redis, MQTT, Stomp and Aliyun MNS/EventBridge.

# Email sending

Email URL format: `mailto:name@email.com?cc=name2@email.com`

* Gmail: please use App Password from https://support.google.com/accounts/answer/185833?p=InvalidSecondFactor

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

# JavaScript Code test

JetBrains HTTP Client uses JavaScript [ECMAScript 5.1](https://www.ecma-international.org/ecma-262/5.1/) as response handler for test.
httpx uses Node.js as JS engine, and you should install Node.js first.

```
### hello ip
GET https://httpbin.org/ip

> {%
    client.test("Request executed successfully", function() {
        client.log(response.status);
        client.log(response.contentType);
        client.log(response.body);
    });
%}
```

**Attentions**:

* You should know the difference between ECMAScript 5.1 and Node.js
* JavaScript code test is available for HTTP, gRPC, RSocket, Dubbo and other protocols with httpx

# How to build from source?

httpx uses [Toolchains Maven Plugin](https://github.com/linux-china/toolchains-maven-plugin) to build project, and you don't need to install GraalVM first, and GraalVM will be
installed in `~/.m2/jdks`.

```shell
./mvnw -Pnative -DskipTests clean package
```

# Development setup

* docker-compose.yml:  MQTT/1883, Stomp/61613, RabbitMQ/5672, SMTP/1025, Redpanda/9092

# References

* JetBrains HTTP client: https://www.jetbrains.com/help/idea/http-client-in-product-code-editor.html
* http-request-in-editor-spec: https://github.com/JetBrains/http-request-in-editor-spec/blob/master/spec.md
* Awaitility: small Java DSL for synchronizing asynchronous operations https://github.com/awaitility/awaitility
* RSocket: https://rsocket.io/
* gRPCurl: https://github.com/fullstorydev/grpcurl
* picocli: https://picocli.info/
* GraalVM: https://www.graalvm.org/
* UPX: Ultimate Packer for eXecutables -  https://upx.github.io/
* type-detecting-hints-for-third-party-libraries: https://github.com/joshlong/type-detecting-hints-for-third-party-libraries
* IANA Message Headers: https://www.iana.org/assignments/message-headers/message-headers.xhtml
* MailCatcher: https://github.com/sj26/mailcatcher/
