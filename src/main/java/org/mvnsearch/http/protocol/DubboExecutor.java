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
import java.util.*;


public class DubboExecutor extends HttpBaseExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(DubboExecutor.class);
    public static final Map<String, String> SHORT_TYPE_MAPPING = new HashMap<>();

    static {
        SHORT_TYPE_MAPPING.put("boolean", "java.lang.Boolean");
        SHORT_TYPE_MAPPING.put("Boolean", "java.lang.Boolean");
        SHORT_TYPE_MAPPING.put("Boolean[]", "java.lang.Boolean[]");
        SHORT_TYPE_MAPPING.put("byte", "java.lang.Byte");
        SHORT_TYPE_MAPPING.put("Byte", "java.lang.Byte");
        SHORT_TYPE_MAPPING.put("Byte[]", "java.lang.Byte[]");
        SHORT_TYPE_MAPPING.put("char", "java.lang.Char");
        SHORT_TYPE_MAPPING.put("Char", "java.lang.Char");
        SHORT_TYPE_MAPPING.put("Char[]", "java.lang.Char[]");
        SHORT_TYPE_MAPPING.put("short", "java.lang.Short");
        SHORT_TYPE_MAPPING.put("Short", "java.lang.Short");
        SHORT_TYPE_MAPPING.put("Short[]", "java.lang.Short[]");
        SHORT_TYPE_MAPPING.put("int", "java.lang.Integer");
        SHORT_TYPE_MAPPING.put("Integer", "java.lang.Integer");
        SHORT_TYPE_MAPPING.put("Integer[]", "java.lang.Integer[]");
        SHORT_TYPE_MAPPING.put("long", "java.lang.Long");
        SHORT_TYPE_MAPPING.put("Long", "java.lang.Long");
        SHORT_TYPE_MAPPING.put("Long[]", "java.lang.Long[]");
        SHORT_TYPE_MAPPING.put("float", "java.lang.Float");
        SHORT_TYPE_MAPPING.put("Float", "java.lang.Float");
        SHORT_TYPE_MAPPING.put("Float[]", "java.lang.Float[]");
        SHORT_TYPE_MAPPING.put("double", "java.lang.Double");
        SHORT_TYPE_MAPPING.put("Double", "java.lang.Double");
        SHORT_TYPE_MAPPING.put("Double[]", "java.lang.Double[]");
        SHORT_TYPE_MAPPING.put("String", "java.lang.String");
        SHORT_TYPE_MAPPING.put("String[]", "java.lang.String[]");
    }

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
                paramsTypeArray = Arrays.stream(parts.split(","))
                        .map(typeName -> SHORT_TYPE_MAPPING.getOrDefault(typeName, typeName))
                        .toArray(String[]::new);
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
                        runJsTest(httpRequest, 200, Collections.emptyMap(), "application/json", text);
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
        ByteBuffer buf = ByteBuffer.allocate(4096);
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
        } while (readCount == 4096);
        return bos.toByteArray();
    }

}
