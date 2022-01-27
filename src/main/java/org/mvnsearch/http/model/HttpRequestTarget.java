package org.mvnsearch.http.model;

import java.net.URI;

@SuppressWarnings("unused")
public class HttpRequestTarget {
    private String method;
    private String fragment;
    private String host;
    private String pathAbsolute;
    private int port;
    private String query;
    private String schema;
    private URI uri;

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
                schema = "tcp://";
            } else {
                schema = "http://";
            }
        }
        builder.append(schema);
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

    public void setHostHeader(String hostHeader) {
        if (this.uri == null) {
            if (hostHeader.contains("://")) { // URI
                final URI uri = URI.create(hostHeader);
                if (pathAbsolute == null) {
                    this.pathAbsolute = host;
                }
                this.host = uri.getHost();
                this.schema = uri.getScheme();
                this.port = uri.getPort();
            } else if (hostHeader.contains(":")) { // host and port
                final String[] parts = hostHeader.split(":", 2);
                if (pathAbsolute == null) {
                    this.pathAbsolute = host;
                }
                this.host = parts[0];
                this.port = Integer.parseInt(parts[1]);
            } else {  // host only
                if (pathAbsolute == null) {
                    this.pathAbsolute = host;
                }
                this.host = hostHeader;
            }
        }
    }

    public static HttpRequestTarget valueOf(String method, String requestLine) {
        final HttpRequestTarget requestTarget = new HttpRequestTarget();
        requestTarget.method = method;
        String requestUri = requestLine;
        if (requestLine.contains(" HTTP/")) {  // request line with protocol `GET /index.html HTTP/1.1`
            requestUri = requestLine.substring(0, requestLine.lastIndexOf(" "));
            final String protocol = requestLine.substring(requestLine.lastIndexOf(" ") + 1);
            if (protocol.contains("HTTPS")) {
                requestTarget.schema = "https://";
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
