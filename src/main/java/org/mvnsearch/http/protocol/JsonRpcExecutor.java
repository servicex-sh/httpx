package org.mvnsearch.http.protocol;

import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpMethod;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.utils.JsonUtils;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class JsonRpcExecutor extends HttpExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(JsonRpcExecutor.class);

    public List<byte[]> execute(HttpRequest httpRequest) {
        final URI jsonRpcUri = httpRequest.getRequestTarget().getUri();
        System.out.println("JSONRPC " + jsonRpcUri);
        System.out.println();
        String functionName = jsonRpcUri.getPath().substring(1);
        if (functionName.contains("/")) {
            functionName = functionName.substring(functionName.lastIndexOf('/') + 1);
        }
        Map<String, Object> jsonRpcRequest = new HashMap<>();
        jsonRpcRequest.put("jsonrpc", "2.0");
        jsonRpcRequest.put("method", functionName);
        jsonRpcRequest.put("id", 0);
        Object params = null;
        String body = httpRequest.jsonArrayBodyWithArgsHeaders();
        if (!body.isEmpty()) {
            try {
                if (!body.startsWith("{")) {
                    if (!body.startsWith("[")) {
                        body = "[" + body + "]";
                    }
                    params = JsonUtils.readValue(body, List.class);
                } else {
                    params = JsonUtils.readValue(body, Map.class);
                }
            } catch (Exception e) {
                System.out.println("Failed to parse params: " + body);
                return Collections.emptyList();
            }
        }
        if (params != null) {
            jsonRpcRequest.put("params", params);
        }
        if (jsonRpcUri.getScheme().startsWith("http")) {
            httpRequest.setBodyBytes(JsonUtils.writeValueAsBytes(jsonRpcRequest));
            httpRequest.setMethod(HttpMethod.valueOf("POST"));
            return super.execute(httpRequest);
        } else {
            return jsonRpcByTcp(jsonRpcUri, httpRequest, jsonRpcRequest);
        }
    }


    public List<byte[]> jsonRpcByTcp(URI jsonRpcUri, HttpRequest httpRequest, Map<String, Object> jsonRpcRequest) {
        final byte[] content = JsonUtils.writeValueAsBytes(jsonRpcRequest);
        try (SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(jsonRpcUri.getHost(), jsonRpcUri.getPort()))) {
            socketChannel.write(ByteBuffer.wrap(content));
            final byte[] data = extractData(socketChannel);
            String resultJson = new String(data, StandardCharsets.UTF_8);
            System.out.println(prettyJsonFormat(resultJson));
            runJsTest(httpRequest, 200, Collections.emptyMap(), "application/json", resultJson);
            return List.of(resultJson.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("HTX-113-500", jsonRpcUri, e);
        }
        return Collections.emptyList();
    }


    public byte[] extractData(SocketChannel socketChannel) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteBuffer buf = ByteBuffer.allocate(4096);
        int readCount;
        do {
            readCount = socketChannel.read(buf);
            if (readCount > 0) {
                bos.write(buf.array(), 0, readCount);
            }
        } while (readCount == 4096);
        return bos.toByteArray();
    }

}
