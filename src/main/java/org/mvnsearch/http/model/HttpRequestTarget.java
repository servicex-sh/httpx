package org.mvnsearch.http.model;

import java.net.URI;
import java.util.Objects;

@SuppressWarnings("unused")
public class HttpRequestTarget {
    private String method;
    private String requestLine;
    private String fragment;
    private String host;
    private String pathAbsolute;
    private int port;
    private String query;
    private String schema;
    private URI uri;

    public String getRequestLine() {
        return requestLine;
    }

    public String getFragment() {
        return fragment;
    }

    public void setFragment(String fragment) {
        this.fragment = fragment;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPathAbsolute() {
        return pathAbsolute;
    }

    public void setPathAbsolute(String pathAbsolute) {
        this.pathAbsolute = pathAbsolute;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public URI getUri() {
        if (uri != null) {
            return uri;
        }
        StringBuilder builder = new StringBuilder();
        if (schema == null) {
            if (HttpMethod.RSOCKET_METHODS.contains(method)) {
                schema = "tcp";
            } else {
                schema = "http";
            }
        }
        builder.append(schema).append("://");
        builder.append(host);
        if (port > 0) {
            builder.append(":").append(port);
        }
        if (pathAbsolute != null) {
            if (pathAbsolute.startsWith("/")) {
                builder.append(pathAbsolute);
            } else {
                builder.append("/").append(pathAbsolute);
            }
        }
        return URI.create(builder.toString());
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public void setHostOrUriHeader(String headerName, String headerValue) {
        if (Objects.equals(method, "MAIL")) { //ignore Host by mail
            return;
        }
        // aws resource ARN
        if (headerValue.startsWith("arn:aws:")) {
            String temp = headerValue.replace("arn:aws:", "");
            if (temp.contains(":")) {
                temp = temp.substring(0, temp.indexOf(':'));
                if (temp.equals("events")) {
                    this.schema = "eventbridge";
                } else {
                    this.schema = temp;
                }
            }
            return;
        }
        if (this.uri == null) {
            if (headerValue.contains("://")) { // URI
                final URI uri = URI.create(headerValue);
                if (pathAbsolute == null) {
                    this.pathAbsolute = host;
                }
                this.host = uri.getHost();
                this.schema = uri.getScheme();
                this.port = uri.getPort();
                final String rawPath = uri.getRawPath();
                if (rawPath != null && !rawPath.equals("/")) {
                    if (pathAbsolute == null) {
                        this.pathAbsolute = rawPath;
                    } else {
                        if (!(rawPath.endsWith("/") || this.pathAbsolute.startsWith("/"))) {
                            this.pathAbsolute = rawPath + "/" + this.pathAbsolute;
                        } else {
                            this.pathAbsolute = rawPath + this.pathAbsolute;
                        }
                    }
                }
            } else if (headerValue.contains(":")) { // host and port
                final String[] parts = headerValue.split(":", 2);
                if (pathAbsolute == null) {
                    this.pathAbsolute = host;
                }
                this.host = parts[0];
                this.port = Integer.parseInt(parts[1]);
            } else {  // host only
                if (pathAbsolute == null) {
                    this.pathAbsolute = host;
                }
                this.host = headerValue;
            }
        }
    }

    public static HttpRequestTarget valueOf(String method, String requestLine) {
        final HttpRequestTarget requestTarget = new HttpRequestTarget();
        requestTarget.method = method;
        requestTarget.requestLine = requestLine;
        String requestUri = requestLine;
        if (Objects.equals(method, "MAIL")) {  //MAIL
            if (!requestUri.startsWith("mailto:")) {
                requestUri = "mailto:" + requestUri;
            }
            requestTarget.uri = URI.create(requestUri);
            return requestTarget;
        }
        if (requestLine.contains(" HTTP/")) {  // request line with protocol `GET /index.html HTTP/1.1`
            requestUri = requestLine.substring(0, requestLine.lastIndexOf(" "));
            final String protocol = requestLine.substring(requestLine.lastIndexOf(" ") + 1);
            if (protocol.contains("HTTPS")) {
                requestTarget.schema = "https://";
            }
        }
        if (method.equals("DUBBO")) {
            if (!requestUri.startsWith("dubbo://") && requestUri.contains(":")) {
                requestUri = "dubbo://" + requestUri;
            }
        } else if (method.equals("SOFA")) {
            if (!requestUri.startsWith("bolt://") && requestUri.contains(":")) {
                requestUri = "bolt://" + requestUri;
            }
        } else if (method.equals("GRAPHQLWS")) {
            if (!requestUri.startsWith("ws://")) {
                requestUri = "ws://" + requestUri;
            }
        } else if (method.equals("GRAPHQLWSS")) {
            if (!requestUri.startsWith("wss://")) {
                requestUri = "wss://" + requestUri;
            }
        } else if (method.equals("THRIFT")) {
            if (!requestUri.startsWith("thrift://")) {
                requestUri = "thrift://" + requestUri;
            }
        } else if (method.startsWith("ZERO")) {
            if (!requestUri.startsWith("tcp://") && requestUri.contains(":")) {
                requestUri = "tcp://" + requestUri;
            }
        }
        if (!requestUri.contains("://")) { //correct uri without schema
            if (requestUri.contains(":") || requestUri.indexOf('/') > 0) { // uri without schema
                if (HttpMethod.HTTP_METHODS.contains(method)) {
                    requestUri = "http://" + requestUri;
                } else if (HttpMethod.RSOCKET_METHODS.contains(method)) {
                    requestUri = "tcp://" + requestUri;
                } else if (HttpMethod.GRPC_METHODS.contains(method)) {
                    requestUri = "http://" + requestUri;
                }
            }
        }
        if (requestUri.contains("://")) {  //standard URL
            final URI uri = URI.create(requestUri);
            requestTarget.uri = uri;
            requestTarget.schema = uri.getScheme();
            requestTarget.host = uri.getHost();
            requestTarget.port = uri.getPort();
            requestTarget.pathAbsolute = uri.getRawPath();
            requestTarget.query = uri.getRawQuery();
            requestTarget.fragment = uri.getFragment();
        } else if (requestUri.startsWith("/")) { //path
            requestTarget.pathAbsolute = requestUri;
        } else {
            requestTarget.host = requestUri;
        }
        return requestTarget;
    }
}
