package org.mvnsearch.http.protocol;

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
import java.util.Collections;
import java.util.List;


public class ThriftExecutor extends HttpBaseExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(ThriftExecutor.class);

    public List<byte[]> execute(HttpRequest httpRequest) {
        final URI thriftUri = httpRequest.getRequestTarget().getUri();
        System.out.println("THRIFT " + thriftUri);
        System.out.println();
        String serviceName = thriftUri.getPath().substring(1);
        if (serviceName.contains("/")) { //convert '/' to ':'
            serviceName = serviceName.replace('/', ':');
        }
        StringBuilder builder = new StringBuilder();
        builder.append('[').append('1').append(','); // tjson start and version
        builder.append('"').append(serviceName).append('"').append(','); //append service name
        builder.append('1').append(','); // call type - TMessageType.CALL
        builder.append('1').append(','); // message id
        final String jsonText = new String(httpRequest.getBodyBytes(), StandardCharsets.UTF_8);
        try {
            builder.append(JsonUtils.convertToTJSON(jsonText)); // json args
        } catch (Exception ignore) {
            log.error("HTX-002-501", jsonText);
            return Collections.emptyList();
        }
        builder.append(']'); //tjson lose
        byte[] content = builder.toString().getBytes(StandardCharsets.UTF_8);
        try (SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(thriftUri.getHost(), thriftUri.getPort()))) {
            ByteBuffer buffer = ByteBuffer.allocate(content.length + 4);
            buffer.putInt(content.length);
            buffer.put(content);
            buffer.rewind();
            socketChannel.write(buffer);
            final byte[] data = extractData(socketChannel);
            String text = new String(data, StandardCharsets.UTF_8);
            System.out.print(prettyJsonFormat(text));
            return List.of(text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("HTX-103-408", thriftUri, e);
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
