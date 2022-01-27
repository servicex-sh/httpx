httpx: CLI for run http file
==========================

httpx is a CLI to execute requests from [JetBrains Http File](https://www.jetbrains.com/help/idea/http-client-in-product-code-editor.html).

# How to use?

Create index.http file with following code, then `chmod u+x index.http`

```http request
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
```

Then input `httpx myip` or `./index.http myip` to invoke request.

# Protocols Support

* HTTP Request
* RSocket Request
* GRPC Request: you should install [grpcurl](https://github.com/fullstorydev/grpcurl)

# References

* JetBrains HTTP client: https://www.jetbrains.com/help/idea/http-client-in-product-code-editor.html
* http-request-in-editor-spec: https://github.com/JetBrains/http-request-in-editor-spec/blob/master/spec.md
* RSocket: https://rsocket.io/
* gRPCurl: https://github.com/fullstorydev/grpcurl
* picocli: https://picocli.info/
