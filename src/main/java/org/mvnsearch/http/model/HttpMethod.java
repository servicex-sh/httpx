package org.mvnsearch.http.model;

import java.util.List;

@SuppressWarnings("unused")
public class HttpMethod {
    public static final List<String> HTTP_METHODS = List.of("GET", "HEAD", "POST", "PUT", "DELETE", "CONNECT", "OPTION", "TRACE", "PATCH");
    public static final List<String> RSOCKET_METHODS = List.of("RSOCKET", "RPC", "FNF", "STREAM", "METADATA_PUSH");
    public static final List<String> GRPC_METHODS = List.of("GRPC");
    public static final List<String> GRAPHQL_METHODS = List.of("GRAPHQL");
    public static final List<String> DUBBO_METHODS = List.of("DUBBO");
    public static final List<String> MAIL_METHODS = List.of("MAIL");
    public static final List<String> PUB_METHODS = List.of("PUB");
    public static final List<String> SUB_METHODS = List.of("SUB");
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
                || RSOCKET_METHODS.contains(method)
                || GRPC_METHODS.contains(method)
                || GRAPHQL_METHODS.contains(method)
                || DUBBO_METHODS.contains(method)
                || MAIL_METHODS.contains(method)
                || PUB_METHODS.contains(method)
                || SUB_METHODS.contains(method);
    }

    public static HttpMethod valueOf(String methodName) {
        return new HttpMethod(methodName);
    }

    public boolean isHttpMethod() {
        return HTTP_METHODS.contains(this.name);
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

    public boolean isMailMethod() {
        return MAIL_METHODS.contains(name);
    }

    public boolean isPubMethod() {
        return PUB_METHODS.contains(name);
    }

    public boolean isSubMethod() {
        return SUB_METHODS.contains(name);
    }
}
