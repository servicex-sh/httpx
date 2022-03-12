package org.mvnsearch.http.model;

import java.util.Map;

/**
 * Httpx common request - common data structure for request
 *
 * @author linux_china
 */
public class HttpxRequest {
    private String method;
    private String path;
    private String protocol;
    private Map<String, String> headers;
    private byte[] body;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }
}
