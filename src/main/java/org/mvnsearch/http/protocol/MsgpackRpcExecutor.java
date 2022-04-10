package org.mvnsearch.http.protocol;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.jackson.dataformat.MessagePackMapper;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.utils.JsonUtils;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MsgpackRpcExecutor extends HttpBaseExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(MsgpackRpcExecutor.class);

    public List<byte[]> execute(HttpRequest httpRequest) {
        final URI msgpackUri = httpRequest.getRequestTarget().getUri();
        System.out.println("MSGPACK " + msgpackUri);
        System.out.println();
        String functionName = msgpackUri.getPath().substring(1);
        if (functionName.contains("/")) {
            functionName = functionName.substring(functionName.lastIndexOf('/') + 1);
        }
        Object[] args = new Object[]{};
        String body = httpRequest.bodyText();
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
        List<Object> msgpackRequest = new ArrayList<>();
        msgpackRequest.add(0); //RPC request
        msgpackRequest.add(0); //msg id
        msgpackRequest.add(functionName);
        msgpackRequest.add(args);
        ObjectMapper objectMapper = new MessagePackMapper();
        try (SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(msgpackUri.getHost(), msgpackUri.getPort()))) {
            byte[] content = objectMapper.writeValueAsBytes(msgpackRequest);
            socketChannel.write(ByteBuffer.wrap(content));
            final byte[] data = extractData(socketChannel);
            if (data.length == 0) {
                System.out.println("Failed to call remote service, please check function and arguments!");
                return Collections.emptyList();
            }
            List<Object> response = objectMapper.readValue(data, new TypeReference<>() {
            });
            if (response.size() > 3 && response.get(3) != null) {
                final String resultJson = JsonUtils.writeValueAsPrettyString(response.get(3));
                System.out.println(prettyJsonFormat(resultJson));
                runJsTest(httpRequest, 200, Collections.emptyMap(), "application/json", resultJson);
                return List.of(resultJson.getBytes(StandardCharsets.UTF_8));
            } else {
                Object error = response.get(2);
                if (error == null) {
                    error = "Unknown error";
                }
                return List.of(JsonUtils.writeValueAsBytes(error));
            }
        } catch (Exception e) {
            log.error("HTX-111-500", msgpackUri, e);
        }
        return Collections.emptyList();
    }


    public byte[] extractData(SocketChannel socketChannel) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteBuffer buf = ByteBuffer.allocate(1024);
        int readCount;
        do {
            readCount = socketChannel.read(buf);
            if (readCount < 0) {
                return new byte[]{};
            }
            bos.write(buf.array(), 0, readCount);
        } while (readCount == 1024);
        return bos.toByteArray();
    }

}
