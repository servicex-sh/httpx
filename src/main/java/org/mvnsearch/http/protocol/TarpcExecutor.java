package org.mvnsearch.http.protocol;

import org.apache.commons.lang3.StringUtils;
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
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class TarpcExecutor extends HttpBaseExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(TarpcExecutor.class);


    public List<byte[]> execute(HttpRequest httpRequest) {
        final URI tarpcUri = httpRequest.getRequestTarget().getUri();
        System.out.println("TARPC " + tarpcUri);
        System.out.println();
        Instant now = Instant.now().plusSeconds(10); //plus 10 seconds for deadline
        String functionName = StringUtils.capitalize(tarpcUri.getPath().substring(1));
        String jsonRequest = """
                {
                  "Request": {
                    "context": {
                      "deadline": {
                        "secs_since_epoch": %d,
                        "nanos_since_epoch": %d
                      },
                      "trace_context": {
                        "trace_id": [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                        "span_id": 2661146547092162534,
                        "sampling_decision": "Unsampled"
                      }
                    },
                    "id": 0,
                    "message": {
                      "%s": %s
                    }
                  }
                }
                """.formatted(now.getEpochSecond(), now.getNano(),
                functionName,
                httpRequest.bodyText());
        try (SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(tarpcUri.getHost(), tarpcUri.getPort()))) {
            final String jsonText = JsonUtils.writeValueAsString(JsonUtils.readValue(jsonRequest, Map.class));
            byte[] content = jsonText.getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer = ByteBuffer.allocate(content.length + 4);
            buffer.put((byte) 0x00);
            buffer.put((byte) 0x00);
            buffer.put((byte) 0x01);
            buffer.put((byte) 0x0C);
            // buffer.put((byte) 0x7B);
            // buffer.put((byte) 0x22);
            // buffer.put((byte) 0x52);
            //buffer.put((byte) 0x65);
            buffer.put(content);
            buffer.rewind();
            socketChannel.write(buffer);
            final byte[] data = extractData(socketChannel);
            String text = new String(data, StandardCharsets.UTF_8);
            boolean okResultFound = false;
            if (text.startsWith("{")) {
                final Map<String, Object> result = JsonUtils.readValue(text, Map.class);
                if (result.containsKey("message")) {
                    final Map<String, Object> message = (Map<String, Object>) result.get("message");
                    if (message.containsKey("Ok")) {
                        final Map<String, Object> okResult = (Map<String, Object>) message.get("Ok");
                        if (okResult.containsKey(functionName)) {
                            final String resultJson = JsonUtils.writeValueAsPrettyString(okResult.get(functionName));
                            System.out.println(prettyJsonFormat(resultJson));
                            okResultFound = true;
                            runJsTest(httpRequest, 200, Collections.emptyMap(), "application/json", resultJson);
                        }
                    }
                }
            }
            if (!okResultFound) {
                System.out.print(prettyJsonFormat(text));
            }
            return List.of(text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("HTX-107-408", tarpcUri, e);
        }
        return Collections.emptyList();
    }


    public byte[] extractData(SocketChannel socketChannel) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteBuffer buf = ByteBuffer.allocate(1024);
        int readCount;
        int counter = 0;
        do {
            readCount = socketChannel.read(buf);
            int startOffset = 0;
            int length = readCount;
            if (counter == 0) {
                startOffset = 4;
                length = readCount - 4;
            }
            bos.write(buf.array(), startOffset, length);
            counter++;
        } while (readCount == 1024);
        return bos.toByteArray();
    }

}
