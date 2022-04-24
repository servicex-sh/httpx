package org.mvnsearch.http.model;

import java.util.List;

@SuppressWarnings("unused")
public class HttpMethod {
    public static final List<String> HTTP_METHODS = List.of("GET", "HEAD", "POST", "PUT", "DELETE", "CONNECT", "OPTION", "TRACE", "PATCH");
    public static final List<String> REST_METHODS = List.of("REST");
    public static final List<String> RSOCKET_METHODS = List.of("RSOCKET", "RPC", "FNF", "STREAM", "METADATA_PUSH", "GRAPHQLRS");
    public static final List<String> GRPC_METHODS = List.of("GRPC");
    public static final List<String> GRAPHQL_METHODS = List.of("GRAPHQL", "GRAPHQLWS", "GRAPHQLWSS");
    public static final List<String> DUBBO_METHODS = List.of("DUBBO");
    public static final List<String> SOFA_METHODS = List.of("SOFA", "BOLT");
    public static final List<String> TARPC_METHODS = List.of("TARPC");
    public static final List<String> MSGPACK_METHODS = List.of("MSGPACK");
    public static final List<String> NVIM_METHODS = List.of("NVIM");
    public static final List<String> JSONRPC_METHODS = List.of("JSONRPC");
    public static final List<String> THRIFT_METHODS = List.of("THRIFT");
    public static final List<String> ZEROMQ_METHODS = List.of("ZEROREQ");
    public static final List<String> MAIL_METHODS = List.of("MAIL");
    public static final List<String> ALIYUN_METHODS = List.of("ALIYUN", "ALICLOUD");
    public static final List<String> MEMCACHE_METHODS = List.of("MEMCACHE");
    public static final List<String> AWS_METHODS = List.of("AWS", "AWSPOST", "AWSDELETE", "AWSPUT");
    public static final List<String> PUB_METHODS = List.of("PUB");
    public static final List<String> SUB_METHODS = List.of("SUB");

    public static final List<String> REDIS_METHODS = List.of("RSET", "HMSET", "EVAL");
    public static final List<String> SSH_METHODS = List.of("SSH");
    private String name;

    public HttpMethod() {
    }

    public HttpMethod(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static boolean isRequestLine(String line) {
        final int offset = line.indexOf(' ');
        String method;
        if (offset < 0) {
            method = line;
        } else {
            method = line.substring(0, offset);
        }
        return HTTP_METHODS.contains(method)
                || REST_METHODS.contains(method)
                || RSOCKET_METHODS.contains(method)
                || GRPC_METHODS.contains(method)
                || GRAPHQL_METHODS.contains(method)
                || DUBBO_METHODS.contains(method)
                || THRIFT_METHODS.contains(method)
                || ALIYUN_METHODS.contains(method)
                || AWS_METHODS.contains(method)
                || ZEROMQ_METHODS.contains(method)
                || SOFA_METHODS.contains(method)
                || TARPC_METHODS.contains(method)
                || JSONRPC_METHODS.contains(method)
                || MSGPACK_METHODS.contains(method)
                || NVIM_METHODS.contains(method)
                || MAIL_METHODS.contains(method)
                || PUB_METHODS.contains(method)
                || SUB_METHODS.contains(method)
                || REDIS_METHODS.contains(method)
                || SSH_METHODS.contains(method)
                || MEMCACHE_METHODS.contains(method);
    }

    public static HttpMethod valueOf(String methodName) {
        return new HttpMethod(methodName);
    }

    public boolean isHttpMethod() {
        return HTTP_METHODS.contains(this.name);
    }

    public boolean isRestMethod() {
        return REST_METHODS.contains(this.name);
    }

    public boolean isRSocketMethod() {
        return RSOCKET_METHODS.contains(name);
    }

    public boolean isGrpcMethod() {
        return GRPC_METHODS.contains(name);
    }

    public boolean isGraphQLMethod() {
        return GRAPHQL_METHODS.contains(name);
    }

    public boolean isDubboMethod() {
        return DUBBO_METHODS.contains(name);
    }

    public boolean isThriftMethod() {
        return THRIFT_METHODS.contains(name);
    }

    public boolean isSofaMethod() {
        return SOFA_METHODS.contains(name);
    }

    public boolean isTarpcMethod() {
        return TARPC_METHODS.contains(name);
    }

    public boolean isMsgpackMethod() {
        return MSGPACK_METHODS.contains(name);
    }

    public boolean isNvimMethod() {
        return NVIM_METHODS.contains(name);
    }

    public boolean isJsonRPCMethod() {
        return JSONRPC_METHODS.contains(name);
    }

    public boolean isZeromqMethod() {
        return ZEROMQ_METHODS.contains(name);
    }

    public boolean isMailMethod() {
        return MAIL_METHODS.contains(name);
    }

    public boolean isAliyunMethod() {
        return ALIYUN_METHODS.contains(name);
    }

    public boolean isAwsMethod() {
        return AWS_METHODS.contains(name);
    }

    public boolean isPubMethod() {
        return PUB_METHODS.contains(name);
    }

    public boolean isSubMethod() {
        return SUB_METHODS.contains(name);
    }

    public boolean isMemcacheMethod() {
        return MEMCACHE_METHODS.contains(name);
    }

    public boolean isRedisMethod() {
        return REDIS_METHODS.contains(name);
    }

    public boolean isSSHMethod() {
        return SSH_METHODS.contains(name);
    }
}
