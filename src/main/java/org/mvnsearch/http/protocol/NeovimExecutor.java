package org.mvnsearch.http.protocol;

import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.utils.JsonUtils;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class NeovimExecutor extends MsgpackRpcExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(NeovimExecutor.class);

    public List<byte[]> execute(HttpRequest httpRequest) {
        final Map<String, String> headers = httpRequest.getHeadersMap();
        URI nvimURI = httpRequest.getRequestTarget().getUri();

        if (nvimURI.getPath().isEmpty() || Objects.equals(nvimURI.getHost(), "null")) {
            String host = headers.getOrDefault("Host", "127.0.0.1:6666");
            nvimURI = URI.create("tcp://" + host + "/" + httpRequest.getRequestLine());
        }
        System.out.println("NVIM " + nvimURI);
        System.out.println();
        String functionName = nvimURI.getPath().substring(1);
        if (functionName.contains("/")) {
            functionName = functionName.substring(functionName.lastIndexOf('/') + 1);
        }
        if (functionName.equals("nvim_exec_lua")) {
            String contentType = headers.get("Content-Type");
            if (!headers.containsKey("X-Args-1") && Objects.equals("text/x-lua", contentType)) {
                httpRequest.addHttpHeader("X-Args-1", "[]");
            }
        }
        Object[] args = new Object[]{};
        String body = httpRequest.jsonArrayBodyWithArgsHeaders();
        if (!body.isEmpty()) {
            if (!body.startsWith("[")) {
                body = "[" + body + "]";
            }
            try {
                //noinspection unchecked
                args = JsonUtils.readValue(body, List.class).toArray(new Object[0]);
            } catch (Exception e) {
                System.out.println("Failed to parse args: " + body);
                return Collections.emptyList();
            }
        }
        return super.makeRpcCall(nvimURI, functionName, args, httpRequest);
    }


}
