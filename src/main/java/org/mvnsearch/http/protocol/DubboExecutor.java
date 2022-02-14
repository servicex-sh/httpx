package org.mvnsearch.http.protocol;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.HessianSerializerInput;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.utils.JsonUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class DubboExecutor extends HttpBaseExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(DubboExecutor.class);

    public List<byte[]> execute(HttpRequest httpRequest) {
        final URI dubboUri = httpRequest.getRequestTarget().getUri();
        final Map<String, String> params = queryToMap(dubboUri);
        String methodSignature = params.get("method");
        String serviceName = dubboUri.getPath().substring(1);
        if (serviceName.contains("/")) {
            final String[] parts = serviceName.split("/", 2);
            serviceName = parts[0];
            methodSignature = parts[1];
        }
        String methodName = methodSignature;
        String[] paramsTypeArray = new String[]{};
        Object[] arguments = new Object[]{};
        if (methodSignature.contains("(")) {
            methodName = methodSignature.substring(0, methodSignature.indexOf('('));
            final String parts = methodSignature.substring(methodSignature.indexOf('(') + 1, methodSignature.indexOf(')'));
            if (!parts.isEmpty()) {
                paramsTypeArray = parts.split(",");
            }
        }
        if (paramsTypeArray.length > 0) {
            final byte[] body = httpRequest.getBodyBytes();
            if (body == null || body.length == 0) {
                arguments = new Object[]{null};
            } else {
                String text = new String(body, StandardCharsets.UTF_8);
                if (!text.startsWith("[")) { // one object
                    if (text.startsWith("\"") && text.endsWith("\"")) { //remove double quota
                        text = text.substring(1, text.length() - 1);
                    }
                    arguments = new Object[]{text};
                } else { // array
                    try {
                        final List<?> items = JsonUtils.readValue(text, List.class);
                        arguments = new Object[items.size()];
                        for (int i = 0; i < items.size(); i++) {
                            final Object item = items.get(i);
                            if (item instanceof String) {
                                arguments[i] = item;
                            } else {
                                arguments[i] = JsonUtils.writeValueAsString(item);
                            }
                        }
                    } catch (Exception e) {
                        log.error("HTX-103-500", text);
                    }
                }
            }
        }
        int port = dubboUri.getPort();
        if (port <= 0) {
            port = 20880;
        }
        System.out.println("DUBBO " + dubboUri);
        System.out.println();
        try (SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(dubboUri.getHost(), port))) {
            DubboRpcInvocation invocation = new DubboRpcInvocation(serviceName, methodName, paramsTypeArray, arguments);
            final byte[] contentBytes = invocation.toBytes();
            final byte[] headerBytes = invocation.frameHeaderBytes(0L, contentBytes.length);
            socketChannel.write(ByteBuffer.wrap(headerBytes));
            socketChannel.write(ByteBuffer.wrap(contentBytes));
            final byte[] data = extractData(socketChannel);
            Hessian2Input input = new HessianSerializerInput(new ByteArrayInputStream(data));
            final Integer responseMark = (Integer) input.readObject();
            if (responseMark == 5 || responseMark == 2) { // null return
                System.out.print("===No return value===");
            } else {
                final Object result = input.readObject();
                if (responseMark == 3 || responseMark == 0) { //exception return
                    System.err.print("=====Exception thrown====");
                    System.err.print(result.toString());
                } else {
                    if (result instanceof String || result instanceof Number) {
                        System.out.print(result);
                        return List.of(result.toString().getBytes(StandardCharsets.UTF_8));
                    } else {
                        String text = JsonUtils.writeValueAsPrettyString(result);
                        System.out.print(prettyJsonFormat(text));
                        return List.of(text.getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
        } catch (Exception e) {
            log.error("HTX-103-408", dubboUri);
        }
        return Collections.emptyList();
    }


    public byte[] extractData(SocketChannel socketChannel) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        //byte[] buf = new byte[1024];
        ByteBuffer buf = ByteBuffer.allocate(1024);
        int readCount;
        int counter = 0;
        do {
            readCount = socketChannel.read(buf);
            int startOffset = 0;
            int length = readCount;
            if (counter == 0) {
                startOffset = 16;
                length = readCount - 16;
            }
            bos.write(buf.array(), startOffset, length);
            counter++;
        } while (readCount == 1024);
        return bos.toByteArray();
    }

}
