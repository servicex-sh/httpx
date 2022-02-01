package org.mvnsearch.http.protocol;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.HessianSerializerInput;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mvnsearch.http.model.HttpRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DubboExecutor extends HttpBaseExecutor {
    public List<byte[]> execute(HttpRequest httpRequest) {
        final URI dubboUri = httpRequest.getRequestTarget().getUri();
        String serviceName = dubboUri.getPath().substring(1);
        final Map<String, String> params = getQueryParamsMap(dubboUri);
        String methodSignature = params.get("method");
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
                    final ObjectMapper objectMapper = new ObjectMapper();
                    try {
                        final List<?> items = objectMapper.readValue(text, List.class);
                        arguments = new Object[items.size()];
                        for (int i = 0; i < items.size(); i++) {
                            final Object item = items.get(i);
                            if (item instanceof String) {
                                arguments[i] = item;
                            } else {
                                arguments[i] = objectMapper.writeValueAsString(item);
                            }
                        }
                    } catch (Exception e) {

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
        try (Socket clientSocket = new Socket(dubboUri.getHost(), port)) {
            DubboRpcInvocation invocation = new DubboRpcInvocation(serviceName, methodName, paramsTypeArray, arguments);
            final byte[] contentBytes = invocation.toBytes();
            final byte[] headerBytes = invocation.frameHeaderBytes(0L, contentBytes.length);
            final OutputStream output = clientSocket.getOutputStream();
            output.write(headerBytes);
            output.write(contentBytes);
            final InputStream inputStream = clientSocket.getInputStream();
            final byte[] data = extractData(inputStream);
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
                        ObjectMapper objectMapper = new ObjectMapper();
                        String text = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
                        System.out.print(text);
                        return List.of(text.getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }


    public byte[] extractData(InputStream inputStream) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int readCount;
        int counter = 0;
        do {
            readCount = inputStream.read(buf);
            int startOffset = 0;
            int length = readCount;
            if (counter == 0) {
                startOffset = 16;
                length = readCount - 16;
            }
            bos.write(buf, startOffset, length);
            counter++;
        } while (readCount == 1024);
        return bos.toByteArray();
    }

    public static Map<String, String> getQueryParamsMap(URI url) {
        String queryPart = url.getQuery();
        if (queryPart == null || queryPart.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> queryParams = new HashMap<>();
        try {
            String[] pairs = queryPart.split("&");
            for (String pair : pairs) {
                String[] keyValuePair = pair.split("=");
                queryParams.put(URLDecoder.decode(keyValuePair[0], StandardCharsets.UTF_8.name()),
                        URLDecoder.decode(keyValuePair[1], StandardCharsets.UTF_8.name()));
            }
        } catch (Exception ignore) {

        }
        return queryParams;
    }
}
