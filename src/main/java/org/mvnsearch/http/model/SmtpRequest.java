package org.mvnsearch.http.model;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class SmtpRequest {
    private URL url;
    private String host;
    private int port;
    private boolean ssl;
    private String authorization;
    private String from;
    private String replyTo;
    private String to;
    private String cc;
    private String bcc;
    private String subject;
    private String contentType;
    private HttpRequest httpRequest;

    public SmtpRequest(HttpRequest httpRequest) {
        try {
            this.url = new URL(httpRequest.getRequestTarget().getUri().toString());
            this.to = url.getPath();
            this.from = httpRequest.getHeader("From");
            this.authorization = httpRequest.getHeader("Authorization");
            this.replyTo = httpRequest.getHeader("Reply-To");
            this.subject = httpRequest.getHeader("Subject");
            this.contentType = httpRequest.getHeader("Content-Type");
            final Map<String, String> params = splitQuery(url);
            this.cc = params.get("cc");
            this.bcc = params.get("bcc");
            this.httpRequest = httpRequest;
            String hostHeader = httpRequest.getHeader("Host");
            if (hostHeader != null && !hostHeader.isEmpty()) {
                if (hostHeader.startsWith("ssl://")) {
                    final URI smtpUri = URI.create(hostHeader);
                    this.host = smtpUri.getHost();
                    this.ssl = true;
                    this.port = smtpUri.getPort();
                    if (this.port <= 0) {
                        this.port = 465;
                    }
                } else if (hostHeader.contains(":")) {
                    final String[] parts = hostHeader.split(":");
                    this.host = parts[0];
                    this.port = Integer.parseInt(parts[1]);
                } else {
                    this.host = hostHeader;
                    this.port = 25;
                }
            }
        } catch (Exception e) {

        }
    }

    public URL getUrl() {
        return url;
    }

    public String getHost() {
        return host;
    }

    public String getAuthorization() {
        return authorization;
    }

    public int getPort() {
        return port;
    }

    public boolean isSsl() {
        return ssl;
    }

    public String getFrom() {
        return from;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public String getTo() {
        return to;
    }

    public String getCc() {
        return cc;
    }

    public String getBcc() {
        return bcc;
    }

    public String getSubject() {
        return subject;
    }

    public String getContentType() {
        return contentType;
    }

    public String body() {
        return new String(httpRequest.getBodyBytes(), StandardCharsets.UTF_8);
    }

    public boolean isLegal() {
        return host != null && to != null && from != null;
    }

    public static Map<String, String> splitQuery(URL url) throws UnsupportedEncodingException {
        Map<String, String> params = new LinkedHashMap<>();
        String query = url.getQuery();
        if (query != null && !query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                params.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
            }
        }
        return params;
    }
}
