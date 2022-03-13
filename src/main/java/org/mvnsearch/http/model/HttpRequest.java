package org.mvnsearch.http.model;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"unused", "DuplicatedCode"})
public class HttpRequest {
    private Integer index;
    private String name;
    private String comment;
    private List<String> tags;
    private HttpMethod method;
    private String requestLine;
    private List<HttpHeader> headers;
    private boolean bodyStarted = false;
    private List<String> bodyLines;
    private byte[] body;
    private String jsTestCode;
    private String redirectResponse;
    private HttpRequestTarget requestTarget;

    public HttpRequest() {
    }

    public HttpRequest(Integer index) {
        this.index = index;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getName() {
        return name == null ? index.toString() : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void addTag(String tag) {
        if (this.tags == null) {
            this.tags = new ArrayList<>();
        }
        this.tags.add(tag);
    }

    public boolean containsTag(String name) {
        if (this.tags != null && !this.tags.isEmpty()) {
            for (String tag : tags) {
                if (tag.equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public String getRequestLine() {
        return requestLine;
    }

    public void setRequestLine(String requestLine) {
        this.requestLine = requestLine;
    }

    public void appendRequestLine(String requestPart) {
        if (requestLine == null) {
            requestLine = requestPart.trim();
        } else {
            requestLine = requestLine + requestPart.trim();
        }
    }

    public HttpRequestTarget getRequestTarget() {
        if (requestTarget == null && method != null && requestLine != null) {
            requestTarget = HttpRequestTarget.valueOf(method.getName(), requestLine);
        }
        return requestTarget;
    }

    public void setRequestTarget(HttpRequestTarget requestTarget) {
        this.requestTarget = requestTarget;
    }

    @NotNull
    public List<HttpHeader> getHeaders() {
        return headers == null ? Collections.emptyList() : headers;
    }

    public void setHeaders(List<HttpHeader> headers) {
        this.headers = headers;
    }

    public Map<String, String> getHeadersMap() {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        } else {
            return headers.stream().collect(Collectors.toMap(HttpHeader::getName, HttpHeader::getValue));
        }
    }

    @Nullable
    public String getHeader(String name) {
        if (this.headers != null) {
            for (HttpHeader header : headers) {
                if (header.getName().equalsIgnoreCase(name)) {
                    return header.getValue();
                }
            }
        }
        return null;
    }

    @NotNull
    public String getHeader(String name, @NotNull String defaultValue) {
        if (this.headers != null) {
            for (HttpHeader header : headers) {
                if (header.getName().equalsIgnoreCase(name)) {
                    return header.getValue();
                }
            }
        }
        return defaultValue;
    }

    public void addHttpHeader(String name, String value) {
        if (headers == null) {
            headers = new ArrayList<>();
        }
        if (name.equalsIgnoreCase("authorization")) {
            // Convert `username password` or `username:password` to Base64
            if (value.startsWith("Basic ")) {
                String token = value.substring(6).trim();
                if (token.contains(" ") || token.contains(":")) {
                    String text = token.replace(" ", ":");
                    value = "Basic " + Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
                }
            }
        } else if (name.equalsIgnoreCase("host") || name.equalsIgnoreCase("uri")) {
            getRequestTarget().setHostOrUriHeader(name, value);
        }
        this.headers.add(new HttpHeader(name, value));
    }

    public List<String> getBodyLines() {
        return bodyLines;
    }

    public void setBodyLines(List<String> bodyLines) {
        this.bodyLines = bodyLines;
    }

    public void addBodyLine(String line) {
        if (this.bodyLines == null) {
            bodyLines = new ArrayList<>();
        }
        this.bodyLines.add(line);
    }

    public String getRedirectResponse() {
        return this.redirectResponse;
    }

    @Nullable
    public String getJavaScriptTestCode() {
        return this.jsTestCode;
    }

    public byte[] getBodyBytes() {
        return this.body != null ? this.body : new byte[]{};
    }

    public String bodyText() {
        return this.body != null ? new String(this.body, StandardCharsets.UTF_8) : "";
    }

    public void setBodyBytes(byte[] body) {
        this.body = body;
    }

    @Nullable
    public String[] getBasicAuthorization() {
        final String header = this.getHeader("Authorization");
        if (header != null && header.startsWith("Basic ")) {
            final String base64Text = header.substring(6).trim();
            return new String(Base64.getDecoder().decode(base64Text), StandardCharsets.UTF_8).split("[:\s]");
        }
        return null;
    }

    public Mono<ByteBuf> requestBody() {
        return Mono.create(sink -> {
            final byte[] bodyBytes = getBodyBytes();
            if (bodyBytes == null || bodyBytes.length == 0) {
                sink.success();
            } else {
                sink.success(Unpooled.wrappedBuffer(bodyBytes));
            }
        });
    }

    public boolean isFilled() {
        return method != null && requestLine != null;
    }

    public boolean isBodyEmpty() {
        return bodyLines == null || bodyLines.isEmpty();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isBodyStarted() {
        return bodyStarted;
    }

    public void setBodyStarted(boolean bodyStarted) {
        if (method != null) {
            this.bodyStarted = bodyStarted;
        }
    }

    public void cleanBody() throws Exception {
        cleanBody(null);
    }

    /**
     * clean body: extract javascript test code, redirect response etc
     */
    public void cleanBody(@Nullable Path httpFilePath) throws Exception {
        if (bodyLines != null && !bodyLines.isEmpty()) {
            int offset = 0;
            boolean bodyFromExternal = false;
            if (bodyLines.get(0).startsWith("< ")) { // load body from an external file
                String firstLine = bodyLines.get(0);
                String fileName = firstLine.substring(2).trim();
                if (httpFilePath == null || fileName.startsWith("/") || fileName.contains(":\\")) { //absolute path
                    this.body = Files.readAllBytes(Path.of(fileName));
                } else {
                    this.body = Files.readAllBytes(httpFilePath.resolve(fileName));
                }
                bodyFromExternal = true;
                offset = 1;
            }
            List<String> lines = new ArrayList<>();
            for (String bodyLine : bodyLines.subList(offset, bodyLines.size())) {
                if (!bodyLine.startsWith("<>")) {
                    lines.add(bodyLine);
                }
            }
            // extract js code block
            int jsScriptStartOffset = lines.size();
            int jsScriptEndOffset = -1;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.startsWith("> {%")) {
                    jsScriptStartOffset = i;
                }
                if (line.equals("%}") && i > jsScriptStartOffset) {
                    jsScriptEndOffset = i;
                    break;
                }
            }
            if (jsScriptEndOffset > 0) { // javascript test code found
                this.jsTestCode = String.join(System.lineSeparator(), lines.subList(jsScriptStartOffset + 1, jsScriptEndOffset));
                List<String> cleanLines = new ArrayList<>();
                cleanLines.addAll(lines.subList(0, jsScriptStartOffset));
                cleanLines.addAll(lines.subList(jsScriptEndOffset + 1, lines.size()));
                lines = cleanLines;
            }
            // extract js file '> /path/to/responseHandler.js'
            List<String> jsHandlerFiles = new ArrayList<>();
            for (String line : lines) {
                if (line.startsWith("> ") && line.endsWith(".js")) { // response redirect
                    jsHandlerFiles.add(line);
                }
            }
            if (!jsHandlerFiles.isEmpty()) {
                lines.removeAll(jsHandlerFiles);
                //read js block from files
            }
            //extract redirect response file
            for (String line : lines) {
                if (line.startsWith(">> ") || line.startsWith(">>! ")) { // response redirect
                    this.redirectResponse = line;
                }
            }
            if (this.redirectResponse != null) {
                lines.remove(this.redirectResponse);
            }
            if (!bodyFromExternal) { // process body from lines
                if (!lines.isEmpty()) {
                    //remove empty lines after body
                    while (lines.get(lines.size() - 1).isEmpty()) {
                        lines.remove(lines.size() - 1);
                        if (lines.isEmpty()) {
                            break;
                        }
                    }
                    if (!lines.isEmpty()) {
                        String content = String.join(System.lineSeparator(), lines);
                        this.body = content.getBytes(StandardCharsets.UTF_8);
                    }
                }
            }
        }
    }

    public boolean match(String targetName) {
        return targetName.equalsIgnoreCase(this.name) || Objects.equals(targetName, this.index.toString());
    }

    public HttpxRequest convertToHttpxRequest() {
        HttpxRequest request = new HttpxRequest();
        request.setMethod(this.method.getName());
        if (requestLine.contains(" ")) {
            final String[] parts = requestLine.split("\s+", 2);
            request.setPath(parts[0]);
            request.setProtocol(parts[1]);
        } else {
            request.setPath(requestLine);
        }
        request.setUri(this.getRequestTarget().getUri().toString());
        request.setHeaders(getHeadersMap());
        request.setBody(getBodyBytes());
        return request;
    }
}
